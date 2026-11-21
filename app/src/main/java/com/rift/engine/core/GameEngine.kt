package com.rift.engine.core

import android.content.Context
import android.graphics.Canvas
import com.rift.engine.diagnostics.DiagnosticReporter
import com.rift.engine.diagnostics.DiagnosticsOverlay
import com.rift.engine.plugin.PluginManager
import com.rift.engine.storage.SaveStore

class GameEngine(context: Context) {
    val diagnostics = DiagnosticReporter()
    val saves = SaveStore(context.applicationContext)
    val plugins = PluginManager(diagnostics)
    val scenes = SceneManager(diagnostics)

    fun update(deltaSeconds: Float) {
        if (diagnostics.hasErrors()) return

        runSafely("plugins.update") {
            plugins.onUpdate(deltaSeconds)
        }
        if (!diagnostics.hasErrors()) {
            runSafely("scene.update") {
                scenes.update(deltaSeconds)
            }
        }
    }

    fun render(canvas: Canvas) {
        if (diagnostics.hasErrors()) {
            DiagnosticsOverlay.render(canvas, diagnostics.snapshot())
            return
        }

        runSafely("scene.render") {
            scenes.render(canvas)
        }

        if (diagnostics.hasErrors()) {
            DiagnosticsOverlay.render(canvas, diagnostics.snapshot())
        }
    }

    fun onTouch(x: Float, y: Float, screenWidth: Float, screenHeight: Float) {
        if (diagnostics.hasErrors()) return
        runSafely("scene.input") {
            scenes.onTouch(x, y, screenWidth, screenHeight)
        }
    }

    private inline fun runSafely(subsystem: String, operation: () -> Unit) {
        try {
            operation()
        } catch (exception: Exception) {
            diagnostics.reportException(subsystem, exception)
        }
    }
}
