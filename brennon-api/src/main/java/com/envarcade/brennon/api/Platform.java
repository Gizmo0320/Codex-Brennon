package com.envarcade.brennon.api;

/**
 * Represents the platform Brennon is running on.
 *
 * Brennon supports a wide range of Minecraft server implementations
 * across both proxy-layer and server-layer platforms.
 */
public enum Platform {

    // Server platforms
    PAPER("Paper", PlatformType.SERVER),
    FOLIA("Folia", PlatformType.SERVER),
    FORGE("Forge", PlatformType.SERVER),
    NEOFORGE("NeoForge", PlatformType.SERVER),
    SPONGE("Sponge", PlatformType.SERVER),

    // Proxy platforms
    VELOCITY("Velocity", PlatformType.PROXY),

    // Non-Minecraft
    STANDALONE("Standalone", PlatformType.STANDALONE);

    private final String displayName;
    private final PlatformType type;

    Platform(String displayName, PlatformType type) {
        this.displayName = displayName;
        this.type = type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public PlatformType getType() {
        return type;
    }

    public boolean isServer() {
        return type == PlatformType.SERVER;
    }

    public boolean isProxy() {
        return type == PlatformType.PROXY;
    }

    /**
     * Whether this platform supports Bukkit-based APIs (Paper, Folia).
     */
    public boolean isBukkitBased() {
        return this == PAPER || this == FOLIA;
    }

    /**
     * Whether this platform supports the Adventure text API natively.
     */
    public boolean hasNativeAdventure() {
        return this == PAPER || this == FOLIA || this == VELOCITY || this == SPONGE;
    }

    /**
     * Whether this platform supports Forge-based APIs (Forge, NeoForge).
     */
    public boolean isForgeBased() {
        return this == FORGE || this == NEOFORGE;
    }

    public enum PlatformType {
        SERVER,
        PROXY,
        STANDALONE
    }
}
