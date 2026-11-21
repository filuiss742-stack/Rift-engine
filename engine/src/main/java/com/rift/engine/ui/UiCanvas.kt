package com.rift.engine.ui

import com.rift.engine.graphics.RenderContext
import com.rift.engine.graphics.Sprite
import com.rift.engine.input.EngineKey
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
    var focusable: Boolean = false
    var isFocused: Boolean = false
        internal set

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

enum class UiLayout {
    ABSOLUTE,
    VERTICAL,
    HORIZONTAL
}

class UiPanel(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    var color: Int = 0xCC111827.toInt(),
    cornerRadius: Float = 8f
) : UiElement(x, y, width, height) {
    private val childElements = mutableListOf<UiElement>()
    private val pressedChildren = mutableMapOf<Int, UiElement>()

    var cornerRadius: Float = cornerRadius
        set(value) {
            require(value.isFinite() && value >= 0f) {
                "cornerRadius must be finite and non-negative."
            }
            field = value
        }

    var layout: UiLayout = UiLayout.ABSOLUTE
        set(value) {
            field = value
            layoutChildren()
        }

    var padding: Float = 8f
        set(value) {
            require(value.isFinite() && value >= 0f) { "padding must be finite and non-negative." }
            field = value
            layoutChildren()
        }

    var gap: Float = 8f
        set(value) {
            require(value.isFinite() && value >= 0f) { "gap must be finite and non-negative." }
            field = value
            layoutChildren()
        }

    init {
        require(cornerRadius.isFinite() && cornerRadius >= 0f)
    }

    fun add(child: UiElement): Boolean {
        if (childElements.any { it === child }) return false
        childElements.add(child)
        layoutChildren()
        return true
    }

    fun remove(child: UiElement): Boolean {
        val removed = childElements.removeAll { it === child }
        if (removed) {
            pressedChildren.entries.removeAll { it.value === child }
            child.isFocused = false
            child.onRemoved()
            layoutChildren()
        }
        return removed
    }

    fun children(): List<UiElement> = childElements.toList()

    override fun update(deltaSeconds: Float) {
        childElements.toList().forEach { child ->
            if (child.visible && child.enabled) child.update(deltaSeconds)
        }
    }

    override fun render(context: RenderContext) {
        context.drawRoundRect(x, y, x + width, y + height, cornerRadius, color)
        context.pushState()
        try {
            context.clipRect(x, y, x + width, y + height)
            context.translate(x, y)
            childElements.filter { it.visible }
                .sortedBy { it.zIndex }
                .forEach { it.render(context) }
        } finally {
            context.popState()
        }
    }

    override fun onPointer(event: PointerEvent): Boolean {
        val localEvent = event.copy(x = event.x - x, y = event.y - y)
        return when (event.type) {
            PointerEventType.DOWN -> {
                if (!enabled || !visible || !contains(event.x, event.y)) {
                    false
                } else {
                    for (child in childElements.toList().sortedBy { it.zIndex }.asReversed()) {
                        if (child.visible && child.enabled &&
                            child.contains(localEvent.x, localEvent.y) &&
                            child.onPointer(localEvent)
                        ) {
                            pressedChildren[event.pointerId] = child
                            return true
                        }
                    }
                    true
                }
            }
            PointerEventType.MOVE, PointerEventType.UP, PointerEventType.CANCEL -> {
                val child = pressedChildren[event.pointerId] ?: return contains(event.x, event.y)
                child.onPointer(localEvent)
                if (event.type == PointerEventType.UP || event.type == PointerEventType.CANCEL) {
                    pressedChildren.remove(event.pointerId)
                }
                true
            }
        }
    }

    override fun onRemoved() {
        pressedChildren.values.distinct().forEach { it.onRemoved() }
        pressedChildren.clear()
        childElements.forEach {
            it.isFocused = false
            it.onRemoved()
        }
    }

    private fun layoutChildren() {
        when (layout) {
            UiLayout.ABSOLUTE -> Unit
            UiLayout.VERTICAL -> {
                var cursor = padding
                childElements.forEach { child ->
                    child.x = padding
                    child.y = cursor
                    cursor += child.height + gap
                }
            }
            UiLayout.HORIZONTAL -> {
                var cursor = padding
                childElements.forEach { child ->
                    child.x = cursor
                    child.y = padding
                    cursor += child.width + gap
                }
            }
        }
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
    var focusColor: Int = 0xFFFFD166.toInt(),
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
        focusable = true
    }

    override fun render(context: RenderContext) {
        if (isFocused) {
            context.drawRoundRect(
                x - 3f,
                y - 3f,
                x + width + 3f,
                y + height + 3f,
                cornerRadius + 3f,
                focusColor
            )
        }
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

    internal fun activate() {
        if (enabled && visible) onClick()
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
    internal var diagnostics: com.rift.engine.diagnostics.DiagnosticReporter? = null
    private val elements = mutableListOf<UiElement>()
    private val pressedTargets = mutableMapOf<Int, UiElement>()
    private val failedElements = java.util.Collections.newSetFromMap(
        java.util.IdentityHashMap<UiElement, Boolean>()
    )
    private val handledUiKeys = mutableSetOf<Int>()
    var focusedElement: UiElement? = null
        private set

    fun add(element: UiElement): Boolean {
        if (elements.any { it === element }) return false
        failedElements.remove(element)
        elements.add(element)
        return true
    }

    fun remove(element: UiElement): Boolean {
        val removed = elements.removeAll { it === element }
        if (removed) {
            failedElements.remove(element)
            pressedTargets.entries.removeAll { it.value === element }
            if (containsElement(element, focusedElement)) setFocus(null)
            safely("ui.detach", element) { element.onRemoved() }
        }
        return removed
    }

    fun clear() {
        elements.toList().forEach { element ->
            safely("ui.detach", element) { element.onRemoved() }
        }
        elements.clear()
        pressedTargets.clear()
        failedElements.clear()
        setFocus(null)
        handledUiKeys.clear()
    }

    fun resetInput() {
        pressedTargets.values.distinct().forEach { element ->
            safely("ui.input.reset", element) { element.onRemoved() }
        }
        pressedTargets.clear()
    }

    fun elements(): List<UiElement> = elements.toList()

    fun setFocus(element: UiElement?): Boolean {
        if (element != null && (
            !element.focusable ||
                !element.visible ||
                !element.enabled ||
                element in failedElements ||
                focusableElements().none { it === element }
        )) return false
        focusedElement?.isFocused = false
        focusedElement = element
        focusedElement?.isFocused = true
        return true
    }

    fun focusNext(): Boolean {
        val candidates = focusableElements().filter {
            it.visible && it.enabled && it !in failedElements
        }
        if (candidates.isEmpty()) {
            setFocus(null)
            return false
        }
        val currentIndex = candidates.indexOfFirst { it === focusedElement }
        val next = candidates[(currentIndex + 1) % candidates.size]
        setFocus(next)
        return true
    }

    /** Returns true when the UI consumed the keyboard event. */
    fun dispatchKey(keyCode: Int, pressed: Boolean): Boolean {
        if (keyCode in handledUiKeys) {
            if (!pressed) handledUiKeys.remove(keyCode)
            return true
        }
        if (!pressed) return false
        return when (keyCode) {
            EngineKey.TAB -> {
                if (focusNext()) {
                    handledUiKeys.add(keyCode)
                    true
                } else false
            }
            EngineKey.ENTER, EngineKey.SPACE -> {
                val target = focusedElement
                if (target == null || target !in focusableElements()) {
                    setFocus(null)
                    false
                } else {
                    handledUiKeys.add(keyCode)
                    if (target is UiButton) {
                        safely("ui.activate", target) { target.activate() }
                    }
                    true
                }
            }
            else -> false
        }
    }

    fun update(deltaSeconds: Float) {
        require(deltaSeconds.isFinite() && deltaSeconds >= 0f) {
            "deltaSeconds must be finite and non-negative."
        }
        elements.toList().forEach { element ->
            if (element.visible && element.enabled && element !in failedElements) {
                safely("ui.update", element) { element.update(deltaSeconds) }
            }
        }
    }

    fun render(context: RenderContext) {
        elements.filter { it.visible && it !in failedElements }
            .sortedBy { it.zIndex }
            .forEach { element ->
                safely("ui.render", element) { element.render(context) }
            }
    }

    fun dispatchPointer(event: PointerEvent): Boolean {
        return when (event.type) {
            PointerEventType.DOWN -> {
                for (target in elements.toList().sortedBy { it.zIndex }.asReversed()) {
                    if (target.visible && target.enabled && target !in failedElements &&
                        target.contains(event.x, event.y)
                    ) {
                        val consumed = try {
                            target.onPointer(event)
                        } catch (exception: Exception) {
                            quarantine("ui.input", target, exception)
                            true
                        }
                        if (consumed) {
                            pressedTargets[event.pointerId] = target
                            return true
                        }
                    }
                }
                false
            }
            PointerEventType.UP -> {
                val target = pressedTargets.remove(event.pointerId) ?: return false
                if (target !in failedElements) {
                    safely("ui.input", target) { target.onPointer(event) }
                }
                true
            }
            PointerEventType.CANCEL -> {
                val target = pressedTargets.remove(event.pointerId) ?: return false
                if (target !in failedElements) {
                    safely("ui.input", target) { target.onPointer(event) }
                }
                true
            }
            PointerEventType.MOVE -> {
                val target = pressedTargets[event.pointerId] ?: return false
                if (target !in failedElements) {
                    safely("ui.input", target) { target.onPointer(event) }
                }
                true
            }
        }
    }

    fun isElementQuarantined(element: UiElement): Boolean =
        element in failedElements

    fun retryElement(element: UiElement): Boolean {
        if (elements.none { it === element }) return false
        return failedElements.remove(element)
    }

    private fun safely(
        subsystem: String,
        element: UiElement,
        action: () -> Unit
    ) {
        if (element in failedElements) return
        try {
            action()
        } catch (exception: Exception) {
            quarantine(subsystem, element, exception)
        }
    }

    private fun focusableElements(): List<UiElement> {
        val result = mutableListOf<UiElement>()
        fun collect(element: UiElement) {
            if (element.focusable) result += element
            if (element is UiPanel) element.children().forEach(::collect)
        }
        elements.sortedBy { it.zIndex }.forEach(::collect)
        return result
    }

    private fun containsElement(root: UiElement, target: UiElement?): Boolean {
        if (target == null) return false
        if (root === target) return true
        return root is UiPanel && root.children().any { containsElement(it, target) }
    }

    private fun quarantine(
        subsystem: String,
        element: UiElement,
        exception: Exception
    ) {
        if (!failedElements.add(element)) return
        pressedTargets.entries.removeAll { it.value === element }
        if (containsElement(element, focusedElement)) setFocus(null)
        try {
            element.onRemoved()
        } catch (cleanupException: Exception) {
            exception.addSuppressed(cleanupException)
        }
        val reporter = diagnostics
        if (reporter == null) throw exception
        reporter.reportException(
            subsystem,
            exception,
            "Inspect the failing UI element and call retryElement() after correcting its behavior."
        )
    }
}
