package com.solartweaks.engine

import java.io.InputStream
import java.nio.file.Path
import java.util.*
import kotlin.properties.ReadOnlyProperty

private object VersionDummy

val engineVersion by lazy {
    VersionDummy::class.java.classLoader.getResourceAsStream("version.txt")?.readBytes()?.decodeToString() ?: "unknown"
}

private val lunarVersion = findLunarClass {
    strings has "https://launchermeta.mojang.com/mc/game/version_manifest.json"
    fields { named("id") }
}.also { stringDataFinder("MinecraftVersion.MinecraftVersionBuilder").addBuilderFields(it) }

private val versionAccess by accessor<_, LunarVersion.Static>(lunarVersion, allowNotImplemented = true)

interface LunarVersion : InstanceAccessor {
    val id: String
    val ordinal: Int
    val snapshot: Boolean
//    val optifine: String?
    val optifineHash: String?
    val optifineNamespacedJar: Boolean
    val launchClass: String
    val coreOpenGL: Boolean
    val latest: Boolean
    val javaVersion: Int
    val type: String
    val majorVersion: String
    val exactVersion: String
    val displayName: String?
    val assetIndex: String
    val dependencies: List<String>
    val versionModule: String?
    val defaultMinorVersion: Boolean
    val dependencyPaths: List<Path>

    interface Static : StaticAccessor<LunarVersion>
    companion object : Static by versionAccess.static
}

val LunarVersion.formatted get() = id.drop(1).replace('_', '.')

// Lunar build data
abstract class PropertyHolder(propertiesLoader: () -> Properties) {
    private val lazyProperties by lazy(propertiesLoader)
    fun property() = ReadOnlyProperty<Any?, String?> { _, prop -> lazyProperties.getProperty(prop.name) }
}

fun InputStream.loadProperties() = Properties().also { it.load(this) }

object LunarBuildData : PropertyHolder({
    (VersionDummy::class.java.classLoader.getResourceAsStream("lunarBuildData.txt") ?: error("No lunar build data!"))
        .loadProperties()
}) {
    val gitBranch by property()
    val gitHash by property()
    val fullGitHash by property()
    val production by property()
    val proguardUuid by property()
    val lunarVersion by property()
}