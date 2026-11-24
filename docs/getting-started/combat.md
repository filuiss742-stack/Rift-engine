# Real-Time Combat Basics

Rift separates physical movement/collision from damage resolution:

- `PhysicsWorld` moves bodies and resolves physical contacts.
- `CombatWorld` checks owner-bound AABB hitboxes and hurtboxes and applies damage.
- `HealthComponent` stores health and owns the death callback.

## Health

```kotlin
val health = HealthComponent(
    maxHealth = 100f,
    onDeath = { gameState.putBoolean("enemy.defeated", true) }
)
enemy.addComponent(health)
```

Damage is clamped at zero. Healing is clamped at maximum health and does not revive a defeated entity. Call `reset()` when intentionally reusing an entity.

## Register hurtboxes and hitboxes

```kotlin
combat.addHurtbox(enemy, teamId = "enemy")

val slash = combat.addHitbox(
    owner = swordArea,
    damage = 25f,
    teamId = "player",
    targetTeams = setOf("enemy"),
    durationSeconds = 0.18f
)
```

A hitbox uses its owner's transformed AABB. Each hitbox damages a given target at most once. Team IDs prevent friendly fire, and `targetTeams` can further restrict targets. Timed hitboxes expire automatically.

Remove registrations when objects leave a scene:

```kotlin
combat.removeHitbox(slash)
combat.removeOwner(enemy)
```

The scene automatically removes combat registrations when an owned GameObject is removed.

Current scope is intentionally small: no separate attack shapes, knockback, invulnerability windows, combo buffering, status effects, or advanced combat framework yet. These can be layered on the public combat primitives without changing the platform hosts.
