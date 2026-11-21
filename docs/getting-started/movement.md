# Add Basic Movement

Movement profiles are shared Kotlin APIs. They do not depend on keyboard, touch, or a specific platform.

## Top-down

```kotlin
import com.rift.engine.movement.MovementInput
import com.rift.engine.movement.TopDownMovementProfile

player.movementProfile = TopDownMovementProfile(speed = 180f)
player.movementInput = MovementInput(horizontal = 1f, vertical = 0f)
```

The axes are normalized by the profile, so diagonal movement is not faster.

## Side-view platform movement

```kotlin
import com.rift.engine.movement.MovementInput
import com.rift.engine.movement.PlatformerMovementProfile

player.movementProfile = PlatformerMovementProfile()
player.movementBody.isGrounded = true // temporary game-supplied ground state
player.movementInput = MovementInput(horizontal = 1f, jumpPressed = true)
```

Positive Y points down. Gravity is positive and jump velocity is negative. Set `jumpPressed` for only the update in which a new jump press occurs.

## Responsive platformer movement

For more game-feel, use `ResponsivePlatformerProfile`:

```kotlin
player.movementProfile = ResponsivePlatformerProfile(
    maxSpeed = 220f,
    groundAcceleration = 1800f,
    groundDeceleration = 2200f,
    airControl = 0.6f,
    coyoteTimeSeconds = 0.1f,
    jumpBufferSeconds = 0.12f,
    variableJump = true
)

// Set jumpPressed on the press edge and jumpHeld while the control stays down.
player.movementInput = MovementInput(
    horizontal = horizontalAxis,
    jumpPressed = jumpWasPressed,
    jumpHeld = jumpIsHeld
)
```

The responsive profile keeps per-character jump timers, so create one profile instance per character. Its default gravity is 980 logical pixels/s². If your game separately synchronizes its vertical velocity with a GameObject controlled by `PhysicsWorld`, set the profile's `gravity = 0f` and let the physics world apply gravity; do not apply both gravity sources to the same movement.

The physics world uses AABB/circle bodies and updates grounded state for its GameObjects. `MovementBody` and `PhysicsBody` are separate APIs; connect them deliberately in game code rather than assuming one automatically controls the other. See the [2D physics guide](physics.md) for supported behavior and limitations.

## Update timing

Use seconds for `deltaSeconds`. The platform host controls the loop. Do not multiply the profile speed by delta time yourself: `GameObject.update` already integrates velocity into position using `deltaSeconds`.

See [movement and timing details](../movement-and-timing.md).
