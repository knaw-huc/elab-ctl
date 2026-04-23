pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
    }
    val shadowVersion: String by settings
    plugins {
        id("com.gradleup.shadow") version shadowVersion
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "elabctl"