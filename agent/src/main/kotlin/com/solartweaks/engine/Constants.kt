package com.solartweaks.engine

import com.solartweaks.engine.util.getAppClasses
import com.solartweaks.engine.util.internalName

const val lunarPackage = "com/moonsworth/lunar/"
const val oldLunarPackage = "lunar/"
const val minecraftPackage = "net/minecraft/"
const val optifinePackage = "net/optifine/"

val isOptifineLoaded by lazy {
    globalInstrumentation.getAppClasses().any { it.internalName.startsWith(optifinePackage) }
}

val isGraalLoaded by lazy { runCatching { Class.forName("org.graalvm.polyglot.Context") }.isSuccess }