package com.solartweaks.engine

import com.solartweaks.engine.bridge.initBridge
import com.solartweaks.engine.bridge.initGLCompat
import com.solartweaks.engine.bridge.initGuiScreen
import com.solartweaks.engine.tweaks.*
import com.solartweaks.engine.util.FinderContext
import com.solartweaks.engine.util.findMainLoader
import com.solartweaks.engine.util.loadConstant
import java.io.File
import java.lang.instrument.Instrumentation

lateinit var globalInstrumentation: Instrumentation
lateinit var globalConfiguration: Configuration

val finders = FinderContext()
val mainLoader by lazy {
    globalInstrumentation.findMainLoader().also {
        if (it == ClassLoader.getSystemClassLoader()) error("Invalid main Lunar Loader $it")
    }
}

fun premain(arg: String?, inst: Instrumentation) {
    // Disable Graals warning that we are in an interpreted (= inefficient) context
    // This will almost always be the case
    System.setProperty("polyglot.engine.WarnInterpreterOnly", "false")

    println("Solar Engine version $engineVersion")

    // Load configuration
    val configFile = File(arg ?: "config.json")
    println("Using config file $configFile")

    globalConfiguration = loadConfig(configFile)
    println("Configuration: $globalConfiguration")

    globalInstrumentation = inst
    finders.registerWith(inst)

    // Important!
    inst.installAntiClassLoader()

    // FIXME: come up with a better way to do this
    initTweaks()
    initInternalTweaks()
    initCapeSystemTweaks()
    initCustomCommands()
    initBridge()
    initRichPresence()
    initGLCompat()
    initGuiScreen()
    initCracked()
    initStatsProtocol()
    initBufferUtil()
    initCustomCosmetics()
    initCustomMods()
}