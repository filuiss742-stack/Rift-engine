package com.rift.engine.tilemap

import com.rift.engine.entity.GameObject
import com.rift.engine.graphics.RenderContext
import com.rift.engine.graphics.SpriteSheet
import com.rift.engine.graphics.TextureResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TileMapComponentTest {
    @Test
    fun rendersOnlyOccupiedTilesUsingCachedSpriteSheetFrames() {
        val texture = FakeTexture(64, 32)
        val sheet = SpriteSheet(texture, 16, 16)
        val tileMap = TileMapComponent(
            columns = 3,
            rows = 2,
            tileWidth = 16f,
            tileHeight = 16f,
            spriteSheet = sheet,
            initialTiles = intArrayOf(0, TileMapComponent.EMPTY, 5, 2, 3, TileMapComponent.EMPTY)
        )
        tileMap.setTile(1, 1, 7)
        val owner = GameObject(10f, 20f, 48f, 32f).apply {
            renderDefaultVisual = false
            addComponent(tileMap)
        }
        val context = RecordingContext()

        owner.render(context)

        assertEquals(4, context.draws.size)
        assertEquals(listOf(0, 5, 2, 3), context.draws.map { it.sourceLeft / 16 + (it.sourceTop / 16) * 4 })
        assertEquals(listOf(0f, 32f, 0f, 16f), context.draws.map { it.destinationLeft })
        assertEquals(listOf(0f, 0f, 16f, 16f), context.draws.map { it.destinationTop })
        assertEquals(7, tileMap.tileAt(1, 1))
    }

    @Test
    fun rejectsInvalidTileCoordinatesAndFrameIndices() {
        val sheet = SpriteSheet(FakeTexture(32, 32), 16, 16)
        val map = TileMapComponent(2, 2, 16f, 16f, sheet)

        assertThrows(IllegalArgumentException::class.java) {
            map.setTile(2, 0, 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            map.setTile(0, 0, 9)
        }
    }

    private class FakeTexture(
        override val width: Int,
        override val height: Int
    ) : TextureResource {
        override val isDisposed = false
        override fun close() = Unit
    }

    private data class Draw(
        val sourceLeft: Int,
        val sourceTop: Int,
        val destinationLeft: Float,
        val destinationTop: Float
    )

    private class RecordingContext : RenderContext {
        override val width = 320
        override val height = 240
        val draws = mutableListOf<Draw>()
        override fun clear(color: Int) = Unit
        override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, color: Int) = Unit
        override fun drawRoundRect(left: Float, top: Float, right: Float, bottom: Float, radius: Float, color: Int) = Unit
        override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, color: Int, strokeWidth: Float) = Unit
        override fun drawText(text: String, x: Float, y: Float, color: Int, size: Float, bold: Boolean) = Unit
        override fun measureText(text: String, size: Float, bold: Boolean): Float = 0f
        override fun pushState() = Unit
        override fun popState() = Unit
        override fun translate(x: Float, y: Float) = Unit
        override fun rotate(degrees: Float) = Unit
        override fun scale(x: Float, y: Float) = Unit
        override fun clipRect(left: Float, top: Float, right: Float, bottom: Float) = Unit
        override fun drawTextureRegion(
            texture: TextureResource,
            sourceLeft: Int,
            sourceTop: Int,
            sourceRight: Int,
            sourceBottom: Int,
            destinationLeft: Float,
            destinationTop: Float,
            destinationRight: Float,
            destinationBottom: Float,
            alpha: Float
        ) {
            draws += Draw(sourceLeft, sourceTop, destinationLeft, destinationTop)
        }
    }
}
