package com.rift.engine.scene

import com.rift.engine.assets.AssetManager
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
    val ui = UiCanvas()

    var gameState: GameState = GameState()
        internal set
    var assets: AssetManager? = null
        internal set
    var audio: AudioManager? = null
        internal set
    internal var diagnostics: DiagnosticReporter? = null

    protected val objects = mutableListOf<GameObject>()
    private val pendingAdds = mutableListOf<GameObject>()
    private val pendingRemovals = mutableSetOf<GameObject>()
    private val quarantinedObjects = java.util.Collections.newSetFromMap(
        java.util.IdentityHashMap<GameObject, Boolean>()
    )
    private var traversingObjects = false

    protected fun add(gameObject: GameObject) {
        if (objects.any { it === gameObject } || pendingAdds.any { it === gameObject }) return
        if (pendingRemovals.remove(gameObject)) return
        quarantinedObjects.remove(gameObject)
        gameObject.diagnostics = diagnostics
        if (traversingObjects) pendingAdds.add(gameObject) else objects.add(gameObject)
    }

    protected fun remove(gameObject: GameObject) {
        if (pendingAdds.removeAll { it === gameObject }) {
            physics.removeBody(gameObject)
            quarantinedObjects.remove(gameObject)
            gameObject.diagnostics = null
            return
        }
        if (traversingObjects) {
            if (objects.any { it === gameObject }) pendingRemovals.add(gameObject)
        } else if (objects.removeAll { it === gameObject }) {
            physics.removeBody(gameObject)
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
        audioManager: AudioManager
    ) {
        diagnostics = reporter
        gameState = sharedState
        assets = assetManager
        audio = audioManager
        objects.forEach { it.diagnostics = reporter }
        pendingAdds.forEach { it.diagnostics = reporter }
    }

    open fun onEnter() = Unit
    open fun onExit() = Unit

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
            traversingObjects = true
            try {
                var index = 0
                while (index < objects.size) {
                    val gameObject = objects[index]
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
