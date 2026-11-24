package com.rift.engine.demo

import com.rift.engine.core.SceneManager
import com.rift.engine.input.EngineKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameplayModeTransitionTest {
    @Test
    fun switchesBetweenTopDownAndPlatformerWhileKeepingSharedProgression() {
        val manager = SceneManager()
        manager.setInitialScene(SandboxGame().createInitialScene())

        manager.onKey(EngineKey.TAB, true)
        manager.update(1f / 60f)

        assertTrue(manager.current is PlatformerDemoScene)
        assertEquals(1, manager.gameState.int("mode-switches"))

        // The held TAB key must not trigger a second transition by itself.
        manager.update(1f / 60f)
        assertTrue(manager.current is PlatformerDemoScene)

        manager.onKey(EngineKey.TAB, false)
        manager.onKey(EngineKey.TAB, true)
        manager.update(1f / 60f)

        assertTrue(manager.current is SandboxScene)
        assertEquals(2, manager.gameState.int("mode-switches"))
    }
}
