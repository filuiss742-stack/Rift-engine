package com.rift.engine.module

import com.rift.engine.diagnostics.DiagnosticReporter
import com.rift.engine.diagnostics.DiagnosticSeverity

/** Runtime registry for optional gameplay modules. */
class GameplayModuleManager(
    private val diagnostics: DiagnosticReporter = DiagnosticReporter()
) {
    private val modules = linkedMapOf<String, GameplayModule>()

    @Synchronized
    fun install(module: GameplayModule): Boolean {
        val descriptor = module.descriptor
        if (!validateDescriptor(descriptor)) return false

        if (modules.containsKey(descriptor.id)) {
            report("MODULE_ID_ALREADY_INSTALLED",
                "Module '" + descriptor.id + "' is already installed. Use replace() explicitly.")
            return false
        }
        if (!validateCompatibility(descriptor, modules)) return false

        return try {
            module.onInstall(GameplayModuleContext(diagnostics))
            modules[descriptor.id] = module
            true
        } catch (exception: Exception) {
            diagnostics.reportException("module[" + descriptor.id + "].onInstall", exception)
            attemptCleanup(module, descriptor.id, "failed install")
            false
        }
    }

    /**
     * Replaces an implementation while preserving its stable module ID.
     * If activation fails, the manager attempts to reactivate the old module.
     */
    @Synchronized
    fun replace(id: String, replacement: GameplayModule): Boolean {
        val current = modules[id]
        if (current == null) {
            report("MODULE_TO_REPLACE_NOT_FOUND",
                "Cannot replace '" + id + "' because it is not installed.")
            return false
        }

        val descriptor = replacement.descriptor
        if (descriptor.id != id) {
            report("MODULE_REPLACEMENT_ID_MISMATCH",
                "Replacement ID '" + descriptor.id + "' must match '" + id + "'.")
            return false
        }
        if (!validateDescriptor(descriptor)) return false

        val remaining = modules.filterKeys { it != id }
        if (!validateCompatibility(descriptor, remaining)) return false

        try {
            current.onUninstall()
        } catch (exception: Exception) {
            diagnostics.reportException("module[" + id + "].onUninstallForReplace", exception)
            return false
        }

        try {
            replacement.onInstall(GameplayModuleContext(diagnostics))
        } catch (exception: Exception) {
            diagnostics.reportException("module[" + id + "].replacement.onInstall", exception)
            attemptCleanup(replacement, id, "failed replacement")
            try {
                current.onInstall(GameplayModuleContext(diagnostics))
            } catch (restoreException: Exception) {
                diagnostics.report(
                    DiagnosticSeverity.ERROR,
                    "MODULE_RESTORE_FAILED",
                    "GameplayModuleManager",
                    "Replacement failed and the previous module could not be reactivated.",
                    restoreException.toString(),
                    "module[" + id + "]"
                )
            }
            return false
        }

        modules[id] = replacement
        return true
    }

    @Synchronized
    fun uninstall(id: String): Boolean {
        val module = modules[id] ?: return false
        val dependents = modules.values
            .filter { it.descriptor.id != id && id in it.descriptor.dependencies }
            .map { it.descriptor.id }

        if (dependents.isNotEmpty()) {
            report("MODULE_REQUIRED_BY_INSTALLED",
                "Module '" + id + "' is required by: " + dependents.joinToString() + ".")
            return false
        }

        return try {
            module.onUninstall()
            modules.remove(id)
            true
        } catch (exception: Exception) {
            diagnostics.reportException("module[" + id + "].onUninstall", exception)
            false
        }
    }

    fun update(deltaSeconds: Float) {
        require(deltaSeconds >= 0f && deltaSeconds.isFinite()) {
            "deltaSeconds must be finite and non-negative."
        }

        val snapshot = synchronized(this) { modules.values.toList() }
        snapshot.forEach { module ->
            val stillInstalled = synchronized(this) {
                modules[module.descriptor.id] === module
            }
            if (stillInstalled) {
                try {
                    module.onUpdate(deltaSeconds)
                } catch (exception: Exception) {
                    diagnostics.reportException(
                        "module[" + module.descriptor.id + "].onUpdate",
                        exception
                    )
                }
            }
        }
    }

    @Synchronized
    fun isInstalled(id: String): Boolean = modules.containsKey(id)

    @Synchronized
    fun hasCapability(capability: String): Boolean {
        return modules.values.any { capability in it.descriptor.capabilities }
    }

    @Synchronized
    fun installed(): List<GameplayModuleDescriptor> {
        return modules.values.map { it.descriptor }
    }

    private fun validateDescriptor(descriptor: GameplayModuleDescriptor): Boolean {
        if (descriptor.id.isBlank() || descriptor.version.isBlank()) {
            report("MODULE_DESCRIPTOR_INVALID", "A module needs a non-blank ID and version.")
            return false
        }
        if (descriptor.id in descriptor.dependencies ||
            descriptor.id in descriptor.conflicts ||
            descriptor.dependencies.any { it.isBlank() } ||
            descriptor.conflicts.any { it.isBlank() } ||
            descriptor.capabilities.any { it.isBlank() }
        ) {
            report("MODULE_DESCRIPTOR_INVALID",
                "Module '" + descriptor.id + "' contains a blank entry or self-reference.")
            return false
        }
        if (descriptor.dependencies.intersect(descriptor.conflicts).isNotEmpty()) {
            report("MODULE_DESCRIPTOR_INVALID",
                "A module cannot both require and conflict with the same module.")
            return false
        }
        return true
    }

    private fun validateCompatibility(
        candidate: GameplayModuleDescriptor,
        installedModules: Map<String, GameplayModule>
    ): Boolean {
        val missing = candidate.dependencies - installedModules.keys
        if (missing.isNotEmpty()) {
            report("MODULE_DEPENDENCY_MISSING",
                "Module '" + candidate.id + "' requires: " + missing.joinToString() + ".")
            return false
        }

        val direct = candidate.conflicts.intersect(installedModules.keys)
        val reverse = installedModules.values
            .filter { candidate.id in it.descriptor.conflicts }
            .map { it.descriptor.id }
            .toSet()
        val conflicts = direct + reverse
        if (conflicts.isNotEmpty()) {
            report("MODULE_CONFLICT",
                "Module '" + candidate.id + "' conflicts with: " + conflicts.joinToString() + ".")
            return false
        }
        return true
    }

    private fun attemptCleanup(module: GameplayModule, id: String, operation: String) {
        try {
            module.onUninstall()
        } catch (cleanupException: Exception) {
            diagnostics.report(
                DiagnosticSeverity.ERROR,
                "MODULE_CLEANUP_FAILED",
                "GameplayModuleManager",
                "Module '" + id + "' failed cleanup after " + operation + ".",
                cleanupException.toString(),
                "module[" + id + "]"
            )
        }
    }

    private fun report(code: String, message: String) {
        diagnostics.report(
            DiagnosticSeverity.ERROR,
            code,
            "GameplayModuleManager",
            message
        )
    }
}
