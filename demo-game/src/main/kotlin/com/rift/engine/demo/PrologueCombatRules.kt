package com.rift.engine.demo

import com.rift.engine.graphics3d.Vector3

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
}
