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
    fun pointerEventsReachSceneAndNamedPointerActions() {
        val manager = SceneManager()
        var receivedType: com.rift.engine.input.PointerEventType? = null
        val scene = object : Scene() {
            init {
                input.bindPointerButton(
                    "interact",
                    com.rift.engine.input.PointerButtons.TOUCH
                )
            }

            override fun onPointer(event: com.rift.engine.input.PointerEvent) {
                receivedType = event.type
            }
        }
        manager.setInitialScene(scene)

        manager.onPointer(
            com.rift.engine.input.PointerEvent(
                pointerId = 4,
                type = com.rift.engine.input.PointerEventType.DOWN,
                x = 12f,
                y = 24f,
                viewportWidth = 100f,
                viewportHeight = 100f,
                button = com.rift.engine.input.PointerButtons.TOUCH
            )
        )

        assertEquals(com.rift.engine.input.PointerEventType.DOWN, receivedType)
        assertTrue(scene.input.isDown("interact"))
        assertTrue(scene.input.wasPressed("interact"))
    }

    @Test
    fun focusLossClearsHeldInput() {
        val manager = SceneManager()
        val scene = object : Scene() {
            init {
                input.bindKey("move", 1)
            }
        }
        manager.setInitialScene(scene)
        manager.onKey(1, true)
        assertTrue(scene.input.isDown("move"))

        manager.resetInput()

        assertTrue(!scene.input.isDown("move"))
    }




    @Test
    fun failingGameObjectDoesNotPreventOtherObjectsFromUpdating() {
        val diagnostics = com.rift.engine.diagnostics.DiagnosticReporter()
        val manager = SceneManager(diagnostics)
        val healthy = GameObject(0f, 0f, 10f, 10f).apply { velocityX = 10f }
        val broken = object : GameObject(0f, 0f, 10f, 10f) {
            override fun update(deltaSeconds: Float) {
                error("broken entity")
            }
        }
        val scene = object : Scene() {
            init {
                add(broken)
                add(healthy)
            }
        }
        manager.setInitialScene(scene)

        manager.update(1f)

        assertEquals(10f, healthy.x, 0.001f)
        assertEquals("RIFT_RUNTIME_EXCEPTION", diagnostics.latestError()?.code)
    }

    @Test
    fun sceneUiConsumesButtonPointerBeforeGameplayCallback() {
        val manager = SceneManager()
        var clicks = 0
        var gameplayPointerEvents = 0
        val scene = object : Scene() {
            init {
                ui.add(
                    com.rift.engine.ui.UiButton(
                        10f,
                        10f,
                        80f,
                        40f,
                        "Open",
                        { clicks += 1 }
                    )
                )
            }

            override fun onPointer(event: com.rift.engine.input.PointerEvent) {
                gameplayPointerEvents += 1
            }
        }
        manager.setInitialScene(scene)

        manager.onPointer(
            com.rift.engine.input.PointerEvent(
                0,
                com.rift.engine.input.PointerEventType.DOWN,
                20f,
                20f,
                100f,
                100f,
                com.rift.engine.input.PointerButtons.TOUCH
            )
        )
        manager.onPointer(
            com.rift.engine.input.PointerEvent(
                0,
                com.rift.engine.input.PointerEventType.UP,
                20f,
                20f,
                100f,
                100f,
                com.rift.engine.input.PointerButtons.TOUCH
            )
        )

        assertEquals(1, clicks)
        assertEquals(0, gameplayPointerEvents)
    }

    @Test
    fun gameStateIsSharedAcrossSceneTransitions() {
        val manager = SceneManager()
        val first = object : Scene() {
            override fun onEnter() {
                gameState.putInt("coins", 7)
            }
        }
        val second = object : Scene() {
            override fun onEnter() {
                assertEquals(7, gameState.int("coins"))
                gameState.putInt("coins", 9)
            }
        }

        manager.setInitialScene(first)
        manager.changeScene(second)
        manager.update(1f / 60f)

        assertSame(manager.gameState, second.gameState)
        assertEquals(9, manager.gameState.int("coins"))
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
