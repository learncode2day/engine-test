package com.solartweaks.engine.tweaks

import com.solartweaks.engine.*
import com.solartweaks.engine.bridge.*
import com.solartweaks.engine.util.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.IFEQ
import java.net.URL
import java.util.*
import javax.imageio.ImageIO
import kotlin.concurrent.thread

internal fun initCustomCommands() {
    fun MethodTransformContext.implementCommand(canceller: MethodVisitor.() -> Unit) = methodEnter {
        load(1)
        invokeMethod(::handleCommand)
        val label = Label()
        visitJumpInsn(IFEQ, label)
        canceller()
        visitLabel(label)
    }

    // doesn't work on legacy branch
    fun MethodsContext.injectCommand() = unnamedMethod {
        // For legacy support, the transformations do not have to match:
        allowMissing = true

        val callbackName = "org/spongepowered/asm/mixin/injection/callback/CallbackInfo"
        method hasDesc "(Ljava/lang/String;L$callbackName;)V"
        strings hasPartial "/"

        transform {
            implementCommand {
                load(2)
                invokeMethod(
                    invocationType = InvocationType.VIRTUAL,
                    owner = callbackName,
                    name = "cancel",
                    descriptor = "()V"
                )
            }
        }
    }

    // 1.7-1.18
    findMinecraftClass {
        existingThreshold = .0

        methods {
            named("bridge\$getClientBrand")
            injectCommand()

            // Legacy compatibility
            unnamedMethod {
                allowMissing = true
                method calls { method named "getMessage" }
                transform { implementCommand { returnMethod() } }
            }
        }
    }

    // 1.19.1+?
    findMinecraftClass {
        methods {
            named("bridge\$getRegisterPacketName")
            injectCommand()
        }
    }
}

internal fun userReloadCapeSystem() {
    if (isModuleEnabled<CapeSystem>()) {
        sendChatMessage("Reloading all cosmetics...")

        runCatching {
            ConfigRegistry.clear()
            Minecraft.world?.actualPlayerEntities?.forEach(CapeUtils::reloadCape)
        }.onSuccess { sendChatMessage("Reloaded all cosmetics!", color = "green") }.onFailure {
            sendChatMessage("Failed to reload all cosmetics: $it", color = "red")
            it.printStackTrace()
        }
    } else {
        sendChatMessage("The Cape System module has not been enabled!", color = "red")
    }
}

internal val builtinCommands: Map<String, (List<String>) -> Unit> = mapOf(
    "reloadcapesystem" to { userReloadCapeSystem() },
    "solartweaks" to {
        Minecraft.player!!.addChatMessage(
            ChatSerializer.jsonToComponent(
                // language=json
                """
            [
                "",
                {
                    "text": "Solar Engine Debug",
                    "color": "red"
                },
                {
                    "text": "\n\n"
                },
                {
                    "text": "Minecraft Version: ",
                    "color": "green"
                },
                {
                    "text": "${minecraftVersion.formatted}\n"
                },
                {
                    "text": "Engine Version: ",
                    "color": "green"
                },
                {
                    "text": "$engineVersion\n"
                },
                {
                    "text": "Username: ",
                    "color": "green"
                },
                {
                    "text": "${Minecraft.player.name}\n"
                },
                {
                    "text": "UUID: ",
                    "color": "green"
                },
                {
                    "text": "${Minecraft.player.gameProfile.id}\n"
                },
                {
                    "text": "Session UUID: ",
                    "color": "green"
                },
                {
                    "text": "pid: ${Minecraft.session.playerID}, uid: ${Minecraft.session.profile.id}\n"
                },
                {
                    "text": "Entity UUID: ",
                    "color": "green"
                },
                {
                    "text": "${EntityBridge.castAccessor(Minecraft.player).uniqueID}\n"
                },
                {
                    "text": "Current Server: ",
                    "color": "green"
                },
                {
                    "text": "${Minecraft.currentServerData?.serverIP() ?: "Singleplayer"}\n"
                },
                {
                    "text": "Active modules (${enabledModules().size}): ",
                    "color": "green"
                },
                {
                    "text": "${enabledModules().joinToString { it::class.java.simpleName }}\n"
                },
                {
                    "text": "Found classes: ",
                    "color": "green"
                },
                {
                    "text": "${finders.finders.count { it.hasValue }}/${finders.finders.size}\n"
                },
                {
                    "text": "Accessors generated: ",
                    "color": "green"
                },
                {
                    "text": "${accessors.count { it.internalName.hasValue }}/${accessors.size}\n"
                },
                {
                    "text": "Loaded cosmetics: ",
                    "color": "green"
                },
                {
                    "text": "${
                    if (isModuleEnabled<CapeSystem>() && shouldImplementItemsCached) {
                        ConfigRegistry.getConfig(Minecraft.player.name).playerItemModels.size
                    } else "Not enabled/overridden/1.16+"
                }\n"
                }
            ]
            """
            )
        )
        Minecraft.player.gameProfile
    },
    "reloadscripts" to {
        globalConfiguration.customCommands.reloadScripts()
        sendChatMessage("Done!", color = "green")
    },
    "reloadmice" to {
        RawInputThread.rescanMice()
        sendChatMessage("Done!", color = "green")
    },
    "solarreload" to { displayCustomScreen(ReloadGUI()) },
    "updatecrackedname" to { args ->
        if (isModuleEnabled<AllowCrackedAccounts>() && isCurrentlyCracked()) {
            val newUsername = args.firstOrNull() ?: return@to sendChatMessage("Specify a username!")

            sendChatMessage("Updating your cracked username to `$newUsername`...")
            updateCrackedUsername(newUsername)
            reloadWebsocket()
            sendChatMessage("Done! Relog for the changes to take effect.", color = "green")
        } else {
            sendChatMessage("Cracked not enabled!")
        }
    },
    "reloadsocket" to {
        reloadWebsocket()
        sendChatMessage("Done!", color = "green")
    }
)

