allprojects {
    group = "com.solartweaks"
    version = "2.1.5"

    repositories {
        mavenCentral()
    }
}

plugins {
    kotlin("jvm") version "1.8.20" apply false
    kotlin("plugin.serialization") version "1.8.20" apply false
    id("org.jetbrains.dokka") version "1.8.10" apply false
}

tasks {
    register("release") {
        dependsOn(":agent:updaterConfig", ":agent:generateConfigurations", ":agent:generateFeatures")
    }
}