package com.rift.engine.scene

import com.rift.engine.entity.GameObject
import org.junit.Assert.assertEquals
import org.junit.Test

class SceneTest {

    @Test
    fun rendersObjectsByLayerAndPreservesInsertionOrderWithinLayer() {
        val order = mutableListOf<String>()
        val scene = object : Scene() {
            init {
                add(OrderedObject("front", 5, order))
                add(OrderedObject("back-a", -2, order))
                add(OrderedObject("back-b", -2, order))
                add(OrderedObject("middle", 1, order))
            }
        }

        scene.render(RecordingRenderContext())

        assertEquals(listOf("back-a", "back-b", "middle", "front"), order)
    }

    private class OrderedObject(
        private val name: String,
        layer: Int,
        private val order: MutableList<String>
    ) : GameObject(0f, 0f, 1f, 1f) {
        init { renderLayer = layer }
        override fun render(context: com.rift.engine.graphics.RenderContext) {
            order += name
        }
    }

    private class RecordingRenderContext : com.rift.engine.graphics.RenderContext {
        override val width = 320
        override val height = 200
        override fun clear(color: Int) = Unit
        override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, color: Int) = Unit
        override fun drawRoundRect(left: Float, top: Float, right: Float, bottom: Float, radius: Float, color: Int) = Unit
        override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, color: Int, strokeWidth: Float) = Unit
        override fun drawText(text: String, x: Float, y: Float, color: Int, size: Float, bold: Boolean) = Unit
        override fun measureText(text: String, size: Float, bold: Boolean): Float = text.length * size
        override fun pushState() = Unit
        override fun popState() = Unit
        override fun translate(x: Float, y: Float) = Unit
        override fun rotate(degrees: Float) = Unit
        override fun scale(x: Float, y: Float) = Unit
        override fun clipRect(left: Float, top: Float, right: Float, bottom: Float) = Unit
    }

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
