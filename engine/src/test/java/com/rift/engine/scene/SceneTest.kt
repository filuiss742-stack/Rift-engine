package com.rift.engine.scene

import com.rift.engine.entity.GameObject
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
        val first = MutatingObject(this, second)

        init {
            add(first)
        }

        fun addToScene(gameObject: GameObject) {
            add(gameObject)
        }

        fun removeFromScene(gameObject: GameObject) {
            remove(gameObject)
        }
    }

    private class MutatingObject(
        private val scene: MutatingScene,
        private val replacement: CountingObject
    ) : GameObject(0f, 0f, 1f, 1f) {
        var updateCount = 0
        private var hasChangedScene = false

        override fun update(deltaSeconds: Float) {
            updateCount += 1

            if (!hasChangedScene) {
                hasChangedScene = true
                scene.removeFromScene(this)
                scene.addToScene(replacement)
            }
        }
    }

    private class CountingObject : GameObject(0f, 0f, 1f, 1f) {
        var updateCount = 0

        override fun update(deltaSeconds: Float) {
            updateCount += 1
        }
    }
}
