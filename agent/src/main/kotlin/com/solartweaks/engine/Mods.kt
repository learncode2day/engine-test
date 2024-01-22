package com.solartweaks.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import java.io.File
import java.lang.instrument.Instrumentation
import java.util.jar.JarFile

internal fun initCustomMods() = Unit

// Custom mods! WOW!
val mods = globalConfiguration.customMods.asSequence()
    .map { File(it) }.filter { it.exists() }.map { JarFile(it) }
    .onEach { globalInstrumentation.appendToSystemClassLoaderSearch(it) }
    .mapNotNull { file ->
        val modInfo = file.getJarEntry("solar.mod.json")
        if (modInfo == null) {
            println("Failed loading Solar Engine mod ${file.name}: no solar.mod.json")
            return@mapNotNull null
        }

        runCatching {
            json.decodeFromString<ModEntry>(file.getInputStream(modInfo).readAllBytes().decodeToString())
        }.onFailure {
            println("Failed to load Solar Engine mod entry ${file.name}")
            it.printStackTrace()
        }.getOrNull()
    }.toList().ensureUnique().mapNotNull { entry ->
        runCatching {
            val klass = Class.forName(entry.initializer)
            val initializer =
                (if (entry.isKotlin) klass.kotlin.objectInstance else klass.getConstructor().newInstance())
                        as? ModInitializer ?: error("Invalid initializer")

            println("Loading mod ${entry.id} (version ${entry.version})")
            initializer.premain(globalInstrumentation)
            println("Mod ${entry.id} has successfully been initialized")

            ModInstance(entry, initializer)
        }.onFailure {
            println("Failed to load Solar Engine mod with id ${entry.id}")
            it.printStackTrace()
        }.getOrNull()
    }

private fun List<ModEntry>.ensureUnique(): List<ModEntry> {
    val set = hashSetOf<String>()
    val result = mutableListOf<ModEntry>()
    forEach { if (set.add(it.id)) result += it else println("Duplicate mod id ${it.id}, skipping it") }

    return result
}

@Serializable
data class ModEntry(
    val id: String,
    val version: String,
    val initializer: String,
    val isKotlin: Boolean,
    // Add packages (binary name) in here to prevent loading those classes
    val systemPackages: List<String> = listOf()
)
data class ModInstance(val entry: ModEntry, val initializer: ModInitializer)

interface ModInitializer {
    fun premain(inst: Instrumentation) {}
    fun lunarInitialized() {}
}

fun modsLunarInit() = mods.forEach { it.initializer.lunarInitialized() }