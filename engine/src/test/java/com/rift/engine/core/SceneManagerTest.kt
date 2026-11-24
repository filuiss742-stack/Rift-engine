package com.rift.engine.core

import com.rift.engine.entity.GameObject
import com.rift.engine.entity.GameComponent
import com.rift.engine.input.EngineKey
import com.rift.engine.scene.Scene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun sceneCanRequestItsOwnSafeTransition() {
        val manager = SceneManager()
        var nextEntered = false
        val nextScene = object : Scene() {
            override fun onEnter() {
                nextEntered = true
            }
        }
        val firstScene = object : Scene() {
            override fun update(deltaSeconds: Float) {
                requestSceneChange(nextScene)
                assertSame(this, manager.current)
            }
        }

        manager.setInitialScene(firstScene)
        manager.update(1f / 60f)

        assertTrue(nextEntered)
        assertSame(nextScene, manager.current)
        assertSame(manager.gameState, nextScene.gameState)
    }


    @Test
    fun heldKeyRemainsDownAcrossTransitionWithoutNewPressEdge() {
        val manager = SceneManager()
        lateinit var nextScene: Scene
        val firstScene = object : Scene() {
            init { input.bindKey("switch", EngineKey.TAB) }
            override fun update(deltaSeconds: Float) {
                requestSceneChange(nextScene)
            }
        }
        nextScene = object : Scene() {
            init { input.bindKey("held", EngineKey.TAB) }
        }

        manager.setInitialScene(firstScene)
        manager.onKey(EngineKey.TAB, true)
        manager.update(1f / 60f)

        assertSame(nextScene, manager.current)
        assertTrue(nextScene.input.isDown("held"))
        assertTrue(!nextScene.input.wasPressed("held"))
    }


    @Test
    fun managerDisposesEveryOwnedSceneAndComponentExactlyOnce() {
        val manager = SceneManager()
        var firstExit = 0
        var firstDispose = 0
        var secondExit = 0
        var secondDispose = 0
        var componentDetach = 0
        val firstObject = GameObject(0f, 0f, 10f, 10f).apply {
            addComponent(object : GameComponent {
                override fun onDetach(owner: GameObject) {
                    componentDetach += 1
                }
            })
        }
        val first = object : Scene() {
            init { add(firstObject) }
            override fun onExit() { firstExit += 1 }
            override fun onDispose() { firstDispose += 1 }
        }
        val second = object : Scene() {
            override fun onExit() { secondExit += 1 }
            override fun onDispose() { secondDispose += 1 }
        }

        manager.setInitialScene(first)
        manager.changeScene(second)
        manager.update(1f / 60f)
        manager.close()
        manager.close()

        assertEquals(1, firstExit)
        assertEquals(1, firstDispose)
        assertEquals(1, secondExit)
        assertEquals(1, secondDispose)
        assertEquals(1, componentDetach)
        assertEquals(null, manager.current)
    }


    @Test
    fun managerDisposesScenesThatWereQueuedButNeverActivated() {
        val manager = SceneManager()
        var queuedDispose = 0
        val active = object : Scene() {}
        val queued = object : Scene() {
            override fun onDispose() { queuedDispose += 1 }
        }

        manager.setInitialScene(active)
        manager.changeScene(queued)
        manager.close()

        assertEquals(1, queuedDispose)
        assertEquals(null, manager.current)
    }


    @Test
    fun inactiveScenesCanBeExplicitlyDisposedDuringLongRunningTransitions() {
        val manager = SceneManager()
        var disposedCount = 0
        var current: Scene = object : Scene() {
            override fun onDispose() { disposedCount += 1 }
        }
        manager.setInitialScene(current)

        repeat(100) {
            val previous = current
            current = object : Scene() {
                override fun onDispose() { disposedCount += 1 }
            }
            manager.changeScene(current)
            manager.update(1f / 60f)

            assertTrue(manager.disposeScene(previous))
            assertTrue(previous.isDisposed)
            assertFalse(manager.disposeScene(previous))
        }

        assertEquals(100, disposedCount)
        assertFalse(manager.disposeScene(current))
        manager.close()
        assertEquals(101, disposedCount)
    }

    @Test
    fun focusedUiConsumesActivationKeysBeforeGameplayBindings() {
        val manager = SceneManager()
        var clicks = 0
        val scene = object : Scene() {
            init {
                input.bindKey("jump", EngineKey.SPACE)
                ui.add(com.rift.engine.ui.UiButton(10f, 10f, 80f, 40f, "Play", onClick = {
                    clicks += 1
                }))
            }
        }
        manager.setInitialScene(scene)

        manager.onKey(EngineKey.TAB, true)
        manager.onKey(EngineKey.TAB, false)
        manager.onKey(EngineKey.SPACE, true)

        assertEquals(1, clicks)
        assertFalse(scene.input.isDown("jump"))
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
    fun failingGameObjectIsQuarantinedUntilExplicitRetry() {
        val diagnostics = com.rift.engine.diagnostics.DiagnosticReporter()
        val manager = SceneManager(diagnostics)
        var shouldFail = true
        var attempts = 0
        val broken = object : GameObject(0f, 0f, 10f, 10f) {
            override fun update(deltaSeconds: Float) {
                attempts += 1
                if (shouldFail) error("broken entity")
                super.update(deltaSeconds)
            }
        }
        val healthy = GameObject(0f, 0f, 10f, 10f).apply { velocityX = 10f }
        val scene = object : Scene() {
            init {
                add(broken)
                add(healthy)
            }
        }
        manager.setInitialScene(scene)

        manager.update(1f)
        manager.update(1f)

        assertEquals(1, attempts)
        assertEquals(20f, healthy.x, 0.001f)
        assertTrue(scene.isObjectQuarantined(broken))
        assertEquals("RIFT_RUNTIME_EXCEPTION", diagnostics.latestError()?.code)

        shouldFail = false
        assertTrue(scene.retryObject(broken))
        manager.update(1f)

        assertEquals(2, attempts)
        assertEquals(30f, healthy.x, 0.001f)
        assertTrue(!scene.isObjectQuarantined(broken))
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
