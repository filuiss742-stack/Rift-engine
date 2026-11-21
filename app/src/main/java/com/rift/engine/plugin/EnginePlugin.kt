package com.rift.engine.plugin

interface EnginePlugin {
    val id: String
    fun onInstall() = Unit
    fun onUpdate(deltaSeconds: Float) = Unit
    fun onUninstall() = Unit
}

class PluginManager {
    private val plugins = linkedMapOf<String, EnginePlugin>()

    fun install(plugin: EnginePlugin) {
        if (plugins.containsKey(plugin.id)) return
        plugins[plugin.id] = plugin
        plugin.onInstall()
    }

    fun uninstall(id: String) {
        plugins.remove(id)?.onUninstall()
    }

    fun onUpdate(deltaSeconds: Float) {
        plugins.values.toList().forEach { it.onUpdate(deltaSeconds) }
    }

    fun all(): List<EnginePlugin> = plugins.values.toList()
}
