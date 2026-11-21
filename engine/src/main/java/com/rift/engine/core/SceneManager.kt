package com.rift.engine.core

import com.rift.engine.diagnostics.DiagnosticReporter
import com.rift.engine.diagnostics.DiagnosticSeverity
import com.rift.engine.graphics.RenderContext
import com.rift.engine.input.InputActionMap
import com.rift.engine.scene.Scene
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Owns the active scene and serializes scene transitions at safe runtime
 * boundaries. Requests may come from a scene callback or another thread;
 * they are applied between callbacks, never halfway through one.
 */
class SceneManager(
    private val diagnostics: DiagnosticReporter = DiagnosticReporter()
) {
    @Volatile
    var current: Scene? = null
        private set

    private val pendingScenes = ConcurrentLinkedQueue<Scene>()
    private val heldKeys = mutableSetOf<Int>()

    @Synchronized
    fun setInitialScene(scene: Scene) {
        if (current == null && pendingScenes.isEmpty()) {
            current = scene
            scene.input.reset()
            safe("scene.onEnter") {
                scene.onEnter()
            }
            syncHeldKeys(scene)
        } else {
            diagnostics.report(
                DiagnosticSeverity.WARNING,
                "SCENE_INITIAL_ALREADY_SET",
                "SceneManager",
                "The initial scene is already set or a scene transition is pending. Use changeScene() to replace it."
            )
        }
    }

    /**
     * Queues a scene transition. The current scene remains active until the
     * current lifecycle, update, render, or input callback has returned.
     */
    fun changeScene(scene: Scene) {
        pendingScenes.offer(scene)
    }

    fun update(deltaSeconds: Float) {
        applyPendingTransitions()
        val activeScene = current ?: return
        try {
            safe("scene.update") {
                activeScene.update(deltaSeconds)
            }
        } finally {
            activeScene.input.endFrame()
        }
        applyPendingTransitions()
    }

    fun render(context: RenderContext) {
        applyPendingTransitions()
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

        safe("scene.render") {
            activeScene.render(context)
        }
        applyPendingTransitions()
    }

    fun onTouch(
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        applyPendingTransitions()
        val activeScene = current ?: return
        safe("scene.input.pointer") {
            activeScene.onTouch(x, y, width, height)
        }
        applyPendingTransitions()
    }

    fun onKey(
        keyCode: Int,
        pressed: Boolean
    ) {
        applyPendingTransitions()
        if (pressed) {
            heldKeys.add(keyCode)
        } else {
            heldKeys.remove(keyCode)
        }

        val activeScene = current ?: return
        activeScene.input.onKey(keyCode, pressed)
        safe("scene.input.key") {
            activeScene.onKey(keyCode, pressed)
        }
        applyPendingTransitions()
    }

    private fun applyPendingTransitions() {
        var applied = 0
        while (true) {
            val nextScene = pendingScenes.poll() ?: return
            val previousScene = current
            if (previousScene === nextScene) {
                continue
            }

            if (previousScene != null) {
                safe("scene.onExit") {
                    previousScene.onExit()
                }
            }

            current = nextScene
            nextScene.input.reset()
            safe("scene.onEnter") {
                nextScene.onEnter()
            }
            syncHeldKeys(nextScene)

            applied += 1
            if (applied >= MAX_TRANSITIONS_PER_BOUNDARY && pendingScenes.isNotEmpty()) {
                diagnostics.report(
                    DiagnosticSeverity.WARNING,
                    "SCENE_TRANSITION_LIMIT_REACHED",
                    "SceneManager",
                    "More than " + MAX_TRANSITIONS_PER_BOUNDARY +
                        " scene transitions were requested at one runtime boundary. Remaining transitions are deferred."
                )
                return
            }
        }
    }

    private fun syncHeldKeys(scene: Scene) {
        heldKeys.forEach { keyCode ->
            scene.input.onKey(keyCode, true)
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

    private companion object {
        const val MAX_TRANSITIONS_PER_BOUNDARY = 32
    }
}
