package com.rift.engine.graphics

import com.rift.engine.entity.GameObject
import com.rift.engine.physics.Aabb
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sign

/**
 * Drives a [Camera]: smooth follow, dead zone, world bounds and shake.
 *
 * Viewport dimensions are screen-space logical pixels. Call [update] after
 * scene physics has stepped so the camera follows the resolved world position.
 * Smoothing is frame-rate independent: 0 snaps instantly; larger values follow
 * more tightly. Shake may temporarily move the view outside world bounds.
 */
class CameraRig(
    private val camera: Camera,
    private val viewportWidth: Float,
    private val viewportHeight: Float,
    val smoothing: Float = 8f,
    val deadZoneWidth: Float = 0f,
    val deadZoneHeight: Float = 0f,
    var worldBounds: Aabb? = null,
    shakeSeed: Long = 1L
) {
    private val random = kotlin.random.Random(shakeSeed)
    private var shakeAmplitude = 0f
    private var shakeDuration = 0f
    private var shakeRemaining = 0f
    private var appliedX = 0f
    private var appliedY = 0f

    init {
        require(viewportWidth.isFinite() && viewportWidth > 0f) {
            "viewportWidth must be finite and positive."
        }
        require(viewportHeight.isFinite() && viewportHeight > 0f) {
            "viewportHeight must be finite and positive."
        }
        require(smoothing.isFinite() && smoothing >= 0f) {
            "smoothing must be finite and non-negative."
        }
        require(deadZoneWidth.isFinite() && deadZoneWidth >= 0f) {
            "deadZoneWidth must be finite and non-negative."
        }
        require(deadZoneHeight.isFinite() && deadZoneHeight >= 0f) {
            "deadZoneHeight must be finite and non-negative."
        }
    }

    /** Starts a shake, or replaces the current one only when the new one is stronger. */
    fun shake(amplitude: Float, durationSeconds: Float) {
        require(amplitude.isFinite() && amplitude >= 0f) {
            "amplitude must be finite and non-negative."
        }
        require(durationSeconds.isFinite() && durationSeconds > 0f) {
            "durationSeconds must be finite and positive."
        }
        val currentStrength = if (shakeDuration > 0f) {
            shakeAmplitude * (shakeRemaining / shakeDuration)
        } else {
            0f
        }
        if (amplitude >= currentStrength) {
            shakeAmplitude = amplitude
            shakeDuration = durationSeconds
            shakeRemaining = durationSeconds
        }
    }

    /** Centers the camera on [target] immediately, useful when entering or teleporting. */
    fun snapTo(target: GameObject) {
        removeShake()
        val (nextX, nextY) = clampToBounds(
            target.x + target.width / 2f - visibleWidth() / 2f,
            target.y + target.height / 2f - visibleHeight() / 2f
        )
        camera.x = nextX
        camera.y = nextY
    }

    fun update(target: GameObject, deltaSeconds: Float) {
        require(deltaSeconds.isFinite() && deltaSeconds >= 0f) {
            "deltaSeconds must be finite and non-negative."
        }
        removeShake()

        val cameraCenterX = camera.x + visibleWidth() / 2f
        val cameraCenterY = camera.y + visibleHeight() / 2f
        val targetCenterX = target.x + target.width / 2f
        val targetCenterY = target.y + target.height / 2f
        val dx = outsideDeadZone(targetCenterX - cameraCenterX, deadZoneWidth / 2f)
        val dy = outsideDeadZone(targetCenterY - cameraCenterY, deadZoneHeight / 2f)
        val alpha = if (smoothing == 0f) {
            1f
        } else {
            (1f - exp(-smoothing * deltaSeconds)).coerceIn(0f, 1f)
        }

        val (nextX, nextY) = clampToBounds(camera.x + dx * alpha, camera.y + dy * alpha)
        camera.x = nextX
        camera.y = nextY
        applyShake(deltaSeconds)
    }

    private fun visibleWidth(): Float = viewportWidth / camera.zoom

    private fun visibleHeight(): Float = viewportHeight / camera.zoom

    private fun outsideDeadZone(delta: Float, halfSize: Float): Float =
        if (abs(delta) <= halfSize) 0f else delta - sign(delta) * halfSize

    private fun clampToBounds(x: Float, y: Float): Pair<Float, Float> {
        val bounds = worldBounds ?: return x to y
        return clampAxis(x, bounds.left, bounds.right, visibleWidth()) to
            clampAxis(y, bounds.top, bounds.bottom, visibleHeight())
    }

    private fun clampAxis(value: Float, minimum: Float, maximum: Float, viewSize: Float): Float {
        val span = maximum - minimum
        return if (span <= viewSize) {
            minimum + (span - viewSize) / 2f
        } else {
            value.coerceIn(minimum, maximum - viewSize)
        }
    }

    private fun removeShake() {
        camera.x -= appliedX
        camera.y -= appliedY
        appliedX = 0f
        appliedY = 0f
    }

    private fun applyShake(deltaSeconds: Float) {
        if (shakeRemaining <= 0f) return
        val strength = shakeAmplitude * (shakeRemaining / shakeDuration)
        appliedX = (random.nextFloat() * 2f - 1f) * strength
        appliedY = (random.nextFloat() * 2f - 1f) * strength
        camera.x += appliedX
        camera.y += appliedY
        shakeRemaining = max(0f, shakeRemaining - deltaSeconds)
    }
}
