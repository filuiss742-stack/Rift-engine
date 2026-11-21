# 2D Physics World

Every Scene owns a `PhysicsWorld`. It is stepped automatically after scene objects update.

## Add bodies

```kotlin
import com.rift.engine.physics.PhysicsBodyType

val player = GameObject(80f, 40f, 32f, 32f)
val floor = GameObject(0f, 300f, 800f, 40f)

add(player)
add(floor)

physics.addBody(player) // DYNAMIC by default
physics.addBody(floor, PhysicsBodyType.STATIC)
```

Dynamic bodies receive world gravity and integrate velocity. Static bodies never move. Kinematic bodies move using their assigned velocity but are not accelerated by gravity. A registered body is integrated by PhysicsWorld rather than GameObject.update.

## Collision shapes\n\nBodies use AABB boxes by default. A body can instead use a circle shape with an optional local offset:\n\n```kotlin\nimport com.rift.engine.physics.CollisionShape\n\nphysics.addBody(\n    ball,\n    shape = CollisionShape.Circle(radius = 16f)\n)\n```\n\nCircle radius follows the largest absolute GameObject scale. Circle/box and circle/circle contacts are supported; rotated boxes still use enclosing AABBs.\n\n## Sensors and filtering

```kotlin
import com.rift.engine.physics.CollisionFilter
import com.rift.engine.physics.PhysicsBodyType

val pickup = GameObject(160f, 80f, 20f, 20f)
add(pickup)

physics.addBody(
    pickup,
    type = PhysicsBodyType.STATIC,
    isSensor = true,
    collisionFilter = CollisionFilter(
        categoryBits = 0b0010,
        maskBits = 0b0001
    )
)
```

Sensors report overlap contacts but do not push bodies apart. Category/mask bits must agree on both bodies. Equal non-zero group indices override masks: positive groups always collide, negative groups never collide.

## Read contacts

```kotlin
physics.contacts().forEach { contact ->
    if (contact.isSensor) {
        // Trigger / pickup overlap.
    } else {
        // Solid contact. The normal points from bodyA to bodyB.
    }
}
```

Contacts describe the most recent physics step. Poll `physics.contactEvents()` for BEGIN, STAY, and END lifecycle entries. Events are snapshots for the most recent step; they do not invoke callbacks automatically.

## Queries

```kotlin
val nearby = physics.queryAabb(Aabb(0f, 0f, 200f, 120f))
val hit = physics.raycast(0f, 100f, 500f, 100f)
```

A raycast returns the nearest hit, including the hit body, point, surface normal, and fraction along the segment. If the segment starts inside a shape, it reports the first exit surface when that surface lies within the segment.

## Current physics scope and limitations

- Box collision shapes use axis-aligned bounds derived from GameObject bounds. Circle shapes support circle/box and circle/circle overlap and response.
- Broad phase uses a spatial hash with stable, deduplicated candidate ordering. Very large bodies that span too many cells safely fall back to checking all bodies; queryAabb and raycast still scan registered bodies.
- The solver performs basic positional separation and removes velocity into the contact normal. It does not yet model friction, restitution, angular velocity, joints, or full rigid-body impulses.
- Adaptive substeps and iterative contact solving improve ordinary fast movement and stacked-body stability, but this is not continuous collision detection; extreme velocities can still pass through thin geometry.
- Rotated box objects use their enclosing axis-aligned bounds, not exact polygon collision. Circle offsets follow the object's transform.
- Contacts and BEGIN/STAY/END lifecycle events are polled after each step; automatic collision callbacks are not implemented yet.
- Movement profiles automatically detect when their GameObject is registered with PhysicsWorld and leave gravity integration to the world. This applies to both PlatformerMovementProfile and ResponsivePlatformerProfile. Keep the PhysicsBody gravityScale above zero when you want world gravity; setting it to 0 disables world gravity for that body.

The API is designed so a spatial-hash broad phase and more shape types can be added behind PhysicsWorld without changing game code.

See the [movement guide](movement.md), [API map](../api-map.md), and [0.5.0 release gates](../roadmap/0.5.0-release-gates.md).
