package com.rift.engine.entity

import com.rift.engine.graphics.RenderContext
import com.rift.engine.movement.MovementBody
import com.rift.engine.movement.MovementInput
import com.rift.engine.movement.MovementProfile
import com.rift.engine.physics.Aabb

/**
 * Basic composable entity used by the scene API.
 *
 * Position and dimensions are measured in logical world pixels. Optional
 * behavior is added through GameComponent rather than genre-specific subclasses.
 */
open class GameObject(
    x: Float,
    y: Float,
    var width: Float,
    var height: Float,
    var color: Int = 0xFFFFFFFF.toInt()
) {
    val movementBody = MovementBody(x, y)

    private val components = mutableListOf<GameComponent>()
    private val pendingComponentAdds = mutableListOf<GameComponent>()
    private val pendingComponentRemovals = mutableListOf<GameComponent>()
    private var traversingComponents = false

    var x: Float
        get() = movementBody.x
        set(value) {
            movementBody.x = value
        }

    var y: Float
        get() = movementBody.y
        set(value) {
            movementBody.y = value
        }

    var velocityX: Float
        get() = movementBody.velocityX
        set(value) {
            movementBody.velocityX = value
        }

    var velocityY: Float
        get() = movementBody.velocityY
        set(value) {
            movementBody.velocityY = value
        }

    var isGrounded: Boolean
        get() = movementBody.isGrounded
        set(value) {
            movementBody.isGrounded = value
        }

    var movementProfile: MovementProfile? = null
    var movementInput: MovementInput = MovementInput()
    var active: Boolean = true

    /**
     * Adds a component by object identity. The same instance cannot be attached
     * twice, but multiple components of the same class are allowed.
     *
     * If called during component update/render, attachment is deferred until
     * the current traversal completes.
     */
    fun addComponent(component: GameComponent): Boolean {
        if (containsIdentity(components, component) ||
            containsIdentity(pendingComponentAdds, component)
        ) {
            return false
        }

        if (traversingComponents) {
            pendingComponentRemovals.removeAll { it === component }
            pendingComponentAdds.add(component)
        } else {
            components.add(component)
            component.onAttach(this)
        }
        return true
    }

    /**
     * Removes a component by object identity. Detachment is deferred during
     * component traversal.
     */
    fun removeComponent(component: GameComponent): Boolean {
        val pendingAddRemoved = pendingComponentAdds.removeAll { it === component }
        if (traversingComponents) {
            if (!containsIdentity(components, component)) {
                return pendingAddRemoved
            }
            if (!containsIdentity(pendingComponentRemovals, component)) {
                pendingComponentRemovals.add(component)
            }
            return true
        }

        val removed = components.removeAll { it === component }
        if (removed) {
            component.onDetach(this)
        }
        return removed || pendingAddRemoved
    }

    fun components(): List<GameComponent> = components.toList()

    inline fun <reified T : GameComponent> component(): T? {
        return components().firstOrNull { it is T } as? T
    }

    open fun update(deltaSeconds: Float) {
        if (!active) {
            return
        }

        movementProfile?.update(
            movementBody,
            movementInput,
            deltaSeconds
        )

        x += velocityX * deltaSeconds
        y += velocityY * deltaSeconds

        traverseComponents { component ->
            component.update(this, deltaSeconds)
        }
    }

    open fun render(context: RenderContext) {
        if (!active) {
            return
        }

        context.drawRoundRect(
            x,
            y,
            x + width,
            y + height,
            12f,
            color
        )

        traverseComponents { component ->
            component.render(this, context)
        }
    }

    fun bounds(): Aabb {
        return Aabb(
            left = x,
            top = y,
            right = x + width,
            bottom = y + height
        )
    }

    private inline fun traverseComponents(action: (GameComponent) -> Unit) {
        applyPendingComponentChanges()
        traversingComponents = true
        try {
            var index = 0
            while (index < components.size) {
                action(components[index])
                index += 1
            }
        } finally {
            traversingComponents = false
            applyPendingComponentChanges()
        }
    }

    private fun applyPendingComponentChanges() {
        if (pendingComponentRemovals.isNotEmpty()) {
            val removals = pendingComponentRemovals.toList()
            pendingComponentRemovals.clear()
            removals.forEach { component ->
                val removed = components.removeAll { it === component }
                if (removed) {
                    component.onDetach(this)
                }
            }
        }

        if (pendingComponentAdds.isNotEmpty()) {
            val additions = pendingComponentAdds.toList()
            pendingComponentAdds.clear()
            additions.forEach { component ->
                if (!containsIdentity(components, component)) {
                    components.add(component)
                    component.onAttach(this)
                }
            }
        }
    }

    private fun containsIdentity(
        collection: List<GameComponent>,
        target: GameComponent
    ): Boolean = collection.any { it === target }
}
