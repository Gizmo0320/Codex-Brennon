package com.envarcade.brennon.core.module

import com.envarcade.brennon.api.module.Module
import com.envarcade.brennon.api.module.ModuleManager
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

/**
 * Core implementation of the module manager.
 *
 * Manages the lifecycle of Brennon modules — registration,
 * enable/disable, and lookup.
 */
class CoreModuleManager : ModuleManager {

    private val modules = ConcurrentHashMap<String, Module>()

    override fun registerModule(module: Module) {
        if (modules.containsKey(module.id)) {
            println("[Brennon] Module '${module.id}' is already registered, skipping.")
            return
        }

        modules[module.id] = module

        try {
            module.onEnable()
            println("[Brennon] Module '${module.name}' v${module.version} enabled.")
        } catch (e: Exception) {
            println("[Brennon] Failed to enable module '${module.name}': ${e.message}")
            e.printStackTrace()
        }
    }

    override fun getModule(id: String): Optional<Module> =
        Optional.ofNullable(modules[id])

    override fun getModules(): Collection<Module> =
        modules.values.toList()

    override fun enableModule(id: String) {
        val module = modules[id]
            ?: throw IllegalArgumentException("Module '$id' is not registered.")

        if (module.isEnabled) {
            println("[Brennon] Module '$id' is already enabled.")
            return
        }

        try {
            module.onEnable()
            println("[Brennon] Module '${module.name}' enabled.")
        } catch (e: Exception) {
            println("[Brennon] Failed to enable module '${module.name}': ${e.message}")
            e.printStackTrace()
        }
    }

    override fun disableModule(id: String) {
        val module = modules[id]
            ?: throw IllegalArgumentException("Module '$id' is not registered.")

        if (!module.isEnabled) {
            println("[Brennon] Module '$id' is already disabled.")
            return
        }

        try {
            module.onDisable()
            println("[Brennon] Module '${module.name}' disabled.")
        } catch (e: Exception) {
            println("[Brennon] Failed to disable module '${module.name}': ${e.message}")
            e.printStackTrace()
        }
    }

    override fun getEnabledModules(): Collection<Module> =
        modules.values.filter { it.isEnabled }

    /**
     * Disables all modules. Called during shutdown.
     */
    fun disableAll() {
        modules.values.filter { it.isEnabled }.forEach { module ->
            try {
                module.onDisable()
                println("[Brennon] Module '${module.name}' disabled.")
            } catch (e: Exception) {
                println("[Brennon] Error disabling module '${module.name}': ${e.message}")
            }
        }
        modules.clear()
    }
}
