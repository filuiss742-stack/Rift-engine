package com.rift.engine.graphics3d

/**
 * Runtime action arbitration for a directional sprite library.
 *
 * Higher-priority one-shot actions (hit, dodge, attack) can interrupt lower
 * priority movement. A lower-priority request cannot cancel an unfinished
 * one-shot. When a one-shot ends, the controller returns to the configured
 * fallback action and resets its clock, avoiding animation-time carry-over.
 */
class DirectionalSpriteActionController(
    private val library: DirectionalSpriteLibrary,
    private val fallbackAction: String = library.defaultAction
) {
    private var elapsed = 0f
    private var priority = Int.MIN_VALUE
    private var requestedAction = fallbackAction
    private var selection = library.resolve(fallbackAction)

    init {
        require(fallbackAction in library.clipsByAction) {
            "Controller fallback action must exist in the sprite library."
        }
    }

    val currentAction: String get() = selection.action
    val currentAnimation: DirectionalSpriteAnimation get() = selection.animation
    val elapsedSeconds: Float get() = elapsed
    val usedFallback: Boolean get() = selection.usedFallback
    val isFinished: Boolean get() = currentAnimation.isFinished(elapsedSeconds)

    /**
     * Requests an action. Returns false when a higher-priority one-shot owns
     * the character or when the request is already active.
     */
    fun play(action: String, actionPriority: Int = 0, restart: Boolean = false): Boolean {
        if (!restart && action == requestedAction && !isFinished) return false
        if (!restart && !isFinished && !currentAnimation.loop && actionPriority < priority) return false
        val resolved = library.resolve(action)
        if (!restart && resolved.action == selection.action && !isFinished) {
            requestedAction = action
            return false
        }
        requestedAction = action
        selection = resolved
        priority = actionPriority
        elapsed = 0f
        return true
    }

    /** Advances clip time and returns to fallback after a one-shot completes. */
    fun update(deltaSeconds: Float) {
        require(deltaSeconds.isFinite() && deltaSeconds >= 0f) {
            "Sprite action delta must be finite and non-negative."
        }
        elapsed += deltaSeconds
        if (!currentAnimation.loop && currentAnimation.isFinished(elapsed)) {
            requestedAction = fallbackAction
            selection = library.resolve(fallbackAction)
            priority = Int.MIN_VALUE
            elapsed = 0f
        }
    }

    fun reset() {
        requestedAction = fallbackAction
        selection = library.resolve(fallbackAction)
        priority = Int.MIN_VALUE
        elapsed = 0f
    }
}
