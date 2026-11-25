package com.rift.engine.demo

import com.rift.engine.graphics3d.Vector3
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrologueCombatRulesTest {
    @Test
    fun acceptsTargetsInFrontAndForgivingDiagonalArc() {
        assertTrue(PrologueCombatRules.isTargetInMeleeArc(
            Vector3(0f,0f,-1f),Vector3(.8f,0f,-1f)
        ))
        assertTrue(PrologueCombatRules.isTargetInMeleeArc(
            Vector3(1f,0f,0f),Vector3(1f,0f,.7f)
        ))
    }

    @Test
    fun rejectsTargetsBehindOrBesideThePlayer() {
        assertFalse(PrologueCombatRules.isTargetInMeleeArc(
            Vector3(0f,0f,-1f),Vector3(0f,0f,1f)
        ))
        assertFalse(PrologueCombatRules.isTargetInMeleeArc(
            Vector3(0f,0f,-1f),Vector3(1f,0f,0f)
        ))
    }

    @Test
    fun rejectsZeroLengthDirectionsAndInvalidTolerance() {
        assertFalse(PrologueCombatRules.isTargetInMeleeArc(Vector3.ZERO,Vector3.FORWARD))
        assertFalse(PrologueCombatRules.isTargetInMeleeArc(Vector3.FORWARD,Vector3.ZERO))
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            PrologueCombatRules.isTargetInMeleeArc(Vector3.FORWARD,Vector3.FORWARD,1.1f)
        }
    }
}
