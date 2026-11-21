package com.rift.engine.inventory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryComponentTest {
    @Test
    fun stacksItemsAndReportsPartialAcceptanceWhenSlotsAreFull() {
        val inventory = InventoryComponent(maxSlots = 2)
        inventory.registerItem(ItemDefinition("potion", maxStack = 5))

        assertEquals(8, inventory.add("potion", 8))
        assertEquals(2, inventory.usedSlots())
        assertEquals(0, inventory.remainingSlots())
        assertEquals(2, inventory.add("potion", 10))
        assertEquals(0, inventory.add("potion", 10))
        assertEquals(10, inventory.count("potion"))
        assertEquals(
            listOf(
                InventoryStack("potion", 5),
                InventoryStack("potion", 5)
            ),
            inventory.stacks()
        )
    }

    @Test
    fun removingItemsFreesSlotsAndLaterAddsFillExistingStacksFirst() {
        val inventory = InventoryComponent(maxSlots = 2)
        inventory.registerItem(ItemDefinition("coin", maxStack = 5))
        inventory.add("coin", 8)

        assertEquals(6, inventory.remove("coin", 6))
        assertEquals(2, inventory.count("coin"))
        assertEquals(1, inventory.usedSlots())
        assertEquals(4, inventory.add("coin", 4))
        assertEquals(
            listOf(
                InventoryStack("coin", 5),
                InventoryStack("coin", 1)
            ),
            inventory.stacks()
        )
    }

    @Test
    fun duplicateDefinitionsMustMatchAndUnknownItemsAreRejected() {
        val inventory = InventoryComponent(maxSlots = 4)
        val definition = ItemDefinition("key", maxStack = 1)

        assertTrue(inventory.registerItem(definition))
        assertFalse(inventory.registerItem(definition))
        try {
            inventory.registerItem(ItemDefinition("key", maxStack = 5))
            throw AssertionError("Expected conflicting item definition to fail.")
        } catch (_: IllegalArgumentException) {
            // Expected conflict.
        }
        try {
            inventory.add("missing", 1)
            throw AssertionError("Expected unknown item to fail.")
        } catch (_: IllegalArgumentException) {
            // Expected unknown item.
        }
    }
}
