package com.rift.engine.graphics

import org.junit.Assert.*
import org.junit.Test

class ProceduralMaterialsTest {
    @Test fun oakFloorIsDeterministicAndHasVisibleVariation() {
        val first = ProceduralMaterials.oakFloor(96, 96, 7)
        val second = ProceduralMaterials.oakFloor(96, 96, 7)
        assertArrayEquals(first.copyPixels(), second.copyPixels())
        assertTrue(first.copyPixels().toSet().size > 20)
    }

    @Test fun differentSeedsProduceDifferentMaterials() {
        val first = ProceduralMaterials.plaster(64, 64, 1)
        val second = ProceduralMaterials.plaster(64, 64, 2)
        assertFalse(first.copyPixels().contentEquals(second.copyPixels()))
    }

    @Test fun allMaterialsHaveOpaquePixelsAndCorrectDimensions() {
        val materials = listOf(
            ProceduralMaterials.oakFloor(64, 48),
            ProceduralMaterials.plaster(64, 48),
            ProceduralMaterials.concrete(64, 48)
        )
        materials.forEach {
            assertEquals(64, it.width)
            assertEquals(48, it.height)
            assertEquals(64 * 48, it.copyPixels().size)
            assertTrue(it.copyPixels().all { pixel -> pixel ushr 24 == 0xFF })
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidDimensions() {
        ProceduralMaterials.oakFloor(8, 8)
    }
}
