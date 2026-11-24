package com.rift.engine.demo

import com.rift.engine.input.EngineKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CombatTrialAcceptanceTest {
    private val frame = 1f / 60f

    @Test fun telegraphedAreaStrikeCanBeDodged() {
        val scene = CombatTrialScene()
        scene.player.x = 220f; scene.player.y = 220f
        scene.enemy.x = 250f; scene.enemy.y = 220f
        scene.forceEnemyTelegraph(0.001f)
        scene.input.onKey(EngineKey.A, true); scene.input.onKey(EngineKey.SHIFT, true)
        scene.update(frame)
        assertEquals(100f, scene.playerHealth.currentHealth, 0.001f)
        assertTrue(scene.player.x < 220f)
    }

    @Test fun unavoidedTelegraphDamagesPlayer() {
        val scene = CombatTrialScene()
        scene.player.x = 220f; scene.player.y = 220f
        scene.enemy.x = 250f; scene.enemy.y = 220f
        scene.forceEnemyTelegraph(0.001f)
        scene.update(frame)
        assertEquals(75f, scene.playerHealth.currentHealth, 0.001f)
    }

    @Test fun meleeUsesCombatWorldAndSkillDealsBurstDamage() {
        val scene = CombatTrialScene()
        scene.player.x = 250f; scene.player.y = 240f
        scene.enemy.x = 250f; scene.enemy.y = 240f
        scene.input.onKey(EngineKey.SPACE, true); scene.update(frame)
        assertEquals(48f, scene.enemyHealth.currentHealth, 0.001f)
        scene.input.endFrame(); scene.input.onKey(EngineKey.Q, true); scene.update(frame)
        assertEquals(24f, scene.enemyHealth.currentHealth, 0.001f)
    }

    @Test fun playerDefeatStopsCombatAndRestartRestoresTheEncounter() {
        val scene = CombatTrialScene()
        scene.enemyHealth.applyDamage(24f)
        scene.playerHealth.applyDamage(100f)
        assertTrue(scene.playerDefeated)
        assertEquals(0f, scene.playerHealth.currentHealth, 0.001f)

        scene.input.onKey(EngineKey.R, true)
        scene.update(frame)

        assertFalse(scene.playerDefeated)
        assertEquals(100f, scene.playerHealth.currentHealth, 0.001f)
        assertEquals(60f, scene.enemyHealth.currentHealth, 0.001f)
        assertFalse(scene.enemyDefeated)
        assertTrue(scene.combat.hurtboxes().any { it.owner === scene.enemy })
    }

    @Test fun dialogueChoiceChangesRelationshipAndEnding() {
        val helped = CombatTrialScene()
        assertTrue(helped.chooseDialogue("help"))
        assertFalse("A completed conversation must not award relationship points twice.", helped.chooseDialogue("dismiss"))
        assertEquals(2, helped.gameState.int("story.relationship"))
        assertEquals("GOOD ENDING — TRUST", helped.endingForRelationship(2))
        val dismissed = CombatTrialScene()
        assertTrue(dismissed.chooseDialogue("dismiss"))
        assertEquals(-2, dismissed.gameState.int("story.relationship"))
        assertEquals("BITTER ENDING — DISTANCE", dismissed.endingForRelationship(-2))
    }

    @Test fun dungeonJumpUsesDifferentCameraFraming() {
        val scene = DungeonTrialScene()
        scene.player.isGrounded = true
        scene.input.onKey(EngineKey.SPACE, true)
        scene.update(frame)
        assertTrue(scene.player.velocityY < 0f)
        assertEquals(1.16f, scene.camera.zoom, 0.001f)
    }
}
