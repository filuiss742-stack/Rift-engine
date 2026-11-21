package com.rift.engine.demo

import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SandboxGameTest {
    @Test
    fun createsANewInitialSceneForEachGameSession() {
        val game = SandboxGame()

        val firstScene = game.createInitialScene()
        val secondScene = game.createInitialScene()

        assertTrue(firstScene is SandboxScene)
        assertNotSame(firstScene, secondScene)
    }
}
