package com.rift.engine.input

/**
 * Maps device-independent key codes to named gameplay actions.
 *
 * The map is intended to be owned and updated by the game loop thread.
 * Platform adapters forward physical key events; game code reads action state.
 */
class InputActionMap {
    private val keysByAction = linkedMapOf<String, MutableSet<Int>>()
    private val actionsByKey = linkedMapOf<Int, MutableSet<String>>()
    private val downKeys = mutableSetOf<Int>()
    private val justPressedActions = mutableSetOf<String>()
    private val justReleasedActions = mutableSetOf<String>()

    fun bindKey(actionId: String, keyCode: Int) {
        require(actionId.isNotBlank()) { "actionId must not be blank." }
        require(keyCode > 0) { "keyCode must be greater than zero." }

        val keys = keysByAction.getOrPut(actionId) { linkedSetOf() }
        if (!keys.add(keyCode)) return
        actionsByKey.getOrPut(keyCode) { linkedSetOf() }.add(actionId)

        if (keyCode in downKeys) {
            justPressedActions.add(actionId)
            justReleasedActions.remove(actionId)
        }
    }

    fun unbindKey(actionId: String, keyCode: Int): Boolean {
        val keys = keysByAction[actionId] ?: return false
        if (!keys.remove(keyCode)) return false

        actionsByKey[keyCode]?.let { actions ->
            actions.remove(actionId)
            if (actions.isEmpty()) actionsByKey.remove(keyCode)
        }
        if (keys.isEmpty()) keysByAction.remove(actionId)

        if (!isDown(actionId)) {
            justPressedActions.remove(actionId)
            if (keyCode in downKeys) {
                justReleasedActions.add(actionId)
            }
        }
        return true
    }

    fun unbindAction(actionId: String): Boolean {
        val keys = keysByAction.remove(actionId) ?: return false
        keys.forEach { keyCode ->
            actionsByKey[keyCode]?.let { actions ->
                actions.remove(actionId)
                if (actions.isEmpty()) actionsByKey.remove(keyCode)
            }
        }
        justPressedActions.remove(actionId)
        if (keys.any { it in downKeys }) {
            justReleasedActions.add(actionId)
        }
        return true
    }

    fun onKey(keyCode: Int, pressed: Boolean) {
        if (keyCode <= 0) return

        val actions = actionsByKey[keyCode].orEmpty()
        if (pressed) {
            if (!downKeys.add(keyCode)) return
            actions.forEach { actionId ->
                val otherBoundKeyDown = keysByAction[actionId]
                    .orEmpty()
                    .any { it != keyCode && it in downKeys }
                if (!otherBoundKeyDown) {
                    justPressedActions.add(actionId)
                    justReleasedActions.remove(actionId)
                }
            }
        } else {
            if (!downKeys.remove(keyCode)) return
            actions.forEach { actionId ->
                val anotherBoundKeyDown = keysByAction[actionId]
                    .orEmpty()
                    .any { it in downKeys }
                if (!anotherBoundKeyDown) {
                    justReleasedActions.add(actionId)
                }
            }
        }
    }

    fun isDown(actionId: String): Boolean {
        return keysByAction[actionId].orEmpty().any { it in downKeys }
    }

    fun wasPressed(actionId: String): Boolean = actionId in justPressedActions

    fun wasReleased(actionId: String): Boolean = actionId in justReleasedActions

    fun downKeys(): Set<Int> = downKeys.toSet()

    /** Clears one-frame edges while preserving held keys. */
    fun endFrame() {
        justPressedActions.clear()
        justReleasedActions.clear()
    }

    /** Clears all held keys and one-frame edges. */
    fun reset() {
        downKeys.clear()
        justPressedActions.clear()
        justReleasedActions.clear()
    }
}
