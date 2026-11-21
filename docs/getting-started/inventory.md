# Inventory Basics

`InventoryComponent` is a slot-limited, stack-aware inventory primitive. Register item definitions before adding items.

```kotlin
val inventory = InventoryComponent(maxSlots = 24)
inventory.registerItem(ItemDefinition("health.potion", maxStack = 10))
inventory.registerItem(ItemDefinition("quest.key", maxStack = 1))
player.addComponent(inventory)

val accepted = inventory.add("health.potion", 7)
if (accepted < 7) {
    // Keep or spawn the unaccepted remainder as a pickup.
}

val removed = inventory.remove("health.potion", 2)
val potionCount = inventory.count("health.potion")
```

- Item IDs must be stable lowercase identifiers.
- Existing stacks are filled before new slots are used.
- `add` returns the number accepted; a full inventory does not silently discard the remainder.
- `remove` returns the quantity actually removed.
- `stacks()` returns an immutable snapshot.

The current primitive does not include equipment, weight, crafting, item effects, UI, or structured save serialization.
