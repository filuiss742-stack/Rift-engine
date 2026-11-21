package com.rift.engine.ui

import com.rift.engine.graphics.RenderContext
import com.rift.engine.input.PointerEvent
import com.rift.engine.input.PointerEventType
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Screen-space analog joystick for touch and pointer devices.
 *
 * Axes are normalized to -1..1. Only one pointer owns the joystick at a time.
 */
class VirtualJoystick(
    x: Float,
    y: Float,
    size: Float,
    deadZone: Float = 0.16f,
    private val onAxisChanged: (Float, Float) -> Unit = { _, _ -> },
    var baseColor: Int = 0x665A7899,
    var knobColor: Int = 0xFFB8D7F5.toInt()
) : UiElement(x, y, size, size) {
    var deadZone: Float = deadZone
        set(value) {
            require(value.isFinite() && value in 0f..<1f) {
                "deadZone must be finite and in the range 0..1."
            }
            field = value
        }

    var axisX: Float = 0f
        private set
    var axisY: Float = 0f
        private set

    private var activePointerId: Int? = null
    private val radius: Float get() = min(width, height) * 0.5f

    init {
        require(size.isFinite() && size > 0f) { "Joystick size must be finite and greater than zero." }
        require(deadZone.isFinite() && deadZone in 0f..<1f) {
            "deadZone must be finite and in the range 0..1."
        }
    }

    override fun onPointer(event: PointerEvent): Boolean {
        return when (event.type) {
            PointerEventType.DOWN -> {
                if (!enabled || !visible || activePointerId != null || !insideBase(event.x, event.y)) {
                    false
                } else {
                    activePointerId = event.pointerId
                    updateAxes(event.x, event.y)
                    true
                }
            }
            PointerEventType.MOVE -> {
                if (activePointerId != event.pointerId) false
                else {
                    updateAxes(event.x, event.y)
                    true
                }
            }
            PointerEventType.UP, PointerEventType.CANCEL -> {
                if (activePointerId != event.pointerId) false
                else {
                    release()
                    true
                }
            }
        }
    }

    override fun render(context: RenderContext) {
        val safeRadius = radius
        val centerX = x + width * 0.5f
        val centerY = y + height * 0.5f
        context.drawRoundRect(x, y, x + width, y + height, safeRadius, baseColor)
        val knobRadius = safeRadius * 0.38f
        val knobCenterX = centerX + axisX * safeRadius * 0.58f
        val knobCenterY = centerY + axisY * safeRadius * 0.58f
        context.drawRoundRect(
            knobCenterX - knobRadius,
            knobCenterY - knobRadius,
            knobCenterX + knobRadius,
            knobCenterY + knobRadius,
            knobRadius,
            knobColor
        )
    }

    override fun onRemoved() {
        release()
    }

    private fun insideBase(pointX: Float, pointY: Float): Boolean {
        val dx = pointX - (x + width * 0.5f)
        val dy = pointY - (y + height * 0.5f)
        return dx * dx + dy * dy <= radius * radius
    }

    private fun updateAxes(pointX: Float, pointY: Float) {
        val safeRadius = radius
        if (safeRadius <= 0f) return
        val dx = pointX - (x + width * 0.5f)
        val dy = pointY - (y + height * 0.5f)
        val rawX = dx / safeRadius
        val rawY = dy / safeRadius
        val length = sqrt(rawX * rawX + rawY * rawY)
        val clampedLength = length.coerceAtMost(1f)
        val scale = when {
            length <= deadZone || length <= 0.000001f -> 0f
            else -> ((clampedLength - deadZone) / (1f - deadZone)) / length
        }
        setAxes(rawX * scale, rawY * scale)
    }

    private fun release() {
        activePointerId = null
        setAxes(0f, 0f)
    }

    private fun setAxes(horizontal: Float, vertical: Float) {
        if (axisX == horizontal && axisY == vertical) return
        axisX = horizontal
        axisY = vertical
        onAxisChanged(axisX, axisY)
    }
}

/**
 * Press-and-hold touch button. Callbacks fire only on transitions between
 * zero and one or more active pointers, so multiple fingers do not flicker.
 */
class VirtualButton(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    var label: String,
    private val onPressed: () -> Unit,
    private val onReleased: () -> Unit = {},
    var idleColor: Int = 0xAA315A8C.toInt(),
    var pressedColor: Int = 0xDD24446B.toInt(),
    var textColor: Int = 0xFFFFFFFF.toInt()
) : UiElement(x, y, width, height) {
    private val activePointers = mutableSetOf<Int>()

    val isPressed: Boolean
        get() = activePointers.isNotEmpty()

    override fun onPointer(event: PointerEvent): Boolean {
        return when (event.type) {
            PointerEventType.DOWN -> {
                if (!enabled || !visible || !contains(event.x, event.y)) false
                else {
                    val wasPressed = isPressed
                    activePointers.add(event.pointerId)
                    if (!wasPressed && isPressed) onPressed()
                    true
                }
            }
            PointerEventType.UP, PointerEventType.CANCEL -> {
                val wasPressed = isPressed
                if (!activePointers.remove(event.pointerId)) false
                else {
                    if (wasPressed && !isPressed) onReleased()
                    true
                }
            }
            PointerEventType.MOVE -> event.pointerId in activePointers
        }
    }

    override fun render(context: RenderContext) {
        context.drawRoundRect(
            x,
            y,
            x + width,
            y + height,
            min(width, height) * 0.2f,
            if (isPressed) pressedColor else idleColor
        )
        val textWidth = context.measureText(label, 16f, true)
        val textX = x + ((width - textWidth) * 0.5f).coerceAtLeast(0f)
        val textY = y + (height + 16f) * 0.5f
        context.drawText(label, textX, textY, textColor, 16f, true)
    }

    override fun onRemoved() {
        val wasPressed = isPressed
        activePointers.clear()
        if (wasPressed) onReleased()
    }
}
