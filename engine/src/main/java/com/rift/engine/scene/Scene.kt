package com.rift.engine.scene

import com.rift.engine.assets.AssetManager
import com.rift.engine.combat.CombatWorld
import com.rift.engine.audio.AudioManager
import com.rift.engine.diagnostics.DiagnosticReporter
import com.rift.engine.entity.GameObject
import com.rift.engine.graphics.Camera
import com.rift.engine.graphics.RenderContext
import com.rift.engine.input.InputActionMap
import com.rift.engine.input.PointerEvent
import com.rift.engine.input.PointerEventType
import com.rift.engine.physics.PhysicsWorld
import com.rift.engine.state.GameState
import com.rift.engine.ui.UiCanvas

/**
 * A scene owns its game objects, physics world, camera, input map, and UI.
 * Mutations requested during traversal are deferred to a safe boundary.
 */
abstract class Scene {
    val input = InputActionMap()
    val camera = Camera()
    val physics = PhysicsWorld()
    val combat = CombatWorld()
    val ui = UiCanvas()

    var gameState: GameState = GameState()
        internal set
    var assets: AssetManager? = null
        internal set
    var audio: AudioManager? = null
        internal set
    internal var diagnostics: DiagnosticReporter? = null
    private var sceneTransitionRequester: ((Scene) -> Unit)? = null

    protected val objects = mutableListOf<GameObject>()
    private val pendingAdds = mutableListOf<GameObject>()
    private val pendingRemovals = mutableSetOf<GameObject>()
    private val quarantinedObjects = java.util.Collections.newSetFromMap(
        java.util.IdentityHashMap<GameObject, Boolean>()
    )
    private var traversingObjects = false
    private var disposed = false

    protected fun add(gameObject: GameObject) {
        check(!disposed) { "Cannot add objects to a disposed Scene." }
        if (objects.any { it === gameObject } || pendingAdds.any { it === gameObject }) return
        if (pendingRemovals.remove(gameObject)) return
        quarantinedObjects.remove(gameObject)
        gameObject.diagnostics = diagnostics
        if (traversingObjects) pendingAdds.add(gameObject) else objects.add(gameObject)
    }

    protected fun remove(gameObject: GameObject) {
        if (disposed) return
        if (pendingAdds.removeAll { it === gameObject }) {
            physics.removeBody(gameObject)
            combat.removeOwner(gameObject)
            quarantinedObjects.remove(gameObject)
            gameObject.diagnostics = null
            return
        }
        if (traversingObjects) {
            if (objects.any { it === gameObject }) pendingRemovals.add(gameObject)
        } else if (objects.removeAll { it === gameObject }) {
            physics.removeBody(gameObject)
            combat.removeOwner(gameObject)
            quarantinedObjects.remove(gameObject)
            gameObject.diagnostics = null
        }
    }

    /**
     * Retries an object previously quarantined after an update or render exception.
     * Returns false when the object is not currently owned by this scene.
     */
    fun retryObject(gameObject: GameObject): Boolean {
        if (objects.none { it === gameObject }) return false
        return quarantinedObjects.remove(gameObject)
    }

    fun isObjectQuarantined(gameObject: GameObject): Boolean =
        gameObject in quarantinedObjects

    internal fun attachRuntime(
        reporter: DiagnosticReporter,
        sharedState: GameState,
        assetManager: AssetManager,
        audioManager: AudioManager,
        transitionRequester: (Scene) -> Unit
    ) {
        check(!disposed) { "Cannot attach a disposed Scene to a runtime." }
        diagnostics = reporter
        ui.diagnostics = reporter
        combat.diagnostics = reporter
        gameState = sharedState
        assets = assetManager
        audio = audioManager
        sceneTransitionRequester = transitionRequester
        objects.forEach { it.diagnostics = reporter }
        pendingAdds.forEach { it.diagnostics = reporter }
    }

    /**
     * Requests a safe transition after the current scene callback completes.
     * Shared GameState, assets, and audio services are retained by the manager.
     */
    fun requestSceneChange(nextScene: Scene) {
        check(!disposed) { "Cannot request transitions from a disposed Scene." }
        val requester = sceneTransitionRequester
        if (requester == null) {
            diagnostics?.report(
                com.rift.engine.diagnostics.DiagnosticSeverity.WARNING,
                "SCENE_TRANSITION_RUNTIME_UNAVAILABLE",
                "Scene",
                "Scene changes can only be requested after the scene is attached to a SceneManager."
            )
            return
        }
        requester(nextScene)
    }

