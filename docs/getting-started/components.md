# Compose GameObject Behavior with Components

A GameObject can own optional GameComponent instances. Use components for reusable behavior such as health, interaction, hit detection, animation state, or game-specific abilities. Keep the GameObject itself focused on identity, transform, and ownership.

## Create a component

```kotlin
import com.rift.engine.entity.GameComponent
import com.rift.engine.entity.GameObject

class HealthComponent(
    var maximum: Int = 100
) : GameComponent {
    var current: Int = maximum
        private set

    fun damage(amount: Int) {
        require(amount >= 0)
        current = (current - amount).coerceAtLeast(0)
    }

    fun heal(amount: Int) {
        require(amount >= 0)
        current = (current + amount).coerceAtMost(maximum)
    }

    override fun onAttach(owner: GameObject) {
        current = maximum
    }
}
```

## Attach and query

```kotlin
val player = GameObject(80f, 120f, 48f, 48f)
val health = HealthComponent(maximum = 120)

player.addComponent(health)
val attachedHealth = player.component<HealthComponent>()
attachedHealth?.damage(15)
```

The same component instance cannot be attached twice to one object. Multiple instances of the same component class are allowed. Components are identified by object identity, not by their equals implementation.

## Lifecycle

- onAttach runs when the component becomes owned by the object.
- update runs once for each active GameObject update.
- render runs after the GameObject's default visual.
- onDetach runs when the component is removed.

If a component is added or removed during update/render, the change is deferred until the current component traversal finishes. A component removed during update may still finish its current callback, but it will not be updated again after the traversal boundary.

## Design guidance

- Keep reusable behavior independent of a particular game genre.
- Use explicit references or game-level services for communication; avoid global object searches.
- Do not put platform-specific APIs in components.
- A component callback exception is reported by the runtime boundary, but the current traversal may stop for that frame. Components should validate their own inputs and avoid throwing during normal gameplay.
- Components are runtime objects. Scene serialization and automatic reconstruction are not implemented yet.
