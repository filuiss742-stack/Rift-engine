package com.rift.engine.animation

import com.rift.engine.entity.GameComponent
import com.rift.engine.entity.GameObject
import com.rift.engine.graphics.RenderContext
import com.rift.engine.graphics.Sprite

data class AnimationFrame(
    val sprite: Sprite,
    val durationSeconds: Float
) {
    init {
        require(durationSeconds.isFinite() && durationSeconds > 0f) {
            "Animation frame duration must be finite and greater than zero."
        }
    }
}

/** Time-based sprite animation data, independent of a particular entity. */
class SpriteAnimation(
    frames: List<AnimationFrame>,
    val looping: Boolean = true
) {
    val frames: List<AnimationFrame> = frames.toList()
    val durationSeconds: Float = this.frames.sumOf { it.durationSeconds.toDouble() }.toFloat()

    init {
        require(this.frames.isNotEmpty()) { "Sprite animation must contain at least one frame." }
        require(durationSeconds.isFinite() && durationSeconds > 0f) {
            "Total animation duration must be finite and greater than zero."
        }
    }
}

/** Plays named sprite animations as a reusable GameObject component. */
class SpriteAnimationComponent(
    animations: Map<String, SpriteAnimation>,
    initialAnimationId: String
) : GameComponent {
    private val animations = animations.toMap()
    var currentAnimationId: String = initialAnimationId
        private set
    var isFinished: Boolean = false
        private set

    private var frameIndex = 0
    private var frameElapsed = 0f

    init {
        require(this.animations.isNotEmpty()) { "At least one animation is required." }
        require(initialAnimationId in this.animations) {
            "Initial animation '$initialAnimationId' is not registered."
        }
    }

    val currentSprite: Sprite
        get() = animations.getValue(currentAnimationId).frames[frameIndex].sprite

    fun play(animationId: String, restart: Boolean = true) {
        require(animationId in animations) { "Animation '$animationId' is not registered." }
        if (!restart && currentAnimationId == animationId && !isFinished) return
        currentAnimationId = animationId
        frameIndex = 0
        frameElapsed = 0f
        isFinished = false
    }

    override fun update(owner: GameObject, deltaSeconds: Float) {
        require(deltaSeconds.isFinite() && deltaSeconds >= 0f) {
            "deltaSeconds must be finite and non-negative."
        }
        if (isFinished || deltaSeconds == 0f) return

        val animation = animations.getValue(currentAnimationId)
        frameElapsed += deltaSeconds
        var advances = 0
        while (frameElapsed >= animation.frames[frameIndex].durationSeconds) {
            frameElapsed -= animation.frames[frameIndex].durationSeconds
            if (frameIndex + 1 < animation.frames.size) {
                frameIndex += 1
            } else if (animation.looping) {
                frameIndex = 0
            } else {
                frameIndex = animation.frames.lastIndex
                frameElapsed = 0f
                isFinished = true
                break
            }
            advances += 1
            check(advances <= 10000) {
                "Animation advanced too many frames in one update; reduce deltaSeconds or frame durations."
            }
        }
    }

    override fun render(owner: GameObject, context: RenderContext) {
        currentSprite.draw(context, 0f, 0f, owner.width, owner.height)
    }
}
