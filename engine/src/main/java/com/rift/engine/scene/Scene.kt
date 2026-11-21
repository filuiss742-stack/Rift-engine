package com.rift.engine.scene

import com.rift.engine.entity.GameObject
import com.rift.engine.graphics.Camera
import com.rift.engine.graphics.RenderContext
import com.rift.engine.input.InputActionMap
import com.rift.engine.input.PointerEvent
import com.rift.engine.input.PointerEventType
import com.rift.engine.physics.PhysicsWorld

/**
 * A scene owns its game objects.
 *
 * Add/remove requests made while objects are being updated or rendered are
 * deferred until the current traversal finishes. This keeps iteration stable
 * without allocating a copied object list every frame.
 */
abstract class Scene {
    /** Named actions bound by this scene; physical key events are forwarded by SceneManager. */
    val input = InputActionMap()
    val camera = Camera()
    val physics = PhysicsWorld()

    protected val objects = mutableListOf<GameObject>()

    private val pendingAdds = mutableListOf<GameObject>()
    private val pendingRemovals = mutableSetOf<GameObject>()
    private var traversingObjects = false

    protected fun add(gameObject: GameObject) {
        if (traversingObjects) {
            pendingAdds.add(gameObject)
        } else {
            objects.add(gameObject)
        }
    }

    protected fun remove(gameObject: GameObject) {
        if (traversingObjects) {
            pendingAdds.remove(gameObject)
            pendingRemovals.add(gameObject)
        } else {
            objects.remove(gameObject)
            physics.removeBody(gameObject)
        }
    }

    open fun onEnter() = Unit

    open fun onExit() = Unit

    open fun update(deltaSeconds: Float) {
        applyPendingMutations()
        traversingObjects = true

        try {
            var index = 0
            while (index < objects.size) {
                objects[index].update(deltaSeconds)
                index += 1
            }
        } finally {
            traversingObjects = false
            applyPendingMutations()
        }

        physics.step(deltaSeconds)
    }

    open fun render(context: RenderContext) {
        camera.withTransform(context) {
            applyPendingMutations()
            traversingObjects = true

            try {
                var index = 0
                while (index < objects.size) {
                    objects[index].render(context)
                    index += 1
                }
            } finally {
                traversingObjects = false
                applyPendingMutations()
            }
        }
    }

    open fun onTouch(
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) = Unit

    open fun onPointer(event: PointerEvent) {
        if (event.type != PointerEventType.CANCEL) {
            onTouch(
                event.x,
                event.y,
                event.viewportWidth,
                event.viewportHeight
            )
        }
    }

    open fun onFocusLost() = Unit

    open fun onKey(
        keyCode: Int,
        pressed: Boolean
    ) = Unit

    private fun applyPendingMutations() {
        if (pendingRemovals.isNotEmpty()) {
            pendingRemovals.forEach(physics::removeBody)
            objects.removeAll(pendingRemovals)
            pendingRemovals.clear()
        }

        if (pendingAdds.isNotEmpty()) {
            objects.addAll(pendingAdds)
            pendingAdds.clear()
        }
    }
}