    open fun onEnter() = Unit
    open fun onExit() = Unit
    open fun onDispose() = Unit

    internal fun disposeRuntime() {
        if (disposed) return
        disposed = true
        val reporter = diagnostics
        try {
            onDispose()
        } catch (exception: Exception) {
            reporter?.reportException(
                "scene.dispose",
                exception,
                "Inspect the Scene.onDispose() cleanup and release only resources owned by this scene."
            )
        }

        ui.clear()
        val ownedObjects = java.util.Collections.newSetFromMap(
            java.util.IdentityHashMap<GameObject, Boolean>()
        )
        ownedObjects.addAll(objects)
        ownedObjects.addAll(pendingAdds)
        objects.clear()
        pendingAdds.clear()
        pendingRemovals.clear()
        quarantinedObjects.clear()
        physics.clear()
        combat.clear()
        ownedObjects.forEach { gameObject ->
            gameObject.disposeComponents(reporter)
            gameObject.diagnostics = null
        }
        sceneTransitionRequester = null
        assets = null
        audio = null
        diagnostics = null
    }

    open fun update(deltaSeconds: Float) {
        require(deltaSeconds.isFinite() && deltaSeconds >= 0f) {
            "deltaSeconds must be finite and non-negative."
        }
        applyPendingMutations()
        traversingObjects = true

        try {
            var index = 0
            while (index < objects.size) {
                val gameObject = objects[index]
                if (gameObject !in quarantinedObjects) {
                    try {
                        gameObject.update(deltaSeconds)
                    } catch (exception: Exception) {
                        quarantinedObjects.add(gameObject)
                        reportSceneFailure("scene.object.update", exception)
                    }
                }
                index += 1
            }
        } finally {
            traversingObjects = false
            applyPendingMutations()
        }

        try {
            physics.step(deltaSeconds)
        } catch (exception: Exception) {
            reportSceneFailure("scene.physics.update", exception)
        }
        try {
            combat.step(deltaSeconds)
        } catch (exception: Exception) {
            reportSceneFailure("scene.combat.update", exception)
        }
        try {
            ui.update(deltaSeconds)
        } catch (exception: Exception) {
            reportSceneFailure("scene.ui.update", exception)
        }
    }

    internal fun dispatchPointer(event: PointerEvent) {
        if (!ui.dispatchPointer(event)) onPointer(event)
    }

    open fun render(context: RenderContext) {
        camera.withTransform(context) {
            applyPendingMutations()
            val renderOrder = objects.sortedBy { it.renderLayer }
            traversingObjects = true
            try {
                var index = 0
                while (index < renderOrder.size) {
                    val gameObject = renderOrder[index]
                    if (gameObject !in quarantinedObjects) {
                        try {
                            gameObject.render(context)
                        } catch (exception: Exception) {
                            quarantinedObjects.add(gameObject)
                            reportSceneFailure("scene.object.render", exception)
                        }
                    }
                    index += 1
                }
            } finally {
                traversingObjects = false
                applyPendingMutations()
            }
        }

        try {
            ui.render(context)
        } catch (exception: Exception) {
            reportSceneFailure("scene.ui.render", exception)
        }
    }

    open fun onTouch(x: Float, y: Float, width: Float, height: Float) = Unit

    open fun onPointer(event: PointerEvent) {
        if (event.type != PointerEventType.CANCEL) {
            onTouch(event.x, event.y, event.viewportWidth, event.viewportHeight)
        }
    }

    open fun onFocusLost() = Unit
    open fun onKey(keyCode: Int, pressed: Boolean) = Unit
    open fun onAxis(axisId: String, value: Float) = Unit

    private fun reportSceneFailure(subsystem: String, exception: Exception) {
        val reporter = diagnostics
        if (reporter == null) throw exception
        reporter.reportException(subsystem, exception)
    }

    private fun applyPendingMutations() {
        if (pendingRemovals.isNotEmpty()) {
            val removals = pendingRemovals.toList()
            pendingRemovals.clear()
            removals.forEach { gameObject ->
                objects.removeAll { it === gameObject }
                physics.removeBody(gameObject)
                combat.removeOwner(gameObject)
                quarantinedObjects.remove(gameObject)
                gameObject.diagnostics = null
            }
        }

        if (pendingAdds.isNotEmpty()) {
            val additions = pendingAdds.toList()
            pendingAdds.clear()
            additions.forEach { gameObject ->
                if (objects.none { it === gameObject }) {
                    quarantinedObjects.remove(gameObject)
                    gameObject.diagnostics = diagnostics
                    objects.add(gameObject)
                }
            }
        }
    }
}
