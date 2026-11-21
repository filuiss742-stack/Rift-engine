package com.rift.engine.entity

import com.rift.engine.diagnostics.DiagnosticReporter
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
    width: Float,
    height: Float,
    var color: Int = 0xFFFFFFFF.toInt()
) {
    val movementBody = MovementBody(x, y)
    val transform = Transform2D()

    var width: Float = width
        set(value) {
            require(value.isFinite() && value >= 0f) {
                "width must be finite and non-negative."
            }
            field = value
        }

    var height: Float = height
        set(value) {
            require(value.isFinite() && value >= 0f) {
                "height must be finite and non-negative."
            }
            field = value
        }

    internal var diagnostics: DiagnosticReporter? = null
    private val failedComponents = java.util.Collections.newSetFromMap(
        java.util.IdentityHashMap<GameComponent, Boolean>()
    )

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

    var rotationDegrees: Float
        get() = transform.rotationDegrees
        set(value) {
            transform.rotationDegrees = value
        }

    var scaleX: Float
        get() = transform.scaleX
        set(value) {
            transform.scaleX = value
        }

    var scaleY: Float
        get() = transform.scaleY
        set(value) {
            transform.scaleY = value
        }

    var movementProfile: MovementProfile? = null
    var movementInput: MovementInput = MovementInput()
    var active: Boolean = true
    var renderDefaultVisual: Boolean = true
    internal var physicsControlled: Boolean = false

    init {
        require(width.isFinite() && width >= 0f) {
            "width must be finite and non-negative."
        }
        require(height.isFinite() && height >= 0f) {
            "height must be finite and non-negative."
        }
    }

    /**
     * Adds a component by object identity. The same instance cannot be attached
     * twice, but multiple components of the same class are allowed.
     *
     * If called during component update/render, attachment is deferred until
     * the current traversal completes.
     */
    fun addComponent(component: GameComponent): Boolean {
        if (traversingComponents) {
            if (containsIdentity(components, component)) {
                return pendingComponentRemovals.removeAll { it === component }
            }
            if (containsIdentity(pendingComponentAdds, component)) {
                return false
            }
            pendingComponentAdds.add(component)
            return true
        }

        if (containsIdentity(components, component)) {
            return false
        }
        return try {
            attachComponent(component)
            true
        } catch (exception: Exception) {
            reportComponentFailure("component.attach", exception)
            false
        }
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
            failedComponents.remove(component)
            try {
                component.onDetach(this)
            } catch (exception: Exception) {
                reportComponentFailure("component.detach", exception)
            }
        }
        return removed || pendingAddRemoved
    }

    fun components(): List<GameComponent> = components.toList()

    inline fun <reified T : GameComponent> component(): T? {
        return components().firstOrNull { it is T } as? T
    }

    open fun update(deltaSeconds: Float) {
        require(deltaSeconds.isFinite() && deltaSeconds >= 0f) {
            "deltaSeconds must be finite and non-negative."
        }
        if (!active) {
            return
        }

        movementProfile?.update(
            movementBody,
            movementInput,
            deltaSeconds
        )

        if (!physicsControlled) {
            x += velocityX * deltaSeconds
            y += velocityY * deltaSeconds
        }

        traverseComponents { component ->
            component.update(this, deltaSeconds)
        }
    }

    open fun render(context: RenderContext) {
        if (!active) {
            return
        }

        context.pushState()
        try {
            context.translate(x, y)
            context.rotate(rotationDegrees)
            context.scale(scaleX, scaleY)
            if (renderDefaultVisual) {
                context.drawRoundRect(
                    0f,
                    0f,
                    width,
                    height,
                    12f,
                    color
                )
            }

            traverseComponents { component ->
                component.render(this, context)
            }
        } finally {
            context.popState()
        }
    }

    fun bounds(): Aabb {
        return transform.transformedBounds(x, y, width, height)
    }

    private inline fun traverseComponents(action: (GameComponent) -> Unit) {
        applyPendingComponentChanges()
        traversingComponents = true
        try {
            var index = 0
            while (index < components.size) {
                val component = components[index]
                if (component !in failedComponents) {
                    try {
                        action(component)
                    } catch (exception: Exception) {
                        failedComponents.add(component)
                        reportComponentFailure("component.update-render", exception)
                    }
                }
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
                failedComponents.remove(component)
                if (removed) {
                    try {
                        component.onDetach(this)
                    } catch (exception: Exception) {
                        reportComponentFailure("component.detach", exception)
                    }
                }
            }
        }

        if (pendingComponentAdds.isNotEmpty()) {
            val additions = pendingComponentAdds.toList()
            pendingComponentAdds.clear()
            additions.forEach { component ->
                if (!containsIdentity(components, component)) {
                    try {
                        attachComponent(component)
                    } catch (exception: Exception) {
                        reportComponentFailure("component.attach", exception)
                    }
                }
            }
        }
    }

    private fun attachComponent(component: GameComponent) {
        failedComponents.remove(component)
        components.add(component)
        try {
            component.onAttach(this)
        } catch (exception: Exception) {
            components.removeAll { it === component }
            try {
                component.onDetach(this)
            } catch (cleanupException: Exception) {
                exception.addSuppressed(cleanupException)
            }
            throw exception
        }
    }

    private fun reportComponentFailure(subsystem: String, exception: Exception) {
        val reporter = diagnostics
        if (reporter == null) throw exception
        reporter.reportException(subsystem, exception)
    }

    private fun containsIdentity(
        collection: List<GameComponent>,
        target: GameComponent
    ): Boolean = collection.any { it === target }
}
