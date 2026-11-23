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


    @Test
    fun drawsTextureAndSourceRegionAtLogicalCoordinates() {
        val source = BufferedImage(4, 2, BufferedImage.TYPE_INT_ARGB)
        for (x in 0 until 2) {
            for (y in 0 until 2) source.setRGB(x, y, 0xFFFF0000.toInt())
        }
        for (x in 2 until 4) {
            for (y in 0 until 2) source.setRGB(x, y, 0xFF00FF00.toInt())
        }
        val texture = DesktopTexture(source)
        val image = BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            val context = DesktopRenderContext(graphics, image.width, image.height)
            context.drawTextureRegion(texture, 2, 0, 4, 2, 10f, 2f, 18f, 6f)

            assertEquals(0xFF00FF00.toInt(), image.getRGB(11, 3))
            assertEquals(0, image.getRGB(9, 3))
            assertEquals(0, image.getRGB(18, 3))
        } finally {
            graphics.dispose()
            texture.close()
        }
    }

    @Test
    fun texturedTriangleMapsUvCoordinatesAndClipsOutsidePixels() {
        val source = BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB)
        for (x in 0 until source.width) {
            for (y in 0 until source.height) source.setRGB(x, y, 0xFFCC3366.toInt())
        }
        val texture = DesktopTexture(source)
        val image = BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            val context = DesktopRenderContext(graphics, image.width, image.height)
            context.drawTexturedTriangle(
                texture,
                2f, 2f, 0f, 0f,
                20f, 2f, 1f, 0f,
                2f, 20f, 0f, 1f
            )

            assertEquals(0xFFCC3366.toInt(), image.getRGB(5, 5))
            assertEquals(0, image.getRGB(20, 20))
        } finally {
            graphics.dispose()
            texture.close()
        }
    }

    @Test
    fun texturedTriangleRepeatsUvCoordinatesOutsideZeroToOne() {
        val source = BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB)
        for (x in 0 until 4) {
            for (y in 0 until 4) {
                source.setRGB(x, y, if (x < 2) 0xFFFF0000.toInt() else 0xFF00FF00.toInt())
            }
        }
        val texture = DesktopTexture(source)
        val image = BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            val context = DesktopRenderContext(graphics, image.width, image.height)
            context.drawTexturedTriangle(
                texture,
                2f, 2f, 0f, 0f,
                34f, 2f, 2f, 0f,
                2f, 34f, 0f, 2f
            )

            assertEquals(0xFFFF0000.toInt(), image.getRGB(5, 5))
            assertEquals(0xFF00FF00.toInt(), image.getRGB(17, 5))
            assertEquals(0xFFFF0000.toInt(), image.getRGB(21, 5))
        } finally {
            graphics.dispose()
            texture.close()
        }
    }

    @Test
    fun textureDrawingHonorsAlpha() {
        val source = BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB)
        for (x in 0 until 2) {
            for (y in 0 until 2) source.setRGB(x, y, 0xFFFF0000.toInt())
        }
        val texture = DesktopTexture(source)
        val image = BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            val context = DesktopRenderContext(graphics, image.width, image.height)
            context.drawTexture(texture, 0f, 0f, 4f, 4f, 0.5f)

            assertEquals(128, (image.getRGB(1, 1) ushr 24) and 0xFF)
            assertEquals(0x00FF0000, image.getRGB(1, 1) and 0x00FFFFFF)
        } finally {
            graphics.dispose()
            texture.close()
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
