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

    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "1.9.22"

    id("it.gianluz.capsule") version "1.0.3"
    id("com.github.johnrengelman.shadow") version "8.0.0"
}

buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("it.gianluz:gradle-capsule-plugin:1.0.3")
        classpath("gradle.plugin.com.github.johnrengelman:shadow:8.0.0")
    }
}

val ktorVersion: String by project
dependencies {
    implementation(kotlin("reflect"))

    implementation("ch.qos.logback:logback-classic:1.5.13")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")
    implementation("com.github.spullara.mustache.java:compiler:0.9.10")
    implementation("com.google.code.gson:gson:2.10")
    implementation("info.freelibrary:jiiify-presentation-v3:0.12.4") // iiif v3
    implementation("io.arrow-kt:arrow-core:2.0.1")
    implementation("io.arrow-kt:arrow-fx-coroutines:2.0.1")
    implementation("io.ktor:ktor-client-cio:${ktorVersion}")
    implementation("io.ktor:ktor-client-core:${ktorVersion}")
    implementation("nl.knaw.huygens:visitei:0.6.2")
    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.4.0")
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0") // 1.7.0 leads to compile error
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("org.redundent:kotlin-xml-builder:1.9.1")
    runtimeOnly("com.github.jai-imageio:jai-imageio-jpeg2000:1.4.0") // jpeg2000 handling in imageio

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(18)
}

application {
    mainClass = "nl.knaw.huc.di.elaborate.elabctl.ElabCtlKt"
    applicationName = "elabctl"
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass
    }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveFileName.set("${project.name}.jar")
}

