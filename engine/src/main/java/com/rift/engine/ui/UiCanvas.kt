package com.rift.engine.ui

import com.rift.engine.graphics.RenderContext
import com.rift.engine.graphics.Sprite
import com.rift.engine.input.PointerEvent
import com.rift.engine.input.PointerEventType

abstract class UiElement(
    x: Float,
    y: Float,
    width: Float,
    height: Float
) {
    var x: Float = x
        set(value) {
            require(value.isFinite()) { "UI x must be finite." }
            field = value
        }
    var y: Float = y
        set(value) {
            require(value.isFinite()) { "UI y must be finite." }
            field = value
        }
    var width: Float = width
        set(value) {
            require(value.isFinite() && value >= 0f) { "UI width must be finite and non-negative." }
            field = value
        }
    var height: Float = height
        set(value) {
            require(value.isFinite() && value >= 0f) { "UI height must be finite and non-negative." }
            field = value
        }

    var visible: Boolean = true
    var enabled: Boolean = true
    var zIndex: Int = 0

    init {
        require(x.isFinite() && y.isFinite()) { "UI position must be finite." }
        require(width.isFinite() && height.isFinite() && width >= 0f && height >= 0f) {
            "UI dimensions must be finite and non-negative."
        }
    }

    open fun update(deltaSeconds: Float) = Unit
    abstract fun render(context: RenderContext)
    open fun onPointer(event: PointerEvent): Boolean = false
    open fun onRemoved() = Unit

    fun contains(pointX: Float, pointY: Float): Boolean =
        pointX >= x && pointX <= x + width &&
            pointY >= y && pointY <= y + height
}

class UiPanel(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    var color: Int = 0xCC111827.toInt(),
    cornerRadius: Float = 8f
) : UiElement(x, y, width, height) {
    var cornerRadius: Float = cornerRadius
        set(value) {
            require(value.isFinite() && value >= 0f) {
                "cornerRadius must be finite and non-negative."
            }
            field = value
        }

    init {
        require(cornerRadius.isFinite() && cornerRadius >= 0f)
    }

    override fun render(context: RenderContext) {
        context.drawRoundRect(x, y, x + width, y + height, cornerRadius, color)
    }
}

class UiLabel(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    var text: String,
    var color: Int = 0xFFFFFFFF.toInt(),
    fontSize: Float = 16f,
    var bold: Boolean = false
) : UiElement(x, y, width, height) {
    var fontSize: Float = fontSize
        set(value) {
            require(value.isFinite() && value > 0f) {
                "fontSize must be finite and greater than zero."
            }
            field = value
        }

    init {
        require(fontSize.isFinite() && fontSize > 0f)
    }

    override fun render(context: RenderContext) {
        context.drawText(text, x, y + fontSize, color, fontSize, bold)
    }
}

class UiButton(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    var text: String,
    private val onClick: () -> Unit,
    var backgroundColor: Int = 0xFF315A8C.toInt(),
    var pressedColor: Int = 0xFF24446B.toInt(),
    var textColor: Int = 0xFFFFFFFF.toInt(),
    fontSize: Float = 16f,
    cornerRadius: Float = 8f
) : UiElement(x, y, width, height) {
    private val pressedPointers = mutableSetOf<Int>()

    var fontSize: Float = fontSize
        set(value) {
            require(value.isFinite() && value > 0f) {
                "fontSize must be finite and greater than zero."
            }
            field = value
        }

    var cornerRadius: Float = cornerRadius
        set(value) {
            require(value.isFinite() && value >= 0f) {
                "cornerRadius must be finite and non-negative."
            }
            field = value
        }

    val isPressed: Boolean
        get() = pressedPointers.isNotEmpty()

    init {
        require(fontSize.isFinite() && fontSize > 0f)
        require(cornerRadius.isFinite() && cornerRadius >= 0f)
    }

    override fun render(context: RenderContext) {
        context.drawRoundRect(
            x,
            y,
            x + width,
            y + height,
            cornerRadius,
            if (isPressed) pressedColor else backgroundColor
        )
        val textWidth = context.measureText(text, fontSize, true)
        val textX = x + ((width - textWidth) * 0.5f).coerceAtLeast(0f)
        val textY = y + (height + fontSize) * 0.5f - fontSize * 0.15f
        context.drawText(text, textX, textY, textColor, fontSize, true)
    }

    override fun onPointer(event: PointerEvent): Boolean {
        return when (event.type) {
            PointerEventType.DOWN -> {
                if (!enabled || !visible || !contains(event.x, event.y)) false
                else {
                    pressedPointers.add(event.pointerId)
                    true
                }
            }
            PointerEventType.UP -> {
                val wasPressed = pressedPointers.remove(event.pointerId)
                if (wasPressed && enabled && visible && contains(event.x, event.y)) onClick()
                wasPressed
            }
            PointerEventType.CANCEL -> pressedPointers.remove(event.pointerId)
            PointerEventType.MOVE -> event.pointerId in pressedPointers
        }
    }

    override fun onRemoved() {
        pressedPointers.clear()
    }
}

class UiImage(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    var sprite: Sprite,
    alpha: Float = 1f
) : UiElement(x, y, width, height) {
    var alpha: Float = alpha
        set(value) {
            require(value.isFinite() && value in 0f..1f) {
                "alpha must be between zero and one."
            }
            field = value
        }

    init {
        require(alpha.isFinite() && alpha in 0f..1f)
    }

    override fun render(context: RenderContext) {
        sprite.draw(context, x, y, width, height, alpha)
    }
}

/**
 * Screen-space UI layer. Elements are drawn by ascending zIndex; hit testing
 * runs in reverse order so the visually topmost interactive control receives the pointer.
 */
class UiCanvas {
    private val elements = mutableListOf<UiElement>()
    private val pressedTargets = mutableMapOf<Int, UiElement>()

    fun add(element: UiElement): Boolean {
        if (elements.any { it === element }) return false
        elements.add(element)
        return true
    }

    fun remove(element: UiElement): Boolean {
        val removed = elements.removeAll { it === element }
        if (removed) {
            pressedTargets.entries.removeAll { it.value === element }
            element.onRemoved()
        }
        return removed
    }

    fun clear() {
        elements.forEach(UiElement::onRemoved)
        elements.clear()
        pressedTargets.clear()
    }

    fun resetInput() {
        pressedTargets.values.distinct().forEach(UiElement::onRemoved)
        pressedTargets.clear()
    }

    fun elements(): List<UiElement> = elements.toList()

    fun update(deltaSeconds: Float) {
        require(deltaSeconds.isFinite() && deltaSeconds >= 0f) {
            "deltaSeconds must be finite and non-negative."
        }
        elements.filter { it.visible && it.enabled }.forEach { it.update(deltaSeconds) }
    }

    fun render(context: RenderContext) {
        elements.filter { it.visible }.sortedBy { it.zIndex }.forEach { it.render(context) }
    }

    fun dispatchPointer(event: PointerEvent): Boolean {
        return when (event.type) {
            PointerEventType.DOWN -> {
                for (target in elements.sortedBy { it.zIndex }.asReversed()) {
                    if (target.visible && target.enabled &&
                        target.contains(event.x, event.y) && target.onPointer(event)
                    ) {
                        pressedTargets[event.pointerId] = target
                        return true
                    }
                }
                false
            }
            PointerEventType.UP, PointerEventType.CANCEL -> {
                val target = pressedTargets.remove(event.pointerId) ?: return false
                target.onPointer(event)
            }
            PointerEventType.MOVE -> {
                pressedTargets[event.pointerId]?.onPointer(event) ?: false
            }
        }
    }
}
