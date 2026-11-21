package com.rift.engine.core

import com.rift.engine.input.EngineKey
import com.rift.engine.scene.Scene
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneInputActionTest {
    @Test
    fun sceneReceivesNamedActionStateAndOneFrameEdges() {
        val manager = SceneManager()
        var heldDuringFirstUpdate = false
        var pressedDuringFirstUpdate = false
        var pressedDuringSecondUpdate = true
        var releasedDuringThirdUpdate = false
        val scene = object : Scene() {
            init {
                input.bindKey("move.left", EngineKey.A)
            }

            private var updateCount = 0

            override fun update(deltaSeconds: Float) {
                updateCount += 1
                when (updateCount) {
                    1 -> {
                        heldDuringFirstUpdate = input.isDown("move.left")
                        pressedDuringFirstUpdate = input.wasPressed("move.left")
                    }
                    2 -> pressedDuringSecondUpdate = input.wasPressed("move.left")
                    3 -> releasedDuringThirdUpdate = input.wasReleased("move.left")
                }
            }
        }

        manager.setInitialScene(scene)
        manager.onKey(EngineKey.A, true)
        manager.update(1f / 60f)
        manager.update(1f / 60f)
        manager.onKey(EngineKey.A, false)
        manager.update(1f / 60f)

        assertTrue(heldDuringFirstUpdate)
        assertTrue(pressedDuringFirstUpdate)
        assertFalse(pressedDuringSecondUpdate)
        assertTrue(releasedDuringThirdUpdate)
    }

    @Test
    fun heldKeysAreSynchronizedToNewSceneAfterTransition() {
        val manager = SceneManager()
        var newSceneSawHeldKey = false
        var newSceneSawPressEdge = false
        val nextScene = object : Scene() {
            init {
                input.bindKey("jump", EngineKey.SPACE)
            }

            override fun update(deltaSeconds: Float) {
                newSceneSawHeldKey = input.isDown("jump")
                newSceneSawPressEdge = input.wasPressed("jump")
            }
        }
        val firstScene = object : Scene() {}

        manager.setInitialScene(firstScene)
        manager.onKey(EngineKey.SPACE, true)
        manager.changeScene(nextScene)
        manager.update(1f / 60f)

        assertTrue(newSceneSawHeldKey)
        assertTrue(newSceneSawPressEdge)
    }
}
