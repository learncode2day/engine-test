package com.solartweaks.engine.tweaks

import com.solartweaks.engine.Unpooled
import com.solartweaks.engine.bridge.*
import com.solartweaks.engine.findLunarClass
import com.solartweaks.engine.tweaks.Heartbeat.send
import com.solartweaks.engine.tweaks.IdentifyPacket.Type.send
import com.solartweaks.engine.util.getObject
import com.solartweaks.engine.util.invokeMethod
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import java.util.*
import javax.swing.JOptionPane
import kotlin.system.exitProcess

internal fun initStatsProtocol() {
    findLunarClass {
        methods {
            "setAccountSession" {
                strings has "Setting account '%s' as the current session."
                transform {
                    methodExit {
                        getObject<StatsProtocol>()
                        invokeMethod(StatsProtocol::asyncIdentify)
                    }
                }
            }
        }
    }
}

private const val statsProtocolURL = "wss://server.solartweaks.com/servers/engine"

private val socketClient = HttpClient(CIO) { install(WebSockets) }

private fun osPlatform() = with(System.getProperty("os.name")) {
    when {
        startsWith("AIX") -> "aix"
        startsWith("Mac OS") -> "darwin"
        startsWith("FreeBSD") -> "freebsd"
        startsWith("Linux") -> "linux"
        startsWith("OpenBSD") -> "openbsd"
        startsWith("SunOS") -> "sunos"
        startsWith("Windows") -> "win32"
        else -> error("Couldn't detect process.platform by os.name")
    }
}

object StatsProtocol {
    private var connection: DefaultWebSocketSession? = null
    private var heartbeatJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private suspend fun connect() = socketClient.webSocketSession(statsProtocolURL)

    fun initialize() {
        if (connection == null) scope.launch {
            connection = connect().apply {
                scope.launch {
                    try {
                        handle()
                    } catch (e: ClosedReceiveChannelException) {
                        handleClose(closeReason.await())
                    }
                }

                scope.launch { sendIdentify() }
            }
        }
    }

    private fun startHeartbeating(interval: Long) {
        heartbeatJob?.cancel("Heartbeat interval changed")
        heartbeatJob = scope.launch {
            while (isActive) {
                Heartbeat.send(connection ?: continue)
                delay(interval)
            }
        }
    }

    fun asyncIdentify() {
        scope.launch { sendIdentify() }
    }

    private suspend fun sendIdentify() = runCatching {
        IdentifyPacket(
            Minecraft.session.profile.id ?: error("Could not identify, player id missing"),
            Minecraft.session.username,
            isCurrentlyCracked(),
            minecraftVersion.exactVersion,
            osPlatform(),
            System.getProperty("os.arch")
        ).send(connection ?: return@runCatching)
    }.onFailure {
        println("Failed to send identify")
        it.printStackTrace()

        clientSideError()
    }

    private suspend fun clientSideError() =
        connection?.close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Client-side error"))

    private fun handleClose(reason: CloseReason?) {
        println("Got disconnected from stats protocol server: $reason")
        heartbeatJob?.cancel("Disconnected")
    }

    private suspend fun DefaultWebSocketSession.handle() {
        while (isActive) {
            val frame = incoming.receive()
            if (frame is Frame.Close) {
                handleClose(frame.readReason())
                break
            }

            val packet = frame as? Frame.Binary ?: continue
            val decoded = packet.readBytes().deserializePacket() ?: continue
            when (decoded) {
                is HeartbeatInfo -> startHeartbeating(decoded.interval.toLong())
                is EndGame -> {
                    // Please don't come after me..
                    JOptionPane.showMessageDialog(
                        null,
                        "Your game got ended with reason: ${decoded.reason}",
                        "Game ended",
                        JOptionPane.ERROR_MESSAGE
                    )

                    exitProcess(-1)
                }

                else -> {
                    /* Unhandled / outgoing packet */
                }
            }
        }
    }
}

// Poorly designed packet system I copied from an old project

private val packetTypes = listOf(IdentifyPacket.Type, HeartbeatInfo.Type, Heartbeat, EndGame.Type)

private interface PacketType<T : Packet> {
    val id: Int

    fun T.serialize(output: PacketBuffer) {
        error("Packet only sent from server, never serialized")
    }

    fun deserialize(input: PacketBuffer): T {
        error("Packet only sent to server, never deserialized")
    }

    fun T.serialize(): ByteArray {
        val buffer = Initializer.initPacketBuffer(Unpooled.buffer())
        buffer.writeVarIntToBuffer(id)
        serialize(buffer)

        return buffer.toByteArray()
    }

    suspend fun T.send(socket: DefaultWebSocketSession): Unit = socket.send(serialize())
}

private sealed interface Packet

private data class IdentifyPacket(
    val uuid: UUID,
    val username: String,
    val cracked: Boolean,
    val version: String,
    val os: String,
    val arch: String
) : Packet {
    companion object Type : PacketType<IdentifyPacket> {
        override val id = 1
        override fun IdentifyPacket.serialize(output: PacketBuffer) = with(output) {
            writeString(uuid.toString())
            writeString(username)
            writeBoolean(cracked)
            writeString(version)
            writeString(os)
            writeString(arch)
        }
    }
}

private data class HeartbeatInfo(val interval: Int) : Packet {
    companion object Type : PacketType<HeartbeatInfo> {
        override val id = 2
        override fun deserialize(input: PacketBuffer) = HeartbeatInfo(input.readInt())
    }
}

private object Heartbeat : Packet, PacketType<Heartbeat> {
    override val id = 3

    // Empty packet
    override fun Heartbeat.serialize(output: PacketBuffer) = Unit
}

private data class EndGame(val reason: String) : Packet {
    companion object Type : PacketType<EndGame> {
        override val id = 4
        override fun deserialize(input: PacketBuffer) = EndGame(input.readStringFromBuffer(512))
    }
}

private fun ByteArray.deserializePacket(): Packet? {
    val buf = Initializer.initPacketBuffer(Unpooled.wrappedBuffer(this))
    val id = buf.readVarIntFromBuffer()
    val type = packetTypes.find { it.id == id } ?: return null

    return type.deserialize(buf).also {
        if (buf.readableBytes() > 0) {
            println("Nag: deserializing packet $it resulted in dangling data! Update packet implementation!")
        }
    }
}