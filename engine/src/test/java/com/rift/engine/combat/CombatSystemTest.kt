package com.rift.engine.combat

import com.rift.engine.entity.GameObject
import com.rift.engine.scene.Scene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CombatSystemTest {
    @Test
    fun healthClampsDamageAndHealingAndFiresDeathOnceUntilReset() {
        var deaths = 0
        val health = HealthComponent(100f, onDeath = { deaths += 1 })

        assertEquals(30f, health.applyDamage(30f), 0.001f)
        assertEquals(20f, health.heal(20f), 0.001f)
        assertEquals(90f, health.applyDamage(90f), 0.001f)
        assertFalse(health.isAlive)
        assertEquals(1, deaths)
        assertEquals(0f, health.applyDamage(5f), 0.001f)
        assertEquals(0f, health.heal(10f), 0.001f)

        health.reset()
        assertTrue(health.isAlive)
        health.applyDamage(100f)
        assertEquals(2, deaths)
    }

    @Test
    fun hitboxDamagesEnemyOnceAndIgnoresFriendlyTargets() {
        val combat = CombatWorld()
        val attacker = GameObject(0f, 0f, 20f, 20f)
        val enemy = GameObject(10f, 0f, 20f, 20f)
        val ally = GameObject(5f, 0f, 20f, 20f)
        val enemyHealth = HealthComponent(100f)
        val allyHealth = HealthComponent(100f)

        combat.addHurtbox(enemy, enemyHealth, "enemy")
        combat.addHurtbox(ally, allyHealth, "player")
        combat.addHitbox(attacker, damage = 25f, teamId = "player", targetTeams = setOf("enemy"))

        combat.step(1f / 60f)
        combat.step(1f / 60f)

        assertEquals(75f, enemyHealth.currentHealth, 0.001f)
        assertEquals(100f, allyHealth.currentHealth, 0.001f)
    }

    @Test
    fun timedHitboxExpiresAndSceneRemovalClearsCombatOwnership() {
        val combat = CombatWorld()
        val owner = GameObject(0f, 0f, 10f, 10f)
        val hitbox = combat.addHitbox(owner, 10f, "player", durationSeconds = 0.05f)
        combat.step(0.1f)
        assertFalse(combat.hitboxes().contains(hitbox))

        val scene = object : Scene() {
            fun addObject(value: GameObject) = add(value)
            fun removeObject(value: GameObject) = remove(value)
        }
        val target = GameObject(0f, 0f, 10f, 10f)
        val health = HealthComponent(10f)
        scene.addObject(target)
        scene.combat.addHurtbox(target, health, "enemy")
        scene.removeObject(target)

        assertTrue(scene.combat.hurtboxes().isEmpty())
    }
}
