package com.rift.engine.core

import org.junit.Assert.assertEquals
import org.junit.Test

class FixedStepAccumulatorTest {
    @Test
    fun accumulatesPartialFramesIntoFixedUpdates() {
        val clock = FixedStepAccumulator(stepSeconds = 0.1f)
        var updates = 0

        clock.advance(0.04f) { updates += 1 }
        assertEquals(0, updates)
        clock.advance(0.06f) { updates += 1 }

        assertEquals(1, updates)
    }

    @Test
    fun limitsCatchUpWorkAndRecordsDroppedTime() {
        val clock = FixedStepAccumulator(
            stepSeconds = 0.1f,
            maxFrameDeltaSeconds = 1f,
            maxStepsPerFrame = 2
        )
        var updates = 0

        clock.advance(0.55f) { updates += 1 }

        assertEquals(2, updates)
        assertEquals(true, clock.droppedTimeSeconds > 0f)
    }

    @Test
    fun ignoresNegativeAndNonFiniteFrameDeltas() {
        val clock = FixedStepAccumulator()
        var updates = 0

        clock.advance(-1f) { updates += 1 }
        clock.advance(Float.NaN) { updates += 1 }

        assertEquals(0, updates)
    }
}
