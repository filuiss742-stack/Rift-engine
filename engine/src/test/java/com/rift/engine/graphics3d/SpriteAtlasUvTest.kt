package com.rift.engine.graphics3d

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SpriteAtlasUvTest {
    @Test
    fun mapsRowMajorFramesToTopLeftNormalizedUvCoordinates() {
        val frame = spriteAtlasFrameUv(frameIndex = 5, columns = 4, rows = 2)

        assertEquals(.25f, frame.left, 0f)
        assertEquals(.5f, frame.top, 0f)
        assertEquals(.5f, frame.right, 0f)
        assertEquals(1f, frame.bottom, 0f)
        assertEquals(TextureUv(.25f,.5f), frame.topLeft)
        assertEquals(TextureUv(.5f,1f), frame.bottomRight)
    }

    @Test
    fun mapsFirstAndLastCellsWithoutBleedingIntoNeighbors() {
        val first = spriteAtlasFrameUv(0,4,2)
        val last = spriteAtlasFrameUv(7,4,2)

        assertEquals(0f,first.left,0f)
        assertEquals(0f,first.top,0f)
        assertEquals(.25f,first.right,0f)
        assertEquals(.5f,first.bottom,0f)
        assertEquals(.75f,last.left,0f)
        assertEquals(.5f,last.top,0f)
        assertEquals(1f,last.right,0f)
        assertEquals(1f,last.bottom,0f)
    }

    @Test
    fun rejectsInvalidGridAndFrameIndices() {
        assertThrows(IllegalArgumentException::class.java) { spriteAtlasFrameUv(-1,4,2) }
        assertThrows(IllegalArgumentException::class.java) { spriteAtlasFrameUv(8,4,2) }
        assertThrows(IllegalArgumentException::class.java) { spriteAtlasFrameUv(0,0,2) }
        assertThrows(IllegalArgumentException::class.java) { spriteAtlasFrameUv(0,4,0) }
    }
}
