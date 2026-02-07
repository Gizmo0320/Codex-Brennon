import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.9.22" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
}

// ============================================================
// Dependency versions
// ============================================================
val adventureVersion = "4.15.0"
val gsonVersion = "2.10.1"
val jedisVersion = "5.1.0"
val hikariVersion = "5.1.0"
val mongoVersion = "4.11.1"
val mysqlVersion = "8.3.0"
val postgresVersion = "42.7.2"
val velocityVersion = "3.3.0-SNAPSHOT"
val paperVersion = "1.20.4-R0.1-SNAPSHOT"
val spongeVersion = "8.2.0"
val jdaVersion = "5.0.0-beta.20"
val sparkVersion = "2.9.4"   // SparkJava for REST API
val javalinVersion = "6.1.3"
val jwtVersion = "4.4.0"
val jlineVersion = "3.25.1"
val luckPermsVersion = "5.5"

// Modules that use their own Gradle toolchains (ForgeGradle / NeoGradle)
val modLoaderModules = setOf("brennon-forge", "brennon-neoforge")

allprojects {
    group = "com.envarcade.brennon"
    version = "2.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://repo.spongepowered.org/repository/maven-public/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.minecraftforge.net/")
        maven("https://repo.dmulloy2.net/repository/public/")
    }
}

// Standard subprojects (everything except Forge/NeoForge which manage their own plugins)
configure(subprojects.filter { it.name !in modLoaderModules }) {
    apply(plugin = "java")
    apply(plugin = "java-library")

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    dependencies {
        add("compileOnly", "net.kyori:adventure-api:$adventureVersion")
        add("compileOnly", "net.kyori:adventure-text-minimessage:$adventureVersion")
        add("compileOnly", "com.google.code.gson:gson:$gsonVersion")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}

// Apply Kotlin to all modules except brennon-api (pure Java) and mod loader modules
configure(subprojects.filter { it.name != "brennon-api" && it.name !in modLoaderModules }) {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        add("implementation", kotlin("stdlib"))
        add("implementation", kotlin("reflect"))
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }
}

// ============================================================
// Shared modules
// ============================================================

project(":brennon-common") {
    dependencies {
        add("api", project(":brennon-api"))
        add("implementation", "com.google.code.gson:gson:$gsonVersion")
    }
}

project(":brennon-database") {
    dependencies {
        add("api", project(":brennon-api"))
        add("implementation", project(":brennon-common"))
        add("implementation", "com.zaxxer:HikariCP:$hikariVersion")
        add("implementation", "org.mongodb:mongodb-driver-sync:$mongoVersion")
        add("runtimeOnly", "com.mysql:mysql-connector-j:$mysqlVersion")
        add("runtimeOnly", "org.postgresql:postgresql:$postgresVersion")
    }
}

project(":brennon-messaging") {
    dependencies {
        add("api", project(":brennon-api"))
        add("implementation", project(":brennon-common"))
        // api: JedisPool is exposed via getPool() — consumers need the Jedis types
        add("api", "redis.clients:jedis:$jedisVersion")
    }
}

project(":brennon-core") {
    dependencies {
        add("api", project(":brennon-api"))
        add("implementation", project(":brennon-common"))
        add("implementation", project(":brennon-database"))
        add("implementation", project(":brennon-messaging"))
        add("implementation", "com.google.code.gson:gson:$gsonVersion")
        add("compileOnly", "net.luckperms:api:$luckPermsVersion")
    }
}

// ============================================================
// Server platform modules
// ============================================================

fun Project.applyPlatformDeps() {
    apply(plugin = "com.github.johnrengelman.shadow")
    dependencies {
        add("implementation", project(":brennon-api"))
        add("implementation", project(":brennon-common"))
        add("implementation", project(":brennon-core"))
        add("implementation", project(":brennon-database"))
        add("implementation", project(":brennon-messaging"))
    }

    tasks.named<ShadowJar>("shadowJar") {
        archiveClassifier.set("")
        // Relocate internal libraries to avoid classpath conflicts with other plugins
        relocate("redis.clients", "com.envarcade.brennon.libs.redis")
        relocate("com.zaxxer.hikari", "com.envarcade.brennon.libs.hikari")
        relocate("org.mongodb", "com.envarcade.brennon.libs.mongodb")
        relocate("org.apache.commons.pool2", "com.envarcade.brennon.libs.pool2")
        relocate("org.bson", "com.envarcade.brennon.libs.bson")
        // Do NOT relocate Gson, Adventure, or Kotlin — shared with platform code
    }
}

