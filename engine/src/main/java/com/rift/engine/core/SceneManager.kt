package com.rift.engine.core

import com.rift.engine.diagnostics.DiagnosticReporter
import com.rift.engine.diagnostics.DiagnosticSeverity
import com.rift.engine.graphics.RenderContext
import com.rift.engine.scene.Scene

class SceneManager(
    private val diagnostics: DiagnosticReporter = DiagnosticReporter()
) {
    var current: Scene? = null
        private set

    fun setInitialScene(scene: Scene) {
        if (current == null) {
            current = scene
            safe("scene.onEnter") {
                scene.onEnter()
            }
        } else {
            diagnostics.report(
                DiagnosticSeverity.WARNING,
                "SCENE_INITIAL_ALREADY_SET",
                "SceneManager",
                "Use changeScene() to replace the active scene."
            )
        }
    }

    fun changeScene(scene: Scene) {
        current?.let { previousScene ->
            safe("scene.onExit") {
                previousScene.onExit()
            }
        }

        current = scene
        safe("scene.onEnter") {
            scene.onEnter()
        }
    }

    fun update(deltaSeconds: Float) {
        current?.update(deltaSeconds)
    }

    fun render(context: RenderContext) {
        val activeScene = current
        if (activeScene == null) {
            diagnostics.report(
                DiagnosticSeverity.ERROR,
                "SCENE_NOT_SET",
                "SceneManager",
                "Call setInitialScene() before rendering."
            )
            return
        }

        activeScene.render(context)
    }

    fun onTouch(
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        current?.onTouch(x, y, width, height)
    }

    fun onKey(
        keyCode: Int,
        pressed: Boolean
    ) {
        current?.onKey(keyCode, pressed)
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
