package com.envarcade.brennon.api.module;

public interface Module {

    String getId();

    String getName();

    String getVersion();

    void onEnable();

    void onDisable();

    boolean isEnabled();
}
