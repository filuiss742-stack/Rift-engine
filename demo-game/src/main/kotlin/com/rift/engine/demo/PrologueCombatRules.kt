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
}
