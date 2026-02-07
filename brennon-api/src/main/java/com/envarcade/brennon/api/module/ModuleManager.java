package com.envarcade.brennon.api.module;

import java.util.Collection;
import java.util.Optional;

public interface ModuleManager {

    void registerModule(Module module);

    Optional<Module> getModule(String id);

    Collection<Module> getModules();

    void enableModule(String id);

    void disableModule(String id);

    Collection<Module> getEnabledModules();
}
