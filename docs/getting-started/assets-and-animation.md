# Assets, Sprites, and Animation

The host supplies a platform-specific TextureLoader. The engine exposes the same AssetManager and TextureResource API to game scenes on Android and Windows.

## Load a texture

Place the image below the project's asset root. Android reads from the host application's packaged assets; the Windows host reads from its configured assets directory.

```kotlin
val texture = requireNotNull(assets).loadTexture(
    id = "hero.sheet",
    path = "sprites/hero.png"
)
```

Asset IDs are stable lowercase identifiers. Paths must be project-relative and cannot contain parent-directory traversal. Repeated loads using the same ID and path return the cached resource. Reusing an ID for another path produces a structured diagnostic.

## Create a sprite sheet

```kotlin
val sheet = SpriteSheet(
    texture = texture,
    frameWidth = 32,
    frameHeight = 32,
    margin = 0,
    spacing = 0
)
val idleSprite = sheet.sprite(0)
```

SpriteSheet maps equally sized cells in row-major order. TextureRegion can also describe a manually selected atlas rectangle. RenderContext supports full-texture and source-region drawing with alpha.

## Animate a GameObject

```kotlin
val walk = SpriteAnimation(
    frames = listOf(
        AnimationFrame(sheet.sprite(0), 0.12f),
        AnimationFrame(sheet.sprite(1), 0.12f),
        AnimationFrame(sheet.sprite(2), 0.12f),
        AnimationFrame(sheet.sprite(3), 0.12f)
    ),
    looping = true
)

val animator = SpriteAnimationComponent(
    animations = mapOf("walk" to walk),
    initialAnimationId = "walk"
)

player.renderDefaultVisual = false
player.addComponent(animator)
```

Use animator.play("walk") or another registered animation ID to change animations. Non-looping animations stop on their final frame and expose isFinished.

## Resource ownership

- AssetManager owns cached texture resources.
- unloadTexture(id) releases one cached texture.
- GameEngine.close() releases all cached textures.
- Do not manually close a texture while a Sprite or animation still references it.
- Missing or invalid textures produce structured diagnostics; loading is synchronous in this milestone.

## Current limitations

- Textures are currently loaded synchronously; background loading and streaming are not implemented.
- Texture formats depend on the platform decoders. PNG is the recommended shared format.
- No font asset pipeline, audio asset pipeline, atlas metadata importer, texture compression, or GPU resource batching yet.
- SpriteSheet supports regular grids. Irregular atlas packing requires explicit TextureRegion definitions.
- Sprite animation is frame-based; skeletal animation and animation blending are not implemented.

See the [API map](../api-map.md), [0.5.0 release gates](../roadmap/0.5.0-release-gates.md), and [shared game state](shared-game-state.md).
