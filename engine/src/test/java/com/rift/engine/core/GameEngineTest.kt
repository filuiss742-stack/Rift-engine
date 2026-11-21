package com.rift.engine.core

import com.rift.engine.diagnostics.DiagnosticSeverity
import com.rift.engine.scene.Scene
import org.junit.Assert.assertEquals
import org.junit.Test

class GameEngineTest {
    @Test
    fun recoverableErrorDoesNotStopSceneUpdates() {
        val engine = GameEngine()
        val scene = CountingScene()
        engine.scenes.setInitialScene(scene)

        engine.update(1f / 60f)
        engine.diagnostics.report(
            DiagnosticSeverity.ERROR,
            "PLUGIN_FAILURE",
            "test",
            "An optional plugin failed."
        )
        engine.update(1f / 60f)

        assertEquals(2, scene.updateCount)
    }

    @Test
    fun fatalDiagnosticStopsSceneUpdates() {
        val engine = GameEngine()
        val scene = CountingScene()
        engine.scenes.setInitialScene(scene)

        engine.update(1f / 60f)
        engine.diagnostics.report(
            DiagnosticSeverity.FATAL,
            "CORE_FAILURE",
            "test",
            "The engine cannot safely continue."
        )
        engine.update(1f / 60f)

        assertEquals(1, scene.updateCount)
    }

    private class CountingScene : Scene() {
        var updateCount = 0
            private set

        override fun update(deltaSeconds: Float) {
            updateCount += 1
        }
    }
}
