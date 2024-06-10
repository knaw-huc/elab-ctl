group = "nl.knaw.huc.di"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

plugins {
    application

    kotlin("jvm") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.22"

    id("it.gianluz.capsule") version "1.0.3"
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.4.0")
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(18)
}

application {
    mainClass = "nl.knaw.huc.di.elaborate.elabctl.ElabCtlKt"
    applicationName = "elabctl"
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass
    }
}
//tasks.create<FatCapsule>("createExecutable") {
//    group = "Distribution"
//    description = "Package into a executable fat jar"
//    applicationClass(application.mainClass.toString())
//    reallyExecutable
//    archiveFileName = application.applicationName
//}

buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("it.gianluz:gradle-capsule-plugin:1.0.3")
    }
}

//apply(plugin = "it.gianluz.capsule")