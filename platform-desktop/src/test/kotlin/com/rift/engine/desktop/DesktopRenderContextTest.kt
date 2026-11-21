package com.rift.engine.desktop

import java.awt.image.BufferedImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DesktopRenderContextTest {
    @Test
    fun pushAndPopRestoreTransformState() {
        val image = BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            val context = DesktopRenderContext(graphics, image.width, image.height)

            context.pushState()
            context.translate(10f, 10f)
            context.drawRect(0f, 0f, 5f, 5f, 0xFFFF0000.toInt())
            context.popState()
            context.drawRect(20f, 20f, 25f, 25f, 0xFF00FF00.toInt())

            assertEquals(0xFFFF0000.toInt(), image.getRGB(11, 11))
            assertEquals(0xFF00FF00.toInt(), image.getRGB(21, 21))
            assertEquals(0, image.getRGB(1, 1))
        } finally {
            graphics.dispose()
        }
    }

    @Test
    fun clipRectRestrictsDrawingInsidePushedState() {
        val image = BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            val context = DesktopRenderContext(graphics, image.width, image.height)

            context.pushState()
            context.clipRect(0f, 0f, 5f, 5f)
            context.drawRect(0f, 0f, 10f, 10f, 0xFF336699.toInt())
            context.popState()

            assertNotEquals(0, image.getRGB(2, 2))
            assertEquals(0, image.getRGB(7, 7))
        } finally {
            graphics.dispose()
        }
    }


    @Test
    fun sceneCameraTransformsWorldObjectsButNotPostSceneOverlay() {
        val image = BufferedImage(60, 60, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            val context = DesktopRenderContext(graphics, image.width, image.height)
            val scene = object : com.rift.engine.scene.Scene() {
                init {
                    add(
                        object : com.rift.engine.entity.GameObject(
                            20f,
                            20f,
                            5f,
                            5f,
                            0xFFFF0000.toInt()
                        ) {
                            override fun render(context: com.rift.engine.graphics.RenderContext) {
                                context.drawRect(x, y, x + width, y + height, color)
                            }
                        }
                    )
                }

                override fun render(context: com.rift.engine.graphics.RenderContext) {
                    super.render(context)
                    context.drawRect(1f, 1f, 3f, 3f, 0xFF00FF00.toInt())
                }
            }
            scene.camera.x = 10f
            scene.camera.y = 10f
            scene.camera.zoom = 2f

            scene.render(context)

            assertEquals(0xFFFF0000.toInt(), image.getRGB(21, 21))
            assertEquals(0xFF00FF00.toInt(), image.getRGB(2, 2))
            assertEquals(0, image.getRGB(11, 11))
        } finally {
            graphics.dispose()
        }
    }

    @Test(expected = IllegalStateException::class)
    fun popStateRejectsUnbalancedStack() {
        val image = BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            DesktopRenderContext(graphics, image.width, image.height).popState()
        } finally {
            graphics.dispose()
        }
    }
}
