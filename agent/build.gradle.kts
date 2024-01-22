import java.security.MessageDigest

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.dokka")
}

val scripts: Configuration by configurations.creating { configurations.compileOnly.get().extendsFrom(this) }

dependencies {
    implementation(project(":util"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0-Beta")
    scripts("org.graalvm.js:js:22.3.1")

    implementation("net.java.jinput:jinput:2.0.9")
    implementation("net.java.jinput:jinput:2.0.9:natives-all")

    ktorClientImplementation("core", "cio", "websockets", "content-negotiation")
    ktorImplementation("serialization-kotlinx-json")
}

fun DependencyHandlerScope.ktorClientImplementation(vararg names: String) =
    names.forEach { ktorImplementation("client-$it") }

fun DependencyHandlerScope.ktorImplementation(name: String) = implementation(
    group = "io.ktor",
    name = "ktor-$name",
    version = "2.3.0"
)


kotlin { jvmToolchain(16) }

tasks {
    processResources {
        expand("version" to version)
    }

    jar {
        archiveBaseName.set("Solar-Engine")

        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        manifest {
            attributes(
                "Premain-Class" to "com.solartweaks.engine.AgentMainKt",
                "Main-Class" to "com.solartweaks.engine.Main"
            )
        }
    }

    register("updaterConfig") {
        dependsOn("jar")
        doLast {
            val engineFile = jar.get().outputs.files.singleFile
            val hash = MessageDigest.getInstance("SHA-1")
            val sha1 = hash.digest(engineFile.readBytes()).joinToString("") { "%02x".format(it) }

            File(buildDir, "updater.json").writeText(
                """
                {
                    "version": "$version",
                    "filename": "${engineFile.name}",
                    "sha1": "$sha1"
                }
                """.trimIndent()
            )
        }
    }

    fun javaRunTask(name: String, mainName: String) {
        register<JavaExec>(name) {
            dependsOn("jar")
            classpath(jar.get().outputs.files.singleFile.absolutePath)
            workingDir = rootDir
            mainClass.set("com.solartweaks.engine.$mainName")
        }
    }

    javaRunTask("generateConfigurations", "GenerateConfigurations")
    javaRunTask("generateFeatures", "GenerateFeatures")

    // This exists so the launcher can consume the api jar separately
    register<Jar>("jsAPIJar") {
        archiveBaseName.set("js-api")
        archiveVersion.set(provider { null })

        from(scripts.map { zipTree(it) })
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}