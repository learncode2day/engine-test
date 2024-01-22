package com.solartweaks.engine.tweaks

import com.solartweaks.engine.*
import com.solartweaks.engine.bridge.Minecraft
import com.solartweaks.engine.bridge.minecraftVersion
import java.time.OffsetDateTime

internal fun initRichPresence() = Unit

private val solarBoot = OffsetDateTime.now()

fun updateRichPresence() = with(RPCBuilder.construct()) {
    withModule<DiscordRichPresence> {
        setDetails("Playing Minecraft ${minecraftVersion.formatted}")
        setStartTimestamp(solarBoot)
        if (showIcon) setLargeImage(icon, iconText)

        val serverText = if (showServerIP) {
            Minecraft.currentServerData?.let { "Multiplayer on ${it.serverIP()}" }
        } else "Multiplayer"

        if (displayActivity) {
            setState(
                when {
                    !Minecraft.isWindowFocused -> afkText
                    Minecraft.currentScreen != null -> menuText
                    Minecraft.hasInGameFocus() -> serverText ?: singlePlayerText
                    else -> "Unknown status"
                }
            )
        }
    }

    build()
}

private val rpcBuilder = findNamedClass("com/jagrosh/discordipc/entities/RichPresence\$Builder") {
    methods {
        named("setState")
        named("setDetails")
        named("setStartTimestamp")
        named("setLargeImage")
        named("build")
        "construct" { method.isConstructor() }
    }
}

private val rpcAccess by accessor<_, RPCBuilder.Static>(rpcBuilder) {
    Class.forName("com.jagrosh.discordipc.entities.RichPresence\$Builder", false, combinedAppLoader)
}

interface RPCBuilder : InstanceAccessor {
    fun setState(state: String): Any
    fun setDetails(details: String): Any
    fun setStartTimestamp(offsetDateTime: OffsetDateTime): Any
    fun setLargeImage(key: String, text: String): Any
    fun build(): Any

    interface Static : StaticAccessor<RPCBuilder> {
        fun construct(): RPCBuilder
    }

    companion object : Static by rpcAccess.static
}