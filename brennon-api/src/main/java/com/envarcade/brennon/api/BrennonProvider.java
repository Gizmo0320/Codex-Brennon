package com.envarcade.brennon.api;

public final class BrennonProvider {

    private static BrennonAPI instance;

    private BrennonProvider() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static BrennonAPI get() {
        if (instance == null) {
            throw new IllegalStateException("Brennon API has not been initialized yet!");
        }
        return instance;
    }

    public static void register(BrennonAPI api) {
        if (instance != null) {
            throw new IllegalStateException("Brennon API has already been registered!");
        }
        instance = api;
    }

    public static void unregister() {
        instance = null;
    }
}
