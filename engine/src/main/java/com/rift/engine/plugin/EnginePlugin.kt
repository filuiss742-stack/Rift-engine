package com.rift.engine.plugin

import com.rift.engine.diagnostics.DiagnosticReporter
import com.rift.engine.diagnostics.DiagnosticSeverity

interface EnginePlugin {
    val id: String
    fun onInstall() = Unit
    fun onUpdate(deltaSeconds: Float) = Unit
    fun onUninstall() = Unit
}

/**
 * Thread-safe lifecycle registry for engine plugins.
 *
 * Lifecycle callbacks are serialized with registry operations. A callback may
 * re-enter this manager on the same thread, but another thread cannot mutate
 * the registry halfway through an install, update, or uninstall operation.
 */
class PluginManager(
    private val diagnostics: DiagnosticReporter = DiagnosticReporter()
) {
    private val plugins = linkedMapOf<String, EnginePlugin>()
    private var closed = false

    @Synchronized
    fun install(plugin: EnginePlugin) {
        if (closed) {
            diagnostics.report(
                DiagnosticSeverity.ERROR,
                "PLUGIN_MANAGER_CLOSED",
                "PluginManager",
                "Cannot install a plugin after the manager has been closed."
            )
            return
        }
        val pluginId = plugin.id
        if (pluginId.isBlank()) {
            diagnostics.report(
                DiagnosticSeverity.ERROR,
                "PLUGIN_ID_EMPTY",
                "PluginManager",
                "A plugin must provide a non-empty stable ID."
            )
            return
        }

        if (plugins.containsKey(pluginId)) {
            diagnostics.report(
                DiagnosticSeverity.WARNING,
                "PLUGIN_ID_CONFLICT",
                "PluginManager",
                "Plugin ID '" + pluginId + "' is already installed. The new plugin was ignored."
            )
            return
        }

        plugins[pluginId] = plugin
        try {
            plugin.onInstall()
        } catch (exception: Exception) {
            if (plugins[pluginId] === plugin) {
                plugins.remove(pluginId)
            }
            try {
                plugin.onUninstall()
            } catch (cleanupException: Exception) {
                exception.addSuppressed(cleanupException)
                diagnostics.reportException("plugin[" + pluginId + "].failedInstallCleanup", cleanupException)
            }
            diagnostics.reportException("plugin[" + pluginId + "].onInstall", exception)
        }
    }

    @Synchronized
    fun uninstall(id: String) {
        val plugin = plugins.remove(id) ?: return
        try {
            plugin.onUninstall()
        } catch (exception: Exception) {
            diagnostics.reportException("plugin[" + id + "].onUninstall", exception)
        }
    }

    @Synchronized
    fun onUpdate(deltaSeconds: Float) {
        require(deltaSeconds.isFinite() && deltaSeconds >= 0f) {
            "deltaSeconds must be finite and non-negative."
        }

        if (closed) return

        // Snapshot iteration permits a callback to uninstall itself re-entrantly.
        val snapshot = plugins.values.toList()
        snapshot.forEach { plugin ->
            if (plugins[plugin.id] === plugin) {
                try {
                    plugin.onUpdate(deltaSeconds)
                } catch (exception: Exception) {
                    diagnostics.reportException("plugin[" + plugin.id + "].onUpdate", exception)
                }
            }
        }
    }

    /** Uninstalls every remaining plugin exactly once, even if one cleanup fails. */
    @Synchronized
    fun close() {
        if (closed) return
        closed = true

        plugins.values.toList().asReversed().forEach { plugin ->
            if (plugins[plugin.id] === plugin) {
                plugins.remove(plugin.id)
                try {
                    plugin.onUninstall()
                } catch (exception: Exception) {
                    diagnostics.reportException("plugin[${plugin.id}].onUninstall", exception)
                }
            }
        }
    }

    @Synchronized
    fun all(): List<EnginePlugin> = plugins.values.toList()
}
