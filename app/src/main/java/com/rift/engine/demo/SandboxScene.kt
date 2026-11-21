package com.rift.engine.demo

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.rift.engine.entity.GameObject
import com.rift.engine.scene.Scene

class SandboxScene : Scene() {
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(15, 19, 31) }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(35, 160, 190, 255)
        strokeWidth = 1f
    }
    private val player = GameObject(140f, 220f, 64f, 64f, Color.rgb(105, 150, 255))

    init {
        add(player)
        add(GameObject(420f, 170f, 100f, 100f, Color.rgb(255, 170, 80)))
        add(GameObject(650f, 350f, 80f, 80f, Color.rgb(120, 220, 170)))
    }

    override fun render(canvas: Canvas) {
        canvas.drawColor(backgroundPaint.color)
        for (x in 0..canvas.width step 64) {
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), canvas.height.toFloat(), gridPaint)
        }
        for (y in 0..canvas.height step 64) {
            canvas.drawLine(0f, y.toFloat(), canvas.width.toFloat(), y.toFloat(), gridPaint)
        }
        super.render(canvas)
        canvas.drawText("RIFT ENGINE  •  CORE TEST", 32f, 52f, textPaint)
        textPaint.textSize = 22f
        canvas.drawText("Touch anywhere to move the blue player", 32f, 86f, textPaint)
        textPaint.textSize = 34f
    }

    override fun onTouch(x: Float, y: Float, screenWidth: Float, screenHeight: Float) {
        player.x = x - player.width / 2f
        player.y = y - player.height / 2f
    }
}
