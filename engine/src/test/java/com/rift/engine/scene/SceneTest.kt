package com.rift.engine.scene

import com.rift.engine.entity.GameObject
import com.rift.engine.graphics.RenderContext
import org.junit.Assert.assertEquals
import org.junit.Test

class SceneTest {
    @Test
    fun objectChangesDuringUpdateAreAppliedAfterTraversal() {
        val scene = MutatingScene()

        scene.update(1f / 60f)

        assertEquals(1, scene.first.updateCount)
        assertEquals(0, scene.second.updateCount)

        scene.update(1f / 60f)

        assertEquals(1, scene.first.updateCount)
        assertEquals(1, scene.second.updateCount)
    }

    private class MutatingScene : Scene() {
        val second = CountingObject()
        val first = object : GameObject(0f, 0f, 1f, 1f) {
            var hasMutatedScene = false

            override fun update(deltaSeconds: Float) {
                super.update(deltaSeconds)
                updateCount += 1

                if (!hasMutatedScene) {
                    hasMutatedScene = true
                    this@MutatingScene.remove(this)
                    this@MutatingScene.add(second)
                }
            }

            var updateCount = 0
        }

        init {
            add(first)
        }

        override fun render(context: RenderContext) = Unit
    }

    private class CountingObject : GameObject(0f, 0f, 1f, 1f) {
        var updateCount = 0

        override fun update(deltaSeconds: Float) {
            updateCount += 1
        }
    }
}
