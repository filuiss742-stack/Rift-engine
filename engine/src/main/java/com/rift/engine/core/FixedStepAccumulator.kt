package com.rift.engine.core

/**
 * Runs game logic at a stable fixed interval while rendering may run at a
 * variable rate. Excess backlog is dropped after [maxStepsPerFrame] updates
 * to prevent a spiral of death on slow devices.
 */
class FixedStepAccumulator(
    val stepSeconds: Float = 1f / 60f,
    private val maxFrameDeltaSeconds: Float = 0.25f,
    private val maxStepsPerFrame: Int = 5
) {
    private var accumulatedSeconds: Double = 0.0

    /** Total time intentionally discarded by frame clamping or backlog limiting. */
    var droppedTimeSeconds: Float = 0f
        private set

    init {
        require(stepSeconds.isFinite() && stepSeconds > 0f) {
            "stepSeconds must be a finite value greater than zero."
        }
        require(maxFrameDeltaSeconds.isFinite() && maxFrameDeltaSeconds > 0f) {
            "maxFrameDeltaSeconds must be a finite value greater than zero."
        }
        require(maxStepsPerFrame > 0) {
            "maxStepsPerFrame must be greater than zero."
        }
    }

    /**
     * Accumulates one rendered frame and calls [onStep] zero or more times
     * with exactly [stepSeconds]. Returns the interpolation fraction remaining
     * toward the next update (0..1); rendering interpolation is not automatic.
     */
    fun advance(frameDeltaSeconds: Float, onStep: (Float) -> Unit): Float {
        if (!frameDeltaSeconds.isFinite() || frameDeltaSeconds <= 0f) {
            return interpolationAlpha()
        }

        val acceptedDelta = frameDeltaSeconds.coerceAtMost(maxFrameDeltaSeconds)
        droppedTimeSeconds += frameDeltaSeconds - acceptedDelta
        accumulatedSeconds += acceptedDelta.toDouble()

        var steps = 0
        val fixedStep = stepSeconds.toDouble()
        while (accumulatedSeconds + 0.0000001 >= fixedStep && steps < maxStepsPerFrame) {
            onStep(stepSeconds)
            accumulatedSeconds = (accumulatedSeconds - fixedStep).coerceAtLeast(0.0)
            steps += 1
        }

        if (steps == maxStepsPerFrame && accumulatedSeconds >= fixedStep) {
            val remainder = accumulatedSeconds % fixedStep
            droppedTimeSeconds += (accumulatedSeconds - remainder).toFloat()
            accumulatedSeconds = remainder
        }

        return interpolationAlpha()
    }

    fun reset() {
        accumulatedSeconds = 0.0
        droppedTimeSeconds = 0f
    }

    private fun interpolationAlpha(): Float =
        (accumulatedSeconds / stepSeconds.toDouble()).toFloat().coerceIn(0f, 1f)
}
