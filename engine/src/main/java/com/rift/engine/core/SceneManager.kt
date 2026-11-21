package com.rift.engine.core

import com.rift.engine.assets.AssetManager
import com.rift.engine.audio.AudioManager
import com.rift.engine.diagnostics.DiagnosticReporter
import com.rift.engine.diagnostics.DiagnosticSeverity
import com.rift.engine.graphics.RenderContext
import com.rift.engine.input.PointerEvent
import com.rift.engine.input.PointerEventType
import com.rift.engine.scene.Scene
import com.rift.engine.state.GameState
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Owns the active scene and serializes scene transitions at safe runtime
 * boundaries. Requests may come from a scene callback or another thread;
 * they are applied between callbacks, never halfway through one.
 */
class SceneManager(
    private val diagnostics: DiagnosticReporter = DiagnosticReporter(),
    private val assets: AssetManager = AssetManager(diagnostics = diagnostics),
    private val audio: AudioManager = AudioManager(diagnostics = diagnostics)
) {
    @Volatile
    var current: Scene? = null
        private set

    val gameState = GameState()

    private val pendingScenes = ConcurrentLinkedQueue<Scene>()
    private val heldKeys = mutableSetOf<Int>()
    private val knownScenes = Collections.newSetFromMap(IdentityHashMap<Scene, Boolean>())
    @Volatile private var closed = false

    @Synchronized
    fun setInitialScene(scene: Scene) {
        check(!closed) { "SceneManager is closed." }
        if (current == null && pendingScenes.isEmpty()) {
            current = scene
            knownScenes.add(scene)
            scene.attachRuntime(diagnostics, gameState, assets, audio, ::changeScene)
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
        if (closed) {
            diagnostics.report(
                DiagnosticSeverity.WARNING,
                "SCENE_MANAGER_CLOSED",
                "SceneManager",
                "A scene transition was requested after the SceneManager was closed."
            )
            return
        }
        pendingScenes.offer(scene)
    }

    @Synchronized
    fun close() {
        if (closed) return
        closed = true
        pendingScenes.clear()
        val activeScene = current
        if (activeScene != null) {
            activeScene.ui.resetInput()
            safe("scene.onExit") { activeScene.onExit() }
        }
        current = null
        heldKeys.clear()
        val scenesToDispose = knownScenes.toList()
        knownScenes.clear()
        scenesToDispose.forEach { scene ->
            safe("scene.dispose") { scene.disposeRuntime() }
        }
    }

    fun update(deltaSeconds: Float) {
        if (closed) return
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
        if (closed) return
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
        onPointer(
            PointerEvent(
                pointerId = 0,
                type = PointerEventType.MOVE,
                x = x,
                y = y,
                viewportWidth = width,
                viewportHeight = height
            )
        )
    }

    fun onPointer(event: PointerEvent) {
        if (closed) return
        applyPendingTransitions()
        val activeScene = current ?: return
        activeScene.input.onPointer(event)
        safe("scene.input.pointer") {
            activeScene.dispatchPointer(event)
        }
        applyPendingTransitions()
    }

    fun resetInput() {
        if (closed) return
        heldKeys.clear()
        current?.input?.reset()
        current?.ui?.resetInput()
        safe("scene.input.reset") {
            current?.onFocusLost()
        }
    }

    fun onAxis(axisId: String, value: Float) {
        if (closed) return
        applyPendingTransitions()
        val activeScene = current ?: return
        activeScene.input.onAxis(axisId, value)
        safe("scene.input.axis") {
            activeScene.onAxis(axisId, value)
        }
        applyPendingTransitions()
    }

    fun onKey(
        keyCode: Int,
        pressed: Boolean
    ) {
        if (closed) return
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
        if (closed) return
        var applied = 0
        while (true) {
            val nextScene = pendingScenes.poll() ?: return
            val previousScene = current
            if (previousScene === nextScene) {
                continue
            }

            if (previousScene != null) {
                previousScene.ui.resetInput()
                safe("scene.onExit") {
                    previousScene.onExit()
                }
            }

            current = nextScene
            knownScenes.add(nextScene)
            nextScene.attachRuntime(diagnostics, gameState, assets, audio, ::changeScene)
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
        heldKeys.forEach(scene.input::syncHeldKey)
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
