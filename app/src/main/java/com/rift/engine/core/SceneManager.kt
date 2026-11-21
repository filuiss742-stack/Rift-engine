package com.rift.engine.core

import android.graphics.Canvas
import com.rift.engine.diagnostics.DiagnosticReporter
import com.rift.engine.diagnostics.DiagnosticSeverity
import com.rift.engine.scene.Scene

class SceneManager(
    private val diagnostics: DiagnosticReporter = DiagnosticReporter()
) {
    var current: Scene? = null
        private set

    fun setInitialScene(scene: Scene) {
        if (current == null) {
            current = scene
            scene.onEnter()
        } else {
            diagnostics.report(
                DiagnosticSeverity.WARNING,
                "SCENE_INITIAL_ALREADY_SET",
                "SceneManager",
                "An initial scene is already active. Use changeScene() to replace it."
            )
        }
    }

    fun changeScene(scene: Scene) {
        current?.onExit()
        current = scene
        scene.onEnter()
    }

    fun update(deltaSeconds: Float) {
        current?.update(deltaSeconds)
    }

    fun render(canvas: Canvas) {
        val scene = current
        if (scene == null) {
            diagnostics.report(
                DiagnosticSeverity.ERROR,
                "SCENE_NOT_SET",
                "SceneManager",
                "No active scene is assigned. Call setInitialScene() before rendering."
            )
            return
        }
        scene.render(canvas)
    }

    fun onTouch(x: Float, y: Float, screenWidth: Float, screenHeight: Float) {
        current?.onTouch(x, y, screenWidth, screenHeight)
    }
}
