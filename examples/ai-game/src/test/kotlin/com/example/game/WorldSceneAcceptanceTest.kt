package com.example.game

import com.rift.engine.input.EngineKey
import com.rift.engine.input.PointerButtons
import com.rift.engine.input.PointerEvent
import com.rift.engine.input.PointerEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldSceneAcceptanceTest {
    private val frame = 1f / 60f

    @Test
    fun keyboardMovementAndEnemyChaseWorkInTheSharedSceneLoop() {
        val scene = WorldScene()
        val playerStart = scene.player.x
        val enemyStart = scene.enemy.x

        scene.input.onKey(EngineKey.D, true)
        scene.update(frame)
        scene.input.endFrame()
        scene.input.onKey(EngineKey.D, false)

        assertTrue(scene.player.x > playerStart)
        assertTrue(scene.enemy.x < enemyStart)
    }

    @Test
    fun attackDefeatsThirtyHealthEnemyAndCoinCanBeCollected() {
        val scene = WorldScene()
        scene.enemy.x = scene.player.x + 10f
        scene.enemy.y = scene.player.y

        repeat(3) {
            scene.input.onKey(EngineKey.SPACE, true)
            scene.update(frame)
            scene.input.endFrame()
            scene.input.onKey(EngineKey.SPACE, false)
            scene.update(0.3f)
            scene.input.endFrame()
        }

        assertTrue(scene.enemyDefeated)
        assertEquals(0f, scene.enemyHealth.currentHealth, 0.001f)
        val droppedCoin = scene.coin
        assertNotNull(droppedCoin)
        assertEquals(0, scene.coins)

        scene.player.x = droppedCoin!!.x
        scene.player.y = droppedCoin.y
        scene.update(frame)

        assertEquals(1, scene.coins)
        assertNull(scene.coin)
    }

    @Test
    fun joystickMovementAndAttackButtonWorkAtTheSameTimeOnSeparatePointers() {
        val scene = WorldScene()
        scene.enemy.x = scene.player.x + 10f
        scene.enemy.y = scene.player.y
        val playerStart = scene.player.x

        scene.ui.dispatchPointer(pointer(1, PointerEventType.DOWN, 80f, 516f))
        scene.ui.dispatchPointer(pointer(1, PointerEventType.MOVE, 136f, 516f))
        scene.ui.dispatchPointer(
            pointer(
                2,
                PointerEventType.DOWN,
                scene.attackButton.x + scene.attackButton.width / 2f,
                scene.attackButton.y + scene.attackButton.height / 2f
            )
        )

        scene.update(frame)

        assertTrue("The joystick should keep moving while another pointer presses attack.", scene.player.x > playerStart)
        assertEquals(20f, scene.enemyHealth.currentHealth, 0.001f)

        scene.ui.dispatchPointer(pointer(2, PointerEventType.UP, scene.attackButton.x + 50f, scene.attackButton.y + 50f))
        scene.ui.dispatchPointer(pointer(1, PointerEventType.UP, 136f, 516f))
    }

    @Test
    fun keyboardDodgeUsesShortSpeedBurst() {
        val scene = WorldScene()
        scene.input.onKey(EngineKey.D, true)
        scene.update(frame)
        scene.input.endFrame()

        val beforeDodge = scene.player.x
        scene.input.onKey(EngineKey.SHIFT, true)
        scene.update(frame)

        assertTrue(scene.player.x - beforeDodge > 5f)
    }

    @Test
    fun virtualJoystickMovesPlayerAndTouchButtonAttacks() {
        val scene = WorldScene()
        val startX = scene.player.x

        scene.ui.dispatchPointer(pointer(1, PointerEventType.DOWN, 80f, 516f))
        scene.ui.dispatchPointer(pointer(1, PointerEventType.MOVE, 136f, 516f))
        scene.update(frame)
        assertTrue(scene.player.x > startX)
        scene.ui.dispatchPointer(pointer(1, PointerEventType.UP, 136f, 516f))

        scene.enemy.x = scene.player.x + 10f
        scene.enemy.y = scene.player.y
        scene.ui.dispatchPointer(
            pointer(
                2,
                PointerEventType.DOWN,
                scene.attackButton.x + scene.attackButton.width / 2f,
                scene.attackButton.y + scene.attackButton.height / 2f
            )
        )
        scene.update(frame)

        assertEquals(20f, scene.enemyHealth.currentHealth, 0.001f)
    }

    private fun pointer(
        pointerId: Int,
        type: PointerEventType,
        x: Float,
        y: Float
    ) = PointerEvent(
        pointerId = pointerId,
        type = type,
        x = x,
        y = y,
        viewportWidth = 960f,
        viewportHeight = 600f,
        button = PointerButtons.TOUCH
    )
}
