package com.example.game

import com.rift.engine.entity.GameObject
import com.rift.engine.graphics.RenderContext
import com.rift.engine.input.EngineKey
import com.rift.engine.scene.Scene

class WorldScene : Scene() {
    private val player = GameObject(
        x = 120f,
        y = 160f,
        width = 42f,
        height = 42f,
        color = 0xFF72A8FF.toInt()
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

        add(player)
    }

    override fun update(deltaSeconds: Float) {
        super.update(deltaSeconds)

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
        val speed = 180f * deltaSeconds

        player.x += horizontal * normalization * speed
        player.y += vertical * normalization * speed
    }

    override fun render(context: RenderContext) {
        context.clear(0xFF111827.toInt())
        super.render(context)
        context.drawText(
            "Rift sample project",
            24f,
            36f,
            0xFFFFFFFF.toInt(),
            22f,
            true
        )
        context.drawText(
            "WASD / arrows to move",
            24f,
            62f,
            0xFFD1D5DB.toInt(),
            15f
        )
    }
}
