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

    private val pressedKeys = mutableSetOf<Int>()

    init {
        add(player)
        add(GameObject(420f, 170f, 100f, 100f, 0xFFFFAA50.toInt()))
        add(GameObject(650f, 350f, 80f, 80f, 0xFF78DCAA.toInt()))
    }

    override fun update(deltaSeconds: Float) {
        super.update(deltaSeconds)

        val movementSpeed = 240f
        val distance = movementSpeed * deltaSeconds

        if (EngineKey.A in pressedKeys || EngineKey.LEFT in pressedKeys) {
            player.x -= distance
        }
        if (EngineKey.D in pressedKeys || EngineKey.RIGHT in pressedKeys) {
            player.x += distance
        }
        if (EngineKey.W in pressedKeys || EngineKey.UP in pressedKeys) {
            player.y -= distance
        }
        if (EngineKey.S in pressedKeys || EngineKey.DOWN in pressedKeys) {
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

    override fun onKey(
        keyCode: Int,
        pressed: Boolean
    ) {
        if (pressed) {
            pressedKeys.add(keyCode)
        } else {
            pressedKeys.remove(keyCode)
        }
    }
}
