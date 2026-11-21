package com.rift.engine.core

import com.rift.engine.diagnostics.DiagnosticReporter
import com.rift.engine.diagnostics.DiagnosticsOverlay
import com.rift.engine.graphics.RenderContext
import com.rift.engine.plugin.PluginManager
import com.rift.engine.module.GameplayModuleManager
import com.rift.engine.storage.InMemorySaveStore
import com.rift.engine.storage.SaveStore
import java.util.concurrent.ConcurrentLinkedQueue

private sealed interface InputEvent {
    data class Pointer(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
    ) : InputEvent

    data class Key(
        val code: Int,
        val pressed: Boolean
    ) : InputEvent

    data class Pointer(val event: PointerEvent) : InputEvent

    data object ResetInput : InputEvent
}

/**
 * Shared runtime. A platform host provides the game loop, renderer and storage.
 *
 * ERROR diagnostics remain visible but do not automatically stop the game.
 * Only an explicitly reported FATAL diagnostic pauses engine updates.
 */
class GameEngine(
    val saves: SaveStore = InMemorySaveStore(),
    val diagnostics: DiagnosticReporter = DiagnosticReporter()
) {
    val plugins = PluginManager(diagnostics)
    val gameplayModules = GameplayModuleManager(diagnostics)
    val scenes = SceneManager(diagnostics)

    private val inputs = ConcurrentLinkedQueue<InputEvent>()

    fun onTouch(x: Float, y: Float, width: Float, height: Float) {
        if (!diagnostics.hasFatalErrors()) {
            inputs.offer(InputEvent.Pointer(x, y, width, height))
        }
    }

    fun onKey(code: Int, pressed: Boolean) {
        if (!diagnostics.hasFatalErrors()) {
            inputs.offer(InputEvent.Key(code, pressed))
        }
    }

    fun onPointer(event: PointerEvent) {
        if (!diagnostics.hasFatalErrors()) {
            inputs.offer(InputEvent.Pointer(event))
        }
    }

    fun onFocusLost() {
        if (!diagnostics.hasFatalErrors()) {
            inputs.offer(InputEvent.ResetInput)
        }
    }

    fun update(deltaSeconds: Float) {
        if (diagnostics.hasFatalErrors()) {
            return
        }

        safe("input.dispatch") {
            while (true) {
                when (val event = inputs.poll() ?: break) {
                    is InputEvent.Pointer -> scenes.onTouch(
                        event.x,
                        event.y,
                        event.width,
                        event.height
                    )

                    is InputEvent.Key -> scenes.onKey(event.code, event.pressed)
                    is InputEvent.Pointer -> scenes.onPointer(event.event)
                    InputEvent.ResetInput -> scenes.resetInput()
                }

                if (diagnostics.hasFatalErrors()) {
                    break
                }
            }
        }

        if (!diagnostics.hasFatalErrors()) {
            safe("plugins.update") {
                plugins.onUpdate(deltaSeconds)
            }
        }

        if (!diagnostics.hasFatalErrors()) {
            safe("gameplayModules.update") {
                gameplayModules.update(deltaSeconds)
            }
        }

        if (!diagnostics.hasFatalErrors()) {
            safe("scene.update") {
                scenes.update(deltaSeconds)
            }
        }
    }

    fun render(context: RenderContext) {
        if (diagnostics.hasFatalErrors()) {
            DiagnosticsOverlay.render(context, diagnostics.snapshot())
            return
        }

        safe("scene.render") {
            scenes.render(context)
        }

        if (diagnostics.hasErrors()) {
            DiagnosticsOverlay.render(context, diagnostics.snapshot())
        }
    }

    private inline fun safe(
        subsystem: String,
        block: () -> Unit
    ) {
        try {
            block()
        } catch (exception: Exception) {
            diagnostics.reportException(subsystem, exception)
        }
    }
}
