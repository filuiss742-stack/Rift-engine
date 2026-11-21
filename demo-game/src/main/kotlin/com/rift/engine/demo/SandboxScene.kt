package com.rift.engine.demo

import com.rift.engine.entity.GameObject
import com.rift.engine.graphics.RenderContext
import com.rift.engine.input.EngineKey
import com.rift.engine.scene.Scene

/**
 * The same demo scene is compiled into both platform applications.
 */
class SandboxScene : Scene() {
    private val player = GameObject(
        x = 140f,
        y = 220f,
        width = 64f,
        height = 64f,
        color = 0xFF6996FF.toInt()
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
        add(GameObject(420f, 170f, 100f, 100f, 0xFFFFAA50.toInt()))
        add(GameObject(650f, 350f, 80f, 80f, 0xFF78DCAA.toInt()))
    }

    override fun update(deltaSeconds: Float) {
        super.update(deltaSeconds)

        val movementSpeed = 240f
        val distance = movementSpeed * deltaSeconds

        if (input.isDown("move.left")) {
            player.x -= distance
        }
        if (input.isDown("move.right")) {
            player.x += distance
        }
        if (input.isDown("move.up")) {
            player.y -= distance
        }
        if (input.isDown("move.down")) {
            player.y += distance
        }
    }

    override fun render(context: RenderContext) {
        context.clear(0xFF0F1320.toInt())

        for (x in 0..context.width step 64) {
            context.drawLine(
                x.toFloat(),
                0f,
                x.toFloat(),
                context.height.toFloat(),
                0x23A0BEFF
            )
        }

        for (y in 0..context.height step 64) {
            context.drawLine(
                0f,
                y.toFloat(),
                context.width.toFloat(),
                y.toFloat(),
                0x23A0BEFF
            )
        }

        super.render(context)

        context.drawText(
            "RIFT ENGINE • SHARED GAME",
            32f,
            52f,
            0xFFFFFFFF.toInt(),
            30f,
            true
        )
        context.drawText(
            "Touch / click to move",
            32f,
            86f,
            0xFFFFFFFF.toInt(),
            20f
        )
        context.drawText(
            "WASD / arrows: move",
            32f,
            114f,
            0xFFFFFFFF.toInt(),
            16f
        )
    }

    override fun onTouch(
        x: Float,
        y: Float,
        screenWidth: Float,
        screenHeight: Float
    ) {
        player.x = x - player.width / 2
        player.y = y - player.height / 2
    }
}
