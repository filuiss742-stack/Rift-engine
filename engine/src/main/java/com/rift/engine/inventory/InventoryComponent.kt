package com.rift.engine.inventory

import com.rift.engine.entity.GameComponent

data class ItemDefinition(
    val id: String,
    val maxStack: Int = 99
) {
    init {
        require(id.matches(Regex("^[a-z][a-z0-9._-]{0,127}$"))) {
            "Item ID must be a stable lowercase identifier."
        }
        require(maxStack > 0) { "maxStack must be greater than zero." }
    }
}

data class InventoryStack(
    val itemId: String,
    val quantity: Int
)

/**
 * Slot-limited inventory primitive. Item definitions are registered explicitly;
 * add() returns the accepted quantity so callers can handle leftovers.
 */
class InventoryComponent(
    val maxSlots: Int
) : GameComponent {
    private val definitions = linkedMapOf<String, ItemDefinition>()
    private val contents = mutableListOf<InventoryStack>()

    init {
        require(maxSlots > 0) { "maxSlots must be greater than zero." }
    }

    fun registerItem(definition: ItemDefinition): Boolean {
        val existing = definitions[definition.id]
        if (existing != null) {
            require(existing == definition) {
                "Item '" + definition.id + "' is already registered with a different definition."
            }
            return false
        }
        definitions[definition.id] = definition
        return true
    }

    fun add(itemId: String, quantity: Int): Int {
        require(quantity > 0) { "quantity must be greater than zero." }
        val definition = definitions[itemId]
            ?: throw IllegalArgumentException("Unknown item ID '" + itemId + "'.")

        var remaining = quantity
        var index = 0
        while (index < contents.size && remaining > 0) {
            val stack = contents[index]
            if (stack.itemId == itemId && stack.quantity < definition.maxStack) {
                val accepted = minOf(remaining, definition.maxStack - stack.quantity)
                contents[index] = stack.copy(quantity = stack.quantity + accepted)
                remaining -= accepted
            }
            index += 1
        }

        while (remaining > 0 && contents.size < maxSlots) {
            val accepted = minOf(remaining, definition.maxStack)
            contents += InventoryStack(itemId, accepted)
            remaining -= accepted
        }

        return quantity - remaining
    }

    fun remove(itemId: String, quantity: Int): Int {
        require(quantity > 0) { "quantity must be greater than zero." }
        var remaining = quantity
        var index = contents.lastIndex
        while (index >= 0 && remaining > 0) {
            val stack = contents[index]
            if (stack.itemId == itemId) {
                val removed = minOf(remaining, stack.quantity)
                val nextQuantity = stack.quantity - removed
                if (nextQuantity == 0) {
                    contents.removeAt(index)
                } else {
                    contents[index] = stack.copy(quantity = nextQuantity)
                }
                remaining -= removed
            }
            index -= 1
        }
        return quantity - remaining
    }

    fun count(itemId: String): Int =
        contents.filter { it.itemId == itemId }.sumOf { it.quantity }

    fun contains(itemId: String, quantity: Int = 1): Boolean {
        require(quantity >= 0) { "quantity must be non-negative." }
        return count(itemId) >= quantity
    }

    fun stacks(): List<InventoryStack> = contents.toList()

    fun usedSlots(): Int = contents.size

    fun remainingSlots(): Int = maxSlots - contents.size

    fun clear() = contents.clear()
}
