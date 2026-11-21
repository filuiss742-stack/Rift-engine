package com.rift.engine.module

import com.rift.engine.diagnostics.DiagnosticReporter

data class GameplayModuleDescriptor(
    val id: String,
    val version: String,
    val dependencies: Set<String> = emptySet(),
    val conflicts: Set<String> = emptySet(),
    val capabilities: Set<String> = emptySet()
)

data class GameplayModuleContext(
    val diagnostics: DiagnosticReporter
)

/**
 * Optional, replaceable gameplay pack.
 *
 * Module code must be included in the application build. This contract does
 * not dynamically download or load arbitrary binaries.
 */
interface GameplayModule {
    val descriptor: GameplayModuleDescriptor

    fun onInstall(context: GameplayModuleContext) = Unit
    fun onUpdate(deltaSeconds: Float) = Unit
    fun onUninstall() = Unit
}
