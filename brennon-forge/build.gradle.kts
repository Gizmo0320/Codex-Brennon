import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("net.minecraftforge.gradle") version "[6.0,6.2)"
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

val shade: Configuration by configurations.creating
configurations.implementation.get().extendsFrom(shade)

minecraft {
    mappings("official", "1.20.1")

    runs {
        create("server") {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            mods {
                create("brennon") {
                    source(sourceSets.main.get())
                }
            }
        }
    }
}

dependencies {
    minecraft("net.minecraftforge:forge:1.20.1-47.4.10")

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

    // Exclude libraries already provided by Forge
    exclude("org/slf4j/**")
    exclude("com/google/gson/**")
    exclude("META-INF/versions/**")

    finalizedBy("reobfShadowJar")
}

reobf {
    create("shadowJar")
}

tasks.named("assemble") {
    dependsOn("shadowJar")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
