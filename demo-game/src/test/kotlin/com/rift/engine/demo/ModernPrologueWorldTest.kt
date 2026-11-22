package com.rift.engine.demo

import org.junit.Assert.assertTrue
import org.junit.Test

class ModernPrologueWorldTest {
    @Test
    fun everyAuthoredObjectFitsInsideItsMap() {
        listOf(ModernPrologueWorld.apartment, ModernPrologueWorld.street).forEach { map ->
            map.objects.forEach { item ->
                assertTrue("object is outside map bounds", item.x >= 0f && item.y >= 0f)
                assertTrue("object exceeds map bounds", item.x + item.width <= map.width && item.y + item.height <= map.height)
                assertTrue("object has invalid size", item.width > 0f && item.height > 0f)
            }
            map.blockers.forEach { blocker ->
                assertTrue("blocker is outside map bounds", blocker.left >= 0f && blocker.top >= 0f && blocker.right <= map.width && blocker.bottom <= map.height)
            }
        }
    }

    @Test
    fun mapsHaveValidSpawnsAndUniqueObjectIds() {
        listOf(ModernPrologueWorld.apartment, ModernPrologueWorld.street).forEach { map ->
            assertTrue(map.spawnX >= 0f && map.spawnX < map.width)
            assertTrue(map.spawnY >= 0f && map.spawnY < map.height)
            assertTrue(map.objects.map { it.id }.toSet().size == map.objects.size)
        }
    }
}