package com.rift.engine.demo

import com.rift.engine.entity.GameObject
import com.rift.engine.graphics.RenderContext
import com.rift.engine.input.EngineKey
import com.rift.engine.input.PointerEvent
import com.rift.engine.input.PointerEventType
import com.rift.engine.movement.MovementInput
import com.rift.engine.movement.PlatformerMovementProfile
import com.rift.engine.physics.PhysicsBodyType
import com.rift.engine.scene.Scene
import com.rift.engine.ui.UiLabel

/**
 * Side-view mode used to prove that a game can replace movement and physics
 * behavior while retaining the same SceneManager-owned progression state.
 */
class PlatformerDemoScene : Scene() {
    private val player = GameObject(
        x = 72f,
        y = 420f,
        width = 34f,
        height = 48f,
        color = 0xFFFFC56B.toInt()
    ).apply {
        movementProfile = PlatformerMovementProfile(
            horizontalSpeed = 220f,
            gravity = 0f,
            jumpSpeed = 440f,
            maxFallSpeed = 900f
        )
        renderLayer = 10
    }

    init {
        input.bindKey("move.left", EngineKey.A)
        input.bindKey("move.left", EngineKey.LEFT)
        input.bindKey("move.right", EngineKey.D)
        input.bindKey("move.right", EngineKey.RIGHT)
        input.bindKey("jump", EngineKey.SPACE)
        input.bindKey("jump", EngineKey.W)
        input.bindKey("jump", EngineKey.UP)
        input.bindKey("switch.mode", EngineKey.TAB)

        physics.gravityX = 0f
        physics.gravityY = 980f
        physics.addBody(player, PhysicsBodyType.DYNAMIC)

        val ground = GameObject(0f, 520f, 1100f, 80f, 0xFF42634F.toInt()).apply {
            renderLayer = 0
        }
        physics.addBody(ground, PhysicsBodyType.STATIC)
        add(ground)

        val lowerPlatform = GameObject(230f, 430f, 180f, 20f, 0xFF668A70.toInt())
        physics.addBody(lowerPlatform, PhysicsBodyType.STATIC)
        add(lowerPlatform)

        val upperPlatform = GameObject(520f, 340f, 180f, 20f, 0xFF668A70.toInt())
        physics.addBody(upperPlatform, PhysicsBodyType.STATIC)
        add(upperPlatform)

        add(player)

        val switches = gameState.int("mode-switches")
        ui.add(
            UiLabel(
                24f,
                18f,
                700f,
                28f,
                "RIFT ENGINE • SIDE-VIEW MODE",
                0xFFFFFFFF.toInt(),
                20f,
                true
            )
        )
        ui.add(
            UiLabel(
                24f,
                46f,
                760f,
                24f,
                "A/D or arrows: move   SPACE/W/↑: jump   TAB: top-down   switches: $switches",
                0xFFDCE1EB.toInt(),
                14f
            )
        )
    }

    override fun update(deltaSeconds: Float) {
        val horizontal = (if (input.isDown("move.right")) 1f else 0f) -
            (if (input.isDown("move.left")) 1f else 0f)
        player.movementInput = MovementInput(
            horizontal = horizontal,
            jumpPressed = input.wasPressed("jump")
        )

        super.update(deltaSeconds)

        if (input.wasPressed("switch.mode")) {
            gameState.putInt("mode-switches", gameState.int("mode-switches") + 1)
            requestSceneChange(SandboxScene())
        }
    }

    override fun render(context: RenderContext) {
        context.clear(0xFF182332.toInt())
        for (x in 0..context.width step 80) {
            context.drawLine(x.toFloat(), 0f, x.toFloat(), context.height.toFloat(), 0x1FFFFFFF)
        }
        super.render(context)
    }

    override fun onPointer(event: PointerEvent) {
        if (event.type == PointerEventType.DOWN && player.isGrounded) {
            player.velocityY = -440f
            player.isGrounded = false
        }
    }
}
