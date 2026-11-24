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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class GameEngineTest {


    @Test
    fun closeWaitsForAnInFlightUpdateBeforeDisposingScenes() {
        val engine = GameEngine()
        val updateStarted = CountDownLatch(1)
        val allowUpdateToFinish = CountDownLatch(1)
        val closeStarted = CountDownLatch(1)
        val closeFinished = AtomicBoolean(false)
        val sceneDisposed = AtomicBoolean(false)
        engine.scenes.setInitialScene(object : Scene() {
            override fun update(deltaSeconds: Float) {
                updateStarted.countDown()
                check(allowUpdateToFinish.await(2, TimeUnit.SECONDS))
            }
            override fun onDispose() {
                sceneDisposed.set(true)
            }
        })

        val updateThread = Thread { engine.update(1f / 60f) }
        updateThread.start()
        assertTrue(updateStarted.await(2, TimeUnit.SECONDS))

        val closeThread = Thread {
            closeStarted.countDown()
            engine.close()
            closeFinished.set(true)
        }
        closeThread.start()

        assertTrue(closeStarted.await(2, TimeUnit.SECONDS))
        assertFalse(closeFinished.await(100, TimeUnit.MILLISECONDS))
        assertFalse(sceneDisposed.get())

        allowUpdateToFinish.countDown()
        updateThread.join(2000)
        closeThread.join(2000)

        assertFalse(updateThread.isAlive)
        assertFalse(closeThread.isAlive)
        assertTrue(closeFinished.get())
        assertTrue(sceneDisposed.get())
    }

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
    fun queuedQuickTapReachesSceneAsPressedAndReleasedInOneUpdate() {
        val engine = GameEngine()
        var pressedDuringUpdate = false
        var releasedDuringUpdate = false
        val scene = object : Scene() {
            init { input.bindKey("attack", 7) }
            override fun update(deltaSeconds: Float) {
                pressedDuringUpdate = input.wasPressed("attack")
                releasedDuringUpdate = input.wasReleased("attack")
            }
        }
        engine.scenes.setInitialScene(scene)

        engine.onKey(7, true)
        engine.onKey(7, false)
        engine.update(1f / 60f)

        assertTrue(pressedDuringUpdate)
        assertTrue(releasedDuringUpdate)
        assertFalse(scene.input.isDown("attack"))
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
        val cleanupOrder = mutableListOf<String>()
        engine.scenes.setInitialScene(object : Scene() {
            override fun onDispose() { cleanupOrder += "scene" }
        })

        engine.plugins.install(object : EnginePlugin {
            override val id = "test.shutdown-plugin"
            override fun onUninstall() { cleanupOrder += "plugin" }
        })
        assertTrue(engine.gameplayModules.install(object : GameplayModule {
            override val descriptor = GameplayModuleDescriptor("test.shutdown-module", "1.0.0")
            override fun onInstall(context: GameplayModuleContext) = Unit
            override fun onUninstall() { cleanupOrder += "module" }
        }))

        engine.close()
        engine.close()

        assertEquals(listOf("scene", "module", "plugin"), cleanupOrder)
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
