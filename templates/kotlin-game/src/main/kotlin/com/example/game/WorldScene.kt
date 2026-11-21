package com.example.game

import com.rift.engine.entity.GameObject
import com.rift.engine.input.EngineKey
import com.rift.engine.physics.PhysicsBodyType
import com.rift.engine.scene.Scene
import com.rift.engine.ui.UiLabel

class WorldScene : Scene() {
    private val player = GameObject(
        x = 120f,
        y = 120f,
        width = 28f,
        height = 28f,
        color = 0xFF67B7FF.toInt()
    )

    init {
        input.bindKey("move.left", EngineKey.A)
        input.bindKey("move.left", EngineKey.LEFT)
        input.bindKey("move.right", EngineKey.D)
        input.bindKey("move.right", EngineKey.RIGHT)
        input.bindKey("move.up", EngineKey.W)
        input.bindKey("move.up", EngineKey.UP)
        input.bindKey("move.down", EngineKey.S)
        input.bindKey("move.down", EngineKey.DOWN)

        player.renderDefaultVisual = true
        physics.gravityY = 0f
        physics.addBody(player, PhysicsBodyType.DYNAMIC, gravityScale = 0f)
        add(player)

        ui.add(UiLabel(16f, 12f, 300f, 28f, "Rift game template — WASD / arrows"))
    }

    override fun update(deltaSeconds: Float) {
        val horizontal = (if (input.isDown("move.right")) 1f else 0f) -
            (if (input.isDown("move.left")) 1f else 0f)
        val vertical = (if (input.isDown("move.down")) 1f else 0f) -
            (if (input.isDown("move.up")) 1f else 0f)
        val lengthSquared = horizontal * horizontal + vertical * vertical
        val normalization = if (lengthSquared > 1f) {
            1f / kotlin.math.sqrt(lengthSquared)
        } else {
            1f
        }

        player.velocityX = horizontal * normalization * 180f
        player.velocityY = vertical * normalization * 180f

        super.update(deltaSeconds)
    }
}
