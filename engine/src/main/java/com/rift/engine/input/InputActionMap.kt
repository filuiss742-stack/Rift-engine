package com.rift.engine.input

/**
 * Maps device-independent keyboard keys and pointer buttons to named actions.
 *
 * The map is intended to be owned and updated by the game-loop thread.
 * Platform adapters forward physical events; game code reads action state.
 */
class InputActionMap {
    private val keysByAction = linkedMapOf<String, MutableSet<Int>>()
    private val actionsByKey = linkedMapOf<Int, MutableSet<String>>()
    private val pointerButtonsByAction = linkedMapOf<String, MutableSet<Int>>()
    private val actionsByPointerButton = linkedMapOf<Int, MutableSet<String>>()
    private val downKeys = mutableSetOf<Int>()
    private val downPointerButtons = linkedMapOf<Int, MutableSet<Int>>()
    private val pointerPositions = linkedMapOf<Int, Pair<Float, Float>>()
    private val justPressedActions = mutableSetOf<String>()
    private val justReleasedActions = mutableSetOf<String>()

    var pointerX: Float = 0f
        private set
    var pointerY: Float = 0f
        private set
    var pointerDeltaX: Float = 0f
        private set
    var pointerDeltaY: Float = 0f
        private set

    fun bindKey(actionId: String, keyCode: Int) {
        require(actionId.isNotBlank()) { "actionId must not be blank." }
        require(keyCode > 0) { "keyCode must be greater than zero." }
        val wasDown = isDownInternal(actionId)
        val keys = keysByAction.getOrPut(actionId) { linkedSetOf() }
        if (!keys.add(keyCode)) return
        actionsByKey.getOrPut(keyCode) { linkedSetOf() }.add(actionId)
        updateEdges(actionId, wasDown)
    }

    fun bindPointerButton(actionId: String, buttonCode: Int) {
        require(actionId.isNotBlank()) { "actionId must not be blank." }
        require(buttonCode > PointerButtons.NONE) { "buttonCode must be greater than zero." }
        val wasDown = isDownInternal(actionId)
        val buttons = pointerButtonsByAction.getOrPut(actionId) { linkedSetOf() }
        if (!buttons.add(buttonCode)) return
        actionsByPointerButton.getOrPut(buttonCode) { linkedSetOf() }.add(actionId)
        updateEdges(actionId, wasDown)
    }

    fun unbindKey(actionId: String, keyCode: Int): Boolean {
        val keys = keysByAction[actionId] ?: return false
        val wasDown = isDownInternal(actionId)
        if (!keys.remove(keyCode)) return false

        actionsByKey[keyCode]?.let { actions ->
            actions.remove(actionId)
            if (actions.isEmpty()) actionsByKey.remove(keyCode)
        }
        if (keys.isEmpty()) keysByAction.remove(actionId)
        updateEdges(actionId, wasDown)
        return true
    }

    fun unbindPointerButton(actionId: String, buttonCode: Int): Boolean {
        val buttons = pointerButtonsByAction[actionId] ?: return false
        val wasDown = isDownInternal(actionId)
        if (!buttons.remove(buttonCode)) return false

        actionsByPointerButton[buttonCode]?.let { actions ->
            actions.remove(actionId)
            if (actions.isEmpty()) actionsByPointerButton.remove(buttonCode)
        }
        if (buttons.isEmpty()) pointerButtonsByAction.remove(actionId)
        updateEdges(actionId, wasDown)
        return true
    }

    fun unbindAction(actionId: String): Boolean {
        val hadKeys = keysByAction.containsKey(actionId)
        val hadButtons = pointerButtonsByAction.containsKey(actionId)
        if (!hadKeys && !hadButtons) return false

        val wasDown = isDownInternal(actionId)
        keysByAction.remove(actionId)?.forEach { keyCode ->
            actionsByKey[keyCode]?.let { actions ->
                actions.remove(actionId)
                if (actions.isEmpty()) actionsByKey.remove(keyCode)
            }
        }
        pointerButtonsByAction.remove(actionId)?.forEach { buttonCode ->
            actionsByPointerButton[buttonCode]?.let { actions ->
                actions.remove(actionId)
                if (actions.isEmpty()) actionsByPointerButton.remove(buttonCode)
            }
        }
        updateEdges(actionId, wasDown)
        return true
    }

