package com.rift.engine.demo

import com.rift.engine.entity.GameObject
import com.rift.engine.graphics.RenderContext
import com.rift.engine.input.EngineKey
import com.rift.engine.movement.MovementInput
import com.rift.engine.movement.TopDownMovementProfile
import com.rift.engine.physics.PhysicsBodyType
import com.rift.engine.scene.Scene
import com.rift.engine.ui.UiLabel
import com.rift.engine.ui.VirtualJoystick

/**
 * Top-down mode in the shared demo. Press TAB to switch to the side-view mode.
 */
class SandboxScene : Scene() {
    private val joystick = VirtualJoystick(24f, 440f, 112f)
    private val player = GameObject(
        x = 140f,
        y = 220f,
        width = 40f,
        height = 40f,
        color = 0xFF6996FF.toInt()
    ).apply {
        movementProfile = TopDownMovementProfile(speed = 240f)
        renderLayer = 10
    }

    init {
        input.bindKey("move.left", EngineKey.A)
        input.bindKey("move.left", EngineKey.LEFT)
        input.bindKey("move.right", EngineKey.D)
        input.bindKey("move.right", EngineKey.RIGHT)
        input.bindKey("move.up", EngineKey.W)
        input.bindKey("move.up", EngineKey.UP)
        input.bindKey("move.down", EngineKey.S)
        input.bindKey("move.down", EngineKey.DOWN)
        input.bindKey("switch.mode", EngineKey.TAB)

        physics.gravityX = 0f
        physics.gravityY = 0f
        physics.addBody(player, PhysicsBodyType.DYNAMIC, gravityScale = 0f)
        add(player)

        add(GameObject(0f, 0f, 960f, 24f, 0xFF36465E.toInt()).apply { renderLayer = -10 })
        add(GameObject(0f, 576f, 960f, 24f, 0xFF36465E.toInt()).apply { renderLayer = -10 })
        add(GameObject(0f, 24f, 24f, 552f, 0xFF36465E.toInt()).apply { renderLayer = -10 })
        add(GameObject(936f, 24f, 24f, 552f, 0xFF36465E.toInt()).apply { renderLayer = -10 })

        val obstacle = GameObject(390f, 170f, 120f, 90f, 0xFFFFAA50.toInt())
        obstacle.renderLayer = 0
        physics.addBody(obstacle, PhysicsBodyType.STATIC)
        add(obstacle)

        val secondObstacle = GameObject(650f, 350f, 80f, 120f, 0xFF78DCAA.toInt())
        secondObstacle.renderLayer = 0
        physics.addBody(secondObstacle, PhysicsBodyType.STATIC)
        add(secondObstacle)

        ui.add(UiLabel(24f, 18f, 560f, 28f, "RIFT ENGINE • TOP-DOWN MODE", 0xFFFFFFFF.toInt(), 20f, true))
        ui.add(UiLabel(24f, 46f, 560f, 24f, "WASD / arrows or joystick: move   TAB: side-view", 0xFFDCE1EB.toInt(), 14f))
        ui.add(joystick)
    }

    override fun update(deltaSeconds: Float) {
        val horizontal = ((if (input.isDown("move.right")) 1f else 0f) -
            (if (input.isDown("move.left")) 1f else 0f) + joystick.axisX).coerceIn(-1f, 1f)
        val vertical = ((if (input.isDown("move.down")) 1f else 0f) -
            (if (input.isDown("move.up")) 1f else 0f) + joystick.axisY).coerceIn(-1f, 1f)
        player.movementInput = MovementInput(horizontal, vertical)

        super.update(deltaSeconds)

        if (input.wasPressed("switch.mode")) {
            gameState.putInt("mode-switches", gameState.int("mode-switches") + 1)
            requestSceneChange(PlatformerDemoScene())
        }
    }

    override fun render(context: RenderContext) {
        context.clear(0xFF0F1320.toInt())

        for (x in 0..context.width step 64) {
            context.drawLine(x.toFloat(), 0f, x.toFloat(), context.height.toFloat(), 0x23A0BEFF)
        }
        for (y in 0..context.height step 64) {
            context.drawLine(0f, y.toFloat(), context.width.toFloat(), y.toFloat(), 0x23A0BEFF)
        }

        super.render(context)
    }

    override fun onTouch(
        x: Float,
        y: Float,
        screenWidth: Float,
        screenHeight: Float
    ) {
        player.x = x - player.width / 2f
        player.y = y - player.height / 2f
    }
}
