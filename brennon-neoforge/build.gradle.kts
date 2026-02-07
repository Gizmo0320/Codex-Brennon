import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("net.neoforged.gradle.userdev") version "7.0.80"
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

val shade: Configuration by configurations.creating
configurations.implementation.get().extendsFrom(shade)

runs {
    configureEach {
        systemProperty("forge.logging.markers", "REGISTRIES")
        systemProperty("forge.logging.console.level", "debug")
        modSource(project.sourceSets.main.get())
    }

    create("server") {
        systemProperty("forge.enabledGameTestNamespaces", "brennon")
    }
}

dependencies {
    implementation("net.neoforged:neoforge:20.4.167")

    shade(project(":brennon-api"))
    shade(project(":brennon-common"))
    shade(project(":brennon-core"))
    shade(project(":brennon-database"))
    shade(project(":brennon-messaging"))

    shade("net.kyori:adventure-api:4.15.0")
    shade("net.kyori:adventure-text-minimessage:4.15.0")
    shade("net.kyori:adventure-text-serializer-gson:4.15.0")
    shade("net.kyori:adventure-text-serializer-plain:4.15.0")

    shade(kotlin("stdlib"))
    shade(kotlin("reflect"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("slim")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    configurations = listOf(shade)

    relocate("redis.clients", "com.envarcade.brennon.libs.redis")
    relocate("com.zaxxer.hikari", "com.envarcade.brennon.libs.hikari")
    relocate("org.mongodb", "com.envarcade.brennon.libs.mongodb")
    relocate("org.apache.commons.pool2", "com.envarcade.brennon.libs.pool2")
    relocate("org.bson", "com.envarcade.brennon.libs.bson")
}

tasks.named("assemble") {
    dependsOn("shadowJar")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
