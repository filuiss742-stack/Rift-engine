package com.rift.engine.core

import com.rift.engine.scene.Scene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneManagerTest {
    @Test
    fun transitionRequestedDuringUpdateRunsAfterCurrentUpdateReturns() {
        val manager = SceneManager()
        var oldUpdateFinished = false
        var nextSceneEntered = false
        val nextScene = object : Scene() {
            override fun onEnter() {
                nextSceneEntered = oldUpdateFinished
            }
        }
        val firstScene = object : Scene() {
            override fun update(deltaSeconds: Float) {
                manager.changeScene(nextScene)
                assertSame(this, manager.current)
                oldUpdateFinished = true
            }
        }

        manager.setInitialScene(firstScene)
        manager.update(1f / 60f)

        assertTrue(oldUpdateFinished)
        assertTrue(nextSceneEntered)
        assertSame(nextScene, manager.current)
    }

    @Test
    fun transitionRequestedDuringOnEnterIsAppliedInOrder() {
        val manager = SceneManager()
        val entered = mutableListOf<String>()
        val finalScene = object : Scene() {
            override fun onEnter() {
                entered += "final"
            }
        }
        val firstScene = object : Scene() {
            override fun onEnter() {
                entered += "first"
                manager.changeScene(finalScene)
            }

            override fun onExit() {
                entered += "first-exit"
            }
        }

        manager.setInitialScene(firstScene)
        manager.update(1f / 60f)

        assertEquals(listOf("first", "first-exit", "final"), entered)
        assertSame(finalScene, manager.current)
    }

    @Test
    fun sameSceneRequestDoesNotRepeatLifecycle() {
        val manager = SceneManager()
        var enterCount = 0
        var exitCount = 0
        val scene = object : Scene() {
            override fun onEnter() {
                enterCount += 1
            }

            override fun onExit() {
                exitCount += 1
            }
        }

        manager.setInitialScene(scene)
        manager.changeScene(scene)
        manager.update(1f / 60f)

        assertEquals(1, enterCount)
        assertEquals(0, exitCount)
    }
}
