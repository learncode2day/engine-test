package com.solartweaks.example

import com.solartweaks.engine.ModInitializer
import com.solartweaks.engine.bridge.*
import com.solartweaks.engine.tweaks.registerCommand
import java.lang.instrument.Instrumentation

@Suppress("unused")
object ExampleMod : ModInitializer {
    override fun premain(inst: Instrumentation) {
        println("Hello from example-mod!")
    }

    override fun lunarInitialized() {
        Minecraft.connect(
            serverData = Initializer.initServerData(
                name = "Hypixel",
                ip = "play.hypixel.net",
                isLan = false
            ),
            parent = Minecraft.currentScreen
        )

        registerCommand("testgui") { displayCustomScreen(TestGui()) }
    }
}

class TestGui : ScreenFacade() {
    override fun setup() {
        elementExtension.setupLabels {
            add(LabelInfo("Welcome to TestGui!", width / 2f, height / 2f))
        }
    }
}