project(":brennon-bukkit") {
    applyPlatformDeps()
    dependencies {
        add("compileOnly", "io.papermc.paper:paper-api:$paperVersion")
    }
}

project(":brennon-folia") {
    applyPlatformDeps()
    dependencies {
        // Folia is a Paper fork — same API, different scheduler
        add("compileOnly", "io.papermc.paper:paper-api:$paperVersion")
    }
}

// brennon-forge and brennon-neoforge are configured in their own build.gradle.kts
// using ForgeGradle and NeoGradle respectively

project(":brennon-sponge") {
    applyPlatformDeps()
    dependencies {
        add("compileOnly", "org.spongepowered:spongeapi:$spongeVersion")
    }
}

// ============================================================
// Proxy platform modules
// ============================================================

project(":brennon-proxy") {
    applyPlatformDeps()
    dependencies {
        add("compileOnly", "com.velocitypowered:velocity-api:$velocityVersion")
        add("annotationProcessor", "com.velocitypowered:velocity-api:$velocityVersion")
    }
}

// ============================================================
// Integration modules
// ============================================================

project(":brennon-discord") {
    applyPlatformDeps()
    dependencies {
        add("implementation", "net.dv8tion:JDA:$jdaVersion") {
            exclude(module = "opus-java")
        }
        // Adventure not provided by JDA — include in fat JAR
        add("implementation", "net.kyori:adventure-api:$adventureVersion")
        add("implementation", "net.kyori:adventure-text-minimessage:$adventureVersion")
    }
}

project(":brennon-web") {
    applyPlatformDeps()
    dependencies {
        add("implementation", "com.sparkjava:spark-core:$sparkVersion")
        add("implementation", "com.google.code.gson:gson:$gsonVersion")
        // Adventure not provided by SparkJava — include in fat JAR
        add("implementation", "net.kyori:adventure-api:$adventureVersion")
        add("implementation", "net.kyori:adventure-text-minimessage:$adventureVersion")
    }
}

// ============================================================
// Standalone
// ============================================================

project(":brennon-standalone") {
    applyPlatformDeps()
    dependencies {
        add("implementation", "org.jline:jline:$jlineVersion")
        // Adventure not provided at runtime — include in fat JAR
        add("implementation", "net.kyori:adventure-api:$adventureVersion")
        add("implementation", "net.kyori:adventure-text-minimessage:$adventureVersion")
    }

    tasks.named<ShadowJar>("shadowJar") {
        manifest {
            attributes["Main-Class"] = "com.envarcade.brennon.standalone.BrennonStandaloneKt"
        }
    }
}

// ============================================================
// Web Server (full-stack dashboard)
// ============================================================

project(":brennon-webserver") {
    applyPlatformDeps()
    dependencies {
        add("implementation", "io.javalin:javalin:$javalinVersion")
        add("implementation", "com.auth0:java-jwt:$jwtVersion")
        add("implementation", "com.google.code.gson:gson:$gsonVersion")
        add("implementation", "org.mongodb:mongodb-driver-sync:$mongoVersion")
        // Adventure not provided at runtime — include in fat JAR
        add("implementation", "net.kyori:adventure-api:$adventureVersion")
        add("implementation", "net.kyori:adventure-text-minimessage:$adventureVersion")
        add("implementation", "org.slf4j:slf4j-simple:2.0.9")
    }

    tasks.named<ShadowJar>("shadowJar") {
        manifest {
            attributes["Main-Class"] = "com.envarcade.brennon.webserver.BrennonWebServerKt"
        }
    }
}
