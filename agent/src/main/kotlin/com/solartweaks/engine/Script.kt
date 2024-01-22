package com.solartweaks.engine

import com.solartweaks.engine.bridge.Initializer
import com.solartweaks.engine.bridge.Minecraft
import com.solartweaks.engine.tweaks.ChatSerializer
import kotlinx.coroutines.*
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

private val jsJobScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

// function command(args)
const val commandEndpoint = "command"
const val scriptLanguage = "js"

fun File.asGraalContext(language: String = scriptLanguage): Context = Context.newBuilder(language)
    .allowIO(true)
    .allowHostClassLookup { true }
    .allowHostAccess(HostAccess.ALL)
    .allowExperimentalOptions(true)
    .option("js.esm-eval-returns-exports", "true")
    .build()
    .apply {
        getBindings(language).putMember("api", JSAPI)
        val result = eval(Source.newBuilder(language, this@asGraalContext).build())
        if (extension == "mjs" && result.hasMembers()) {
            result.memberKeys.forEach { getBindings(language).putMember(it, result.getMember(it)) }
        }
    }

fun Context.runCommand(args: List<String>, language: String = scriptLanguage) =
    getBindings(language).getMember(commandEndpoint).executeVoid(args)

@Suppress("unused")
object JSAPI {
    val player get() = Minecraft.player
    val world get() = Minecraft.world
    val minecraft get() = Minecraft
    val initializer get() = Initializer
    fun toComponent(json: String) = ChatSerializer.jsonToComponent(json)

    fun setTimeout(runnable: Runnable, delay: Long) = Timers.createTask(runnable, delay)
    fun setInterval(runnable: Runnable, delay: Long) = Timers.createTask(runnable, delay, repeats = true)

    fun removeTimer(id: Int) = Timers.removeTask(id)
    fun removeInterval(id: Int) = removeTimer(id)
    fun removeTimeout(id: Int) = removeTimer(id)

    fun fetch(options: RequestOptions) = PromiseImpl { res, rej ->
        jsJobScope.launch {
            (withContext(Dispatchers.IO) { URL(options.url).openConnection() } as HttpURLConnection).runCatching {
                requestMethod = options.method
                options.headers.forEach { (k, v) -> setRequestProperty(k, v) }
                options.body?.let { body ->
                    doOutput = true
                    outputStream.write(body.encodeToByteArray())
                }

                inputStream.readBytes().decodeToString()
            }
                .onSuccess { res.executeVoid(it) }
                .onFailure { rej.executeVoid(it) }
        }
    }

    fun fetch(map: Map<String, Any?>): PromiseImpl {
        val ctor = ::RequestOptions
        val options = ctor.callBy(map.mapKeys { (n) ->
            ctor.parameters.find { it.name == n } ?: error("Unrecognized option $n")
        })

        return fetch(options)
    }

    fun wait(time: Long) = PromiseImpl { resolve, _ ->
        jsJobScope.launch {
            delay(time)
            resolve.executeVoid()
        }
    }

    data class RequestOptions(
        val url: String,
        val method: String = "GET",
        val body: String? = null,
        val headers: Map<String, String> = mapOf(),
    )
}

@Suppress("unused")
fun interface PromiseImpl {
    fun then(resolve: Value, reject: Value)
}

object Timers {
    private var idCounter = 0
        get() = field++

    private val tasks: MutableSet<TimerTask> = Collections.synchronizedSet(hashSetOf())

    data class TimerTask(val job: Job, val id: Int = idCounter)

    fun createTask(runnable: Runnable, delay: Long, repeats: Boolean = false): Int {
        val job = jsJobScope.launch {
            do {
                delay(delay)
                runnable.run()
            } while (repeats)
        }

        val task = TimerTask(job)
        tasks += task

        job.invokeOnCompletion { tasks -= task }
        return task.id
    }

    fun removeTask(id: Int) = tasks.find { it.id == id }?.job?.cancel("Timer was removed")
    fun immediate(runnable: Runnable) = createTask(runnable, 0L)
}

fun runLater(block: () -> Unit) {
    Timers.immediate { Minecraft.submit { block() } }
}