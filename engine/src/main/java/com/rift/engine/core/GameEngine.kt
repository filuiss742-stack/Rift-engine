package com.rift.engine.core

import android.content.Context
import android.graphics.Canvas
import com.rift.engine.diagnostics.DiagnosticReporter
import com.rift.engine.diagnostics.DiagnosticsOverlay
import com.rift.engine.plugin.PluginManager
import com.rift.engine.storage.SaveStore
import java.util.concurrent.ConcurrentLinkedQueue

private data class TouchInput(
    val x: Float,
    val y: Float,
    val screenWidth: Float,
    val screenHeight: Float
)

class GameEngine(context: Context) {
    val diagnostics = DiagnosticReporter()
    val saves = SaveStore(context.applicationContext)
    val plugins = PluginManager(diagnostics)
    val scenes = SceneManager(diagnostics)

    private val pendingInput = ConcurrentLinkedQueue<TouchInput>()

    /**
     * Thread-safe input entry point. Events are applied on the game-loop thread
     * during update(), never directly from Android's UI thread.
     */
    fun onTouch(x: Float, y: Float, screenWidth: Float, screenHeight: Float) {
        if (!diagnostics.hasErrors()) {
            pendingInput.offer(TouchInput(x, y, screenWidth, screenHeight))
        }
    }

    fun update(deltaSeconds: Float) {
        if (diagnostics.hasErrors()) return

        runSafely("input.dispatch") {
            while (true) {
                val input = pendingInput.poll() ?: break
                scenes.onTouch(input.x, input.y, input.screenWidth, input.screenHeight)
                if (diagnostics.hasErrors()) break
            }
        }

        if (!diagnostics.hasErrors()) {
            runSafely("plugins.update") {
                plugins.onUpdate(deltaSeconds)
            }
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

    private inline fun runSafely(subsystem: String, operation: () -> Unit) {
        try {
            operation()
        } catch (exception: Exception) {
            diagnostics.reportException(subsystem, exception)
        }
    }
}
