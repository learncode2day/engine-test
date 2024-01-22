plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":util"))
}

kotlin { jvmToolchain(16) }

tasks {
    jar {
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        manifest {
            attributes(
                "Agent-Class" to "com.solartweaks.engine.DumperMainKt",
                "Premain-Class" to "com.solartweaks.engine.DumperMainKt",
            )
        }
    }
}