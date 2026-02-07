pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.neoforged.net/releases/")
    }
}

rootProject.name = "Brennon"

include(
    // API & shared
    "brennon-api",
    "brennon-common",

    // Infrastructure
    "brennon-database",
    "brennon-messaging",

    // Core implementation
    "brennon-core",

    // Server platform modules
    "brennon-bukkit",       // Paper support
    "brennon-folia",        // Folia support (region-aware)
    "brennon-forge",        // Forge support
    "brennon-neoforge",     // NeoForge support
    "brennon-sponge",       // Sponge support

    // Proxy platform modules
    "brennon-proxy",        // Velocity support

    // Integration modules
    "brennon-discord",      // Discord bot & webhooks
    "brennon-web",          // REST API / web panel backend

    // Standalone
    "brennon-standalone",

    // Web server (full-stack dashboard)
    "brennon-webserver"
)
