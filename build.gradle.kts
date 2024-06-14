group = "nl.knaw.huc.di"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.huygens.knaw.nl/repository/")
    }
}

plugins {
    application

    kotlin("jvm") version "1.9.23"
    kotlin("plugin.serialization") version "1.9.22"

    id("it.gianluz.capsule") version "1.0.3"
}

val ktorVersion: String by project
dependencies {
    implementation(kotlin("reflect"))
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("com.google.code.gson:gson:2.10")
    implementation("io.ktor:ktor-client-cio:${ktorVersion}")
    implementation("io.ktor:ktor-client-core:${ktorVersion}")
    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.4.0")
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.redundent:kotlin-xml-builder:1.9.1")
    implementation("nl.knaw.huygens:visitei:0.6.2")
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