internal val registeredCommands = mutableMapOf<String, (List<String>) -> Unit>()
fun registerCommand(name: String, handler: (List<String>) -> Unit) {
    if (name in registeredCommands) error("That command already exists!")
    registeredCommands[name] = handler
}

internal fun reloadWebsocket() {
    thread { reconnectWebsocket().tryInvoke(cachedLunarMain) }
}

internal class ReloadGUI : ScreenFacade() {
    override fun setup() {
        elementExtension.setupButtons {
            val centerX = width / 2f - 75f
            val centerY = height / 2f - 10f
            add(ButtonInfo("Reload mice (raw)", centerX, centerY - 30f).handler { RawInputThread.rescanMice() })
            add(ButtonInfo("Reload scripts", centerX, centerY).handler {
                globalConfiguration.customCommands.reloadScripts()
            })

            add(ButtonInfo("Reload cape system (OF)", centerX, centerY + 30f).handler { userReloadCapeSystem() })
            add(ButtonInfo("Reconnect websocket", centerX, centerY + 60f).handler { reloadWebsocket() })
        }

        elementExtension.setupLabels {
            add(LabelInfo("Solar Engine Reloader™", width / 2f, height / 2f - 60f))
        }
    }
}

fun handleCommand(command: String): Boolean {
    val (commands) = globalConfiguration.customCommands
    val allCommands = commands.mapValues {
        { args: List<String> ->
            LocalPlayer.castAccessor(Minecraft.player!!).sendChatMessage("${it.value} ${args.joinToString(" ")}")
        }
    } + builtinCommands + globalConfiguration.customCommands.loadedScripts + registeredCommands

    val partialArgs = command.split(' ')
    val name = partialArgs.first().removePrefix("/")

    return if (name == "solarhelp") {
        sendChatMessage("Available commands: ${allCommands.keys.joinToString()}", color = "white")
        true
    } else allCommands[name]?.invoke(partialArgs.drop(1)) != null
}

internal fun sendChatMessage(msg: String, color: String = "gray") = Minecraft.player!!.addChatMessage(
    ChatSerializer.jsonToComponent(
        """
        [
            "",
            {
                "text": "${msg.replace("\"", "\\\"")}",
                "color": "$color"
            }
        ]
        """.trimIndent()
    )
)

private val chatSerializer = finders.findClass {
    strings hasPartial "Don't know how to turn"
    methods {
        "jsonToComponent" {
            arguments[0] = asmTypeOf<String>()
            method.isStatic()
        }
    }
}

private val chatSerializerAccess by accessor<_, ChatSerializer.Static>(chatSerializer)

interface ChatSerializer : InstanceAccessor {
    interface Static : StaticAccessor<ChatSerializer> {
        fun jsonToComponent(json: String): ChatComponent
    }

    companion object : Static by chatSerializerAccess.static
}