plugins {
    kotlin("jvm")
}

version = "0.1"

dependencies {
    // You can add the agent as a jar (there is no maven repo)
    // in a real mod, if you want, or use this project as a git submodule
    implementation(project(":agent"))
}

kotlin { jvmToolchain(16) }

tasks {
    processResources {
        expand("version" to version)
    }
}