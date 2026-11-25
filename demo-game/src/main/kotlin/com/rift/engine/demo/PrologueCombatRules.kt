package com.rift.engine.demo

import com.rift.engine.graphics3d.Vector3

/** Axis-aligned obstacle in the horizontal (X/Z) plane. */
internal data class HorizontalObstacle(
    val centerX: Float,
    val centerZ: Float,
    val halfWidth: Float,
    val halfDepth: Float
)

/** Small deterministic rules shared by the prologue combat loop and its tests. */
internal object PrologueCombatRules {
    /**
     * Melee is directional: the target must be inside the forward half-plane,
     * with a small tolerance so diagonal movement remains forgiving.
     */
    fun isTargetInMeleeArc(
        facing: Vector3,
        targetDelta: Vector3,
        minimumForwardDot: Float = .15f
    ): Boolean {
        require(minimumForwardDot.isFinite() && minimumForwardDot in -1f..1f)
        val horizontalFacing = Vector3(facing.x,0f,facing.z)
        val horizontalTarget = Vector3(targetDelta.x,0f,targetDelta.z)
        if (horizontalFacing.lengthSquared() <= 1e-6f ||
            horizontalTarget.lengthSquared() <= 1e-6f) return false
        return horizontalFacing.normalized().dot(horizontalTarget.normalized()) >= minimumForwardDot
    }
    
    /**
     * The heroine's canon health floor is one: an otherwise lethal hit starts
     * the defeat/recovery beat instead of allowing HP to reach zero.
     */
    fun wouldTriggerDefeat(currentHealth: Int, incomingDamage: Int = 1): Boolean {
        require(currentHealth >= 1) { "Current health must be at least one." }
        require(incomingDamage >= 0) { "Incoming damage cannot be negative." }
        return incomingDamage >= currentHealth
    }

    fun applyDamage(currentHealth: Int, incomingDamage: Int = 1): Int {
        require(currentHealth >= 1) { "Current health must be at least one." }
        require(incomingDamage >= 0) { "Incoming damage cannot be negative." }
        return (currentHealth - incomingDamage).coerceAtLeast(1)
    }

    /** Circle-vs-AABB movement test for the ruin's ground-plane collision. */
    fun isWalkableInRuins(
        x: Float,
        z: Float,
        boundX: Float,
        boundZ: Float,
        radius: Float,
        obstacles: List<HorizontalObstacle>
    ): Boolean {
        require(x.isFinite() && z.isFinite())
        require(boundX.isFinite() && boundZ.isFinite() && boundX > 0f && boundZ > 0f)
        require(radius.isFinite() && radius > 0f)
        if (kotlin.math.abs(x) > boundX - radius || kotlin.math.abs(z) > boundZ - radius) return false
        return obstacles.none { obstacle ->
            require(
                obstacle.centerX.isFinite() && obstacle.centerZ.isFinite() &&
                    obstacle.halfWidth.isFinite() && obstacle.halfDepth.isFinite() &&
                    obstacle.halfWidth > 0f && obstacle.halfDepth > 0f
            ) { "Obstacle dimensions and position must be finite and positive." }
            val nearestX = x.coerceIn(
                obstacle.centerX - obstacle.halfWidth,
                obstacle.centerX + obstacle.halfWidth
            )
            val nearestZ = z.coerceIn(
                obstacle.centerZ - obstacle.halfDepth,
                obstacle.centerZ + obstacle.halfDepth
            )
            val dx = x - nearestX
            val dz = z - nearestZ
            dx * dx + dz * dz < radius * radius
        }
    }
}