    fun onKey(keyCode: Int, pressed: Boolean) {
        if (keyCode <= 0) return
        val actions = actionsByKey[keyCode].orEmpty().toList()
        val wasDown = actions.associateWith(::isDownInternal)

        if (pressed) downKeys.add(keyCode) else downKeys.remove(keyCode)
        actions.forEach { updateEdges(it, wasDown[it] == true) }
    }

    fun onPointer(event: PointerEvent) {
        val previous = pointerPositions[event.pointerId]
        pointerX = event.x
        pointerY = event.y
        pointerDeltaX = if (previous == null) 0f else event.x - previous.first
        pointerDeltaY = if (previous == null) 0f else event.y - previous.second
        pointerPositions[event.pointerId] = event.x to event.y

        when (event.type) {
            PointerEventType.DOWN -> changePointerButton(event.pointerId, event.button, true)
            PointerEventType.UP -> changePointerButton(event.pointerId, event.button, false)
            PointerEventType.MOVE -> Unit
            PointerEventType.CANCEL -> {
                val buttons = downPointerButtons[event.pointerId]?.toList().orEmpty()
                buttons.forEach { changePointerButton(event.pointerId, it, false) }
                pointerPositions.remove(event.pointerId)
            }
        }
    }

    fun isDown(actionId: String): Boolean = isDownInternal(actionId)

    fun wasPressed(actionId: String): Boolean = actionId in justPressedActions

    fun wasReleased(actionId: String): Boolean = actionId in justReleasedActions

    fun downKeys(): Set<Int> = downKeys.toSet()

    fun downPointerButtons(): Map<Int, Set<Int>> = downPointerButtons.mapValues { it.value.toSet() }

    /** Clears one-frame edges while preserving held keys and pointer buttons. */
    fun endFrame() {
        justPressedActions.clear()
        justReleasedActions.clear()
        pointerDeltaX = 0f
        pointerDeltaY = 0f
    }

    /** Clears all held inputs and one-frame edges, for focus loss or scene reset. */
    fun reset() {
        downKeys.clear()
        downPointerButtons.clear()
        pointerPositions.clear()
        justPressedActions.clear()
        justReleasedActions.clear()
        pointerX = 0f
        pointerY = 0f
        pointerDeltaX = 0f
        pointerDeltaY = 0f
    }

    private fun changePointerButton(pointerId: Int, buttonCode: Int, pressed: Boolean) {
        if (buttonCode <= PointerButtons.NONE) return
        val actions = actionsByPointerButton[buttonCode].orEmpty().toList()
        val wasDown = actions.associateWith(::isDownInternal)
        val buttons = downPointerButtons.getOrPut(pointerId) { linkedSetOf() }
        if (pressed) buttons.add(buttonCode) else buttons.remove(buttonCode)
        if (buttons.isEmpty()) downPointerButtons.remove(pointerId)
        actions.forEach { updateEdges(it, wasDown[it] == true) }
    }

    private fun isDownInternal(actionId: String): Boolean {
        val keyboardDown = keysByAction[actionId].orEmpty().any { it in downKeys }
        if (keyboardDown) return true
        val pointerButtons = pointerButtonsByAction[actionId].orEmpty()
        return downPointerButtons.values.any { held -> held.any { it in pointerButtons } }
    }

    private fun updateEdges(actionId: String, wasDown: Boolean) {
        val isDown = isDownInternal(actionId)
        if (!wasDown && isDown) {
            justPressedActions.add(actionId)
            justReleasedActions.remove(actionId)
        } else if (wasDown && !isDown) {
            justReleasedActions.add(actionId)
            justPressedActions.remove(actionId)
        }
    }
}
