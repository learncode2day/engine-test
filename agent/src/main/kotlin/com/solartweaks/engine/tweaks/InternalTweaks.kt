package com.solartweaks.engine.tweaks

import com.solartweaks.engine.*
import com.solartweaks.engine.bridge.*
import com.solartweaks.engine.util.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AnalyzerAdapter
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.util.*
import javax.imageio.ImageIO

internal fun initInternalTweaks() {
    finders.findClass {
        // Backwards compat with vpatcher
        methods {
            "getPreloaded" {
                strings has "(.*)duplicate (.*) definition(.*)"
                transform { fixedValue(0) }
            }
        }
    }

    findMinecraftClass {
        strings hasPartial "Stacktrace:"
        methods {
            "firstTwoElementsOfStackTraceMatch" {
                method calls { method named "getFileName" }
                transform {
                    replaceCall(
                        matcher = { it.name == "getFileName" },
                        replacement = { pop(); loadConstant("") }
                    )
                }
            }
        }
    }

    // Before people cry again: this is not visible to the server
    findMinecraftClass {
        strings has "debug"
        methods {
            "draw" {
                strings has "Towards negative Z"
                transform {
                    replaceCall(
                        matcher = { it.name == "getClientModName" },
                        replacement = { invokeMethod(::hiddenModName) }
                    )
                }
            }
        }
    }

    findLunarClass {
        node implements "com/lunarclient/bukkitapi/nethandler/client/LCNetHandlerClient"
        methods {
            namedTransform("handleNotification") {
                overwrite {
                    load(1)
                    invokeMethod(::handleNotification)
                    returnMethod()
                }
            }

            "handle" {
                strings hasPartial "Exception registered"
                transform {
                    callAdvice(
                        matcher = { it.name == "handle" },
                        afterCall = {
                            load(2)
                            invokeMethod(::handleBukkitPacket)
                        }
                    )
                }
            }
        }
    }

    // TODO: fix 1.16+
    findMinecraftClass {
        strings has "Couldn't render entity"
        methods {
            "renderNameplate" {
                strings has "deadmau5"
                transform {
                    disableFrameComputing()
                    expandFrames()

                    val colorReplacements = mapOf(
                        "getR" to 0.0,
                        "getG" to 2.0,
                        "getB" to 4.0
                    )

                    visitor { parent ->
                        object : AnalyzerAdapter(asmAPI, owner.name, method.access, method.name, method.desc, parent) {
                            override fun visitMethodInsn(
                                opcode: Int,
                                owner: String,
                                name: String,
                                descriptor: String,
                                isInterface: Boolean
                            ) {
                                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
                                if (name in colorReplacements) {
                                    val label = Label()
                                    getProperty(::rgbPlayers)
                                    load(1)

                                    val entityType = Type.getArgumentTypes(method.desc).first()
                                    invokeMethod(
                                        invocationType = InvocationType.VIRTUAL,
                                        owner = entityType.internalName,
                                        name = "bridge\$getUniqueID",
                                        descriptor = "()L${internalNameOf<UUID>()};"
                                    )

                                    invokeMethod(Set<*>::contains)
                                    visitJumpInsn(IFEQ, label)

                                    pop() // remove float
                                    invokeMethod(System::currentTimeMillis) // current time
                                    visitInsn(L2D) // to double
                                    loadConstant(1000.0) // time / 1000 -> seconds
                                    visitInsn(DDIV)

                                    val constant = colorReplacements.getValue(name)
                                    if (constant != 0.0) {
                                        loadConstant(constant) // add a constant term
                                        visitInsn(DADD)
                                    }

                                    invokeMethod(Math::cos)
                                    visitInsn(D2F) // convert to float
                                    visitInsn(DUP)
                                    visitInsn(FMUL)
                                    // result: cos^2(ms/1000 + c)

                                    visitLabel(label)
                                    addCurrentFrame()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Before people cry again: this is not visible to the server
fun hiddenModName() = runCatching { "Solar Tweaks v$engineVersion, Minecraft ${minecraftVersion.id} (hidden from server)" }
    .getOrElse { "Solar Tweaks v$engineVersion, Minecraft version unknown (hidden from server)" }

fun handleBukkitPacket(packet: Any?) {
    if (globalConfiguration.debugPackets && packet != null) {
        println("Incoming packet ${packet::class.java}")
        packet::class.java.declaredFields.joinToString(System.lineSeparator()) { f ->
            "${f.name}: ${f.also { it.isAccessible = true }[packet]}"
        }
    }
}

fun updateWindowTitle() = withModule<WindowTitle> {
    val date = LocalDateTime.now()
    val part = if (date.dayOfMonth == 1 && date.monthValue == 4) "Twix" else "Engine"
    val versionPart = if (showVersion) " (Solar $part v$engineVersion)" else ""
    val advancedPart = if (showAdvancedVersion) runCatching {
        " (${LunarBuildData.gitHash}/${LunarBuildData.gitBranch})"
    }.getOrNull() ?: " (missing data)" else ""

    Minecraft.setDisplayTitle(title + versionPart + advancedPart)
}

fun sendLaunch() {
    runCatching {
        val actualType = when (val type = System.getProperty("solar.launchType")) {
            null -> "patcher"
            "shortcut" -> "launcher"
            "launcher" -> return println("Detected usage of the Solar Tweaks launcher")
            else -> return println("Invalid launch type $type, ignoring...")
        }

        val version = minecraftVersion.formatted
        with(URL("https://server.solartweaks.com/api/launch").openConnection() as HttpURLConnection) {
            requestMethod = "POST"
            doOutput = true
            outputStream.bufferedWriter().write(json.encodeToString(LaunchRequest(actualType, version)))
        }
    }
        .onSuccess { println("Sent launch request") }
        .onFailure { println("Couldn't send launch request: $it") }
}

internal val lunarMain = findLunarClass {
    methods {
        "initLunar" {
            strings has "Starting Lunar client..."
            transform {
                methodExit {
                    invokeMethod(::preloadAccessors)
                    invokeMethod(::updateWindowTitle)
                    invokeMethod(::sendLaunch)
                    invokeMethod(::modsLunarInit)

                    // TODO: release when api gets updated
//                    visitPrintln("Intializing Stats Protocol")
//                    getObject<StatsProtocol>()
//                    invokeMethod(StatsProtocol::initialize)
                }
            }
        }

        withModule<LunarOverlays> {
            "allowOverrideTexture" {
                strings has "assets/lunar/"
                transform { fixedValue(true) }
            }
        }

        "getLunarMain" {
            method returns self
            method.isStatic()
        }

        "reconnectWebsocket" {
            strings has "Establishing connection"
            strings has "Assets"
        }
    }
}

@Serializable
private data class LaunchRequest(val item: String, val version: String)

// I was too lazy to manually import bukkit nethandler thingy
// have some more codegen

private val lcPacketNotification = findNamedClass("com/lunarclient/bukkitapi/nethandler/client/LCPacketNotification") {
    methods {
        named("getMessage")
        named("getLevel")
    }
}

private val notifAccess by accessor<_, LCPacketNotification.Static>(lcPacketNotification)

interface LCPacketNotification : InstanceAccessor {
    val message: String
    val level: String

    interface Static : StaticAccessor<LCPacketNotification>
    companion object : Static by notifAccess.static
}

private val popupHandler = finders.findClass {
    strings has "popups"
    strings has "sound/friend_message.ogg"

    methods {
        "displayPopup" {
            arguments count 2
            arguments[0] = asmTypeOf<String>()
            arguments[1] = asmTypeOf<String>()
        }
    }
}

private val popupHandlerAccess by accessor<_, PopupHandler.Static>(popupHandler)

interface PopupHandler : InstanceAccessor {
    fun displayPopup(title: String, description: String): Any
    interface Static : StaticAccessor<PopupHandler>
    companion object : Static by popupHandlerAccess.static
}

private val getPopupHandlerMethod by lunarMain.methods.late { method returns popupHandler() }
val cachedPopupHandler by lazy { PopupHandler.cast(getPopupHandlerMethod().tryInvoke()) }

private val getLunarMain by lunarMain.methods
internal val cachedLunarMain: Any by lazy { getLunarMain().tryInvoke() }
val reconnectWebsocket by lunarMain.methods

fun handleNotification(notif: Any) = runCatching {
    val actualNotif = LCPacketNotification.cast(notif)
    cachedPopupHandler.displayPopup("Server Notification - ${actualNotif.level}", actualNotif.message)
}

val rgbPlayers = hashSetOf<UUID>()

fun handleWebsocketPacket(packet: ByteArray) = with(Initializer.initPacketBuffer(Unpooled.wrappedBuffer(packet))) {
    when (readVarIntFromBuffer()) {
        4022 -> {
            val uuid = readUUID()
            if (readBoolean()) rgbPlayers += uuid else rgbPlayers -= uuid
        }

        98327 -> {
            if (!cosmeticIndexEntry.hasValue) return@with

            val id = readInt()
            val displayName = readStringFromBuffer(256)
            val animated = readBoolean()
            val categoryName = readStringFromBuffer(64)
            val category = runCatching { CosmeticType.valueOf(categoryName) }.getOrNull() ?: return@with
            val image = ByteArray(readInt())
            readBytes(image)

            val resourceName = generateSequence { ('a'..'z').random() }.take(20)
                .joinToString(separator = "", postfix = ".webp")

            Minecraft.submit {
                val cosmeticLoc = ResourceLocation.createWithDomain("lunar", resourceName)
                val img = ImageIO.read(ByteArrayInputStream(image))
                Minecraft.textureManager.loadTexture(
                    loc = cosmeticLoc,
                    texture = TextureHolder.castAccessor(Initializer.initCopiedDynamicTexture(img))
                )
            }

            cachedCosmeticsManager.addCosmetic(
                CosmeticIndexEntry.construct(
                    id = id,
                    resource = resourceName,
                    name = displayName,
                    animated = animated,
                    geckoLib = false,
                    category = category,
                    indexType = categoryName.lowercase()
                )
            )
        }
    }
}

fun clearRGBPlayers() = rgbPlayers.clear()

fun preloadAccessors() = runCatching {
    println("Preloading ${accessors.size} accessors")
    accessors.forEach { data ->
        runCatching {
            if (data.internalName.hasValue) {
                data.staticAccessor
                data.virtualAccessor
            } else {
                println("Not found: ${data.virtualType.simpleName}")
            }
        }.onFailure {
            println("Failed to preload accessor ${data.virtualAccessorName}")
            it.printStackTrace()
        }
    }

    println("Preloading GLCompat")
    GLCompat
    println("Done!")
}.onFailure {
    println("Error preloading accessors")
    it.printStackTrace()
}