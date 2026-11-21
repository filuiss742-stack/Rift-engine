# Movement Profiles and Fixed Timing

This document describes the movement and timing foundation currently available in Rift Engine. It is deliberately smaller than a full physics or collision system.

## Fixed-step game updates

The Android surface host uses `FixedStepAccumulator` to call `GameEngine.update` at a fixed 1/60 second interval even when rendering takes a variable amount of time.

```kotlin
val clock = FixedStepAccumulator(stepSeconds = 1f / 60f)

clock.advance(frameDeltaSeconds) { fixedDelta ->
    engine.update(fixedDelta)
}
```

- The default update interval is 1/60 second.
- A long rendered frame is clamped to 0.25 seconds by default.
- At most five catch-up updates run per rendered frame by default.
- Excess time is deliberately discarded and counted in `droppedTimeSeconds` to avoid an unbounded catch-up spiral.
- The returned interpolation fraction is available for a future render-interpolation layer; the engine does not interpolate object rendering yet.
- Rendering is paced toward 60 frames per second by the Android surface host. Device scheduling and surface availability can still affect the actual rate.

## Movement state and input

Movement is separate from the input device. Touch controls, keyboard, gamepad, or scripted logic can all produce the same `MovementInput`.

```kotlin
player.movementInput = MovementInput(
    horizontal = 1f,
    vertical = 0f
)
```

Axes are intended to range from -1 to 1. `jumpPressed` is a one-update press signal, not a persistent button-held state.

Each `GameObject` owns a `MovementBody` through `player.movementBody`. Existing `x`, `y`, `velocityX`, and `velocityY` properties remain available as compatibility accessors.

## Top-down movement

```kotlin
player.movementProfile = TopDownMovementProfile(speed = 180f)
player.movementInput = MovementInput(horizontal = 1f, vertical = 0f)
```

Diagonal input is normalized, so moving diagonally is not faster than moving horizontally or vertically.

## Side-view platform movement

```kotlin
player.movementProfile = PlatformerMovementProfile(
    horizontalSpeed = 190f,
    gravity = 980f,
    jumpSpeed = 420f,
    maxFallSpeed = 900f
)
player.movementInput = MovementInput(horizontal = 1f, jumpPressed = true)
```

Positive Y points down. Gravity increases positive vertical velocity; jumping assigns a negative vertical velocity.

**Ground contact is not automatic yet.** A collision system or game code must set `player.movementBody.isGrounded` when the character is standing on a surface, and clear it when leaving the surface. The movement profile does not create floors, resolve collisions, or prevent tunnelling. Those belong to the upcoming collision-world stage.

## Entity update order

When a `GameObject` has a movement profile, its `update(deltaSeconds)` first asks the profile to calculate velocity, then integrates position using that velocity. Do not also move the same object manually in the same update unless you intentionally want to add another movement effect.

## Current limitations

- No collision world or automatic ground detection.
- No slopes, moving platforms, coyote time, jump buffering, friction, acceleration curves, or air-control options yet.
- No render interpolation between fixed updates.
- Movement profiles are mutually exclusive per object at this stage; composing separate locomotion modifiers will be added only when ownership rules are defined.
- Input actions are data values; device-specific bindings and virtual controls are a later layer.

## Troubleshooting

- **The platform character falls forever:** no collision system currently marks it grounded. Set `movementBody.isGrounded` from your game logic until the collision-world module exists.
- **Jump triggers repeatedly:** send `jumpPressed = true` only for the update where the press begins.
- **Diagonal top-down movement is too fast:** the built-in top-down profile normalizes diagonal input; check that another system is not changing velocity afterward.
- **The game slows down after a long frame:** the accumulator limits catch-up work and discards excess time intentionally. Check `droppedTimeSeconds` to observe this behavior.
