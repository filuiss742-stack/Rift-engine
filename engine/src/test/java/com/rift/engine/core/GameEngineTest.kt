package com.rift.engine.core

import com.rift.engine.diagnostics.DiagnosticSeverity
import com.rift.engine.module.GameplayModule
import com.rift.engine.module.GameplayModuleContext
import com.rift.engine.module.GameplayModuleDescriptor
import com.rift.engine.plugin.EnginePlugin
import com.rift.engine.scene.Scene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    @Test
    fun queuesAnalogAxesForTheGameLoopThread() {
        val engine = GameEngine()
        var receivedAxis = 0f
        val scene = object : Scene() {
            override fun onAxis(axisId: String, value: Float) {
                if (axisId == "gamepad.left.x") receivedAxis = value
            }
        }
        engine.scenes.setInitialScene(scene)

        engine.onAxis("gamepad.left.x", 0.75f)
        assertEquals(0f, receivedAxis, 0.001f)
        engine.update(1f / 60f)

        assertEquals(0.75f, receivedAxis, 0.001f)
        engine.close()
    }

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


    @Test
    fun closeCleansPluginsAndGameplayModulesExactlyOnce() {
        val engine = GameEngine()
        var pluginCleanupCount = 0
        var moduleCleanupCount = 0

        engine.plugins.install(object : EnginePlugin {
            override val id = "test.shutdown-plugin"
            override fun onUninstall() { pluginCleanupCount += 1 }
        })
        assertTrue(engine.gameplayModules.install(object : GameplayModule {
            override val descriptor = GameplayModuleDescriptor("test.shutdown-module", "1.0.0")
            override fun onInstall(context: GameplayModuleContext) = Unit
            override fun onUninstall() { moduleCleanupCount += 1 }
        }))

        engine.close()
        engine.close()

        assertEquals(1, pluginCleanupCount)
        assertEquals(1, moduleCleanupCount)
        assertTrue(engine.plugins.all().isEmpty())
        assertTrue(engine.gameplayModules.installed().isEmpty())
    }

    @Test
    fun savesAndLoadsTypedSharedGameState() {
        val engine = GameEngine()
        engine.scenes.gameState.putInt("coins", 75)
        engine.scenes.gameState.putBoolean("quest.done", true)
        engine.scenes.gameState.putString("chapter", "citadel")

        assertTrue(engine.saveGameState("slot-1"))
        engine.scenes.gameState.clear()
        assertTrue(engine.loadGameState("slot-1"))

        assertEquals(75, engine.scenes.gameState.int("coins"))
        assertTrue(engine.scenes.gameState.boolean("quest.done"))
        assertEquals("citadel", engine.scenes.gameState.string("chapter"))
        engine.close()
    }

    @Test
    fun invalidSaveDoesNotPartiallyReplaceCurrentState() {
        val engine = GameEngine()
        engine.scenes.gameState.putInt("coins", 12)
        engine.saves.putString("rift.game-state.broken", "not-a-rift-save")

        assertFalse(engine.loadGameState("broken"))
        assertEquals(12, engine.scenes.gameState.int("coins"))
        assertTrue(engine.diagnostics.snapshot().any { it.code == "RIFT_SAVE_STATE_INVALID" })
        engine.close()
    }

    private class CountingScene : Scene() {
        var updateCount = 0
            private set

        override fun update(deltaSeconds: Float) {
            updateCount += 1
        }
    }
}
