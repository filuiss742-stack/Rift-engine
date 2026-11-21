package com.rift.engine.plugin

import com.rift.engine.diagnostics.DiagnosticReporter
import com.rift.engine.diagnostics.DiagnosticSeverity

interface EnginePlugin {
    val id: String
    fun onInstall() = Unit
    fun onUpdate(deltaSeconds: Float) = Unit
    fun onUninstall() = Unit
}

class PluginManager(
    private val diagnostics: DiagnosticReporter = DiagnosticReporter()
) {
    private val plugins = linkedMapOf<String, EnginePlugin>()

    fun install(plugin: EnginePlugin) {
        if (plugin.id.isBlank()) {
            diagnostics.report(
                DiagnosticSeverity.ERROR,
                "PLUGIN_ID_EMPTY",
                "PluginManager",
                "A plugin must provide a non-empty stable ID."
            )
            return
        }

        if (plugins.containsKey(plugin.id)) {
            diagnostics.report(
                DiagnosticSeverity.WARNING,
                "PLUGIN_ID_CONFLICT",
                "PluginManager",
                "Plugin ID '${plugin.id}' is already installed. The new plugin was ignored."
            )
            return
        }

        plugins[plugin.id] = plugin
        try {
            plugin.onInstall()
        } catch (exception: Exception) {
            plugins.remove(plugin.id)
            diagnostics.reportException("plugin[${plugin.id}].onInstall", exception)
        }
    }

    fun uninstall(id: String) {
        val plugin = plugins.remove(id) ?: return
        try {
            plugin.onUninstall()
        } catch (exception: Exception) {
            diagnostics.reportException("plugin[$id].onUninstall", exception)
        }
    }

    fun onUpdate(deltaSeconds: Float) {
        plugins.values.toList().forEach { plugin ->
            try {
                plugin.onUpdate(deltaSeconds)
            } catch (exception: Exception) {
                diagnostics.reportException("plugin[${plugin.id}].onUpdate", exception)
            }
        }
    }

    fun all(): List<EnginePlugin> = plugins.values.toList()
}
