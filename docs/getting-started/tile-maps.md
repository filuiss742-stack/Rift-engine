# 2D Tile Maps

`TileMapComponent` renders a regular grid of frames from a `SpriteSheet`. Attach it to a `GameObject` and set `renderDefaultVisual = false` so the owner acts as the map transform.

```kotlin
val texture = assets.loadTexture("world.tiles", "tiles/world.png")
    ?: error("Missing world tileset")
val sheet = SpriteSheet(texture, frameWidth = 16, frameHeight = 16)

val map = GameObject(
    x = 0f,
    y = 0f,
    width = 32 * 16f,
    height = 20 * 16f
).apply {
    renderDefaultVisual = false
    addComponent(
        TileMapComponent(
            columns = 32,
            rows = 20,
            tileWidth = 16f,
            tileHeight = 16f,
            spriteSheet = sheet
        )
    )
}
```

The tile index is row-major. `TileMapComponent.EMPTY` (-1) leaves a cell transparent. Use `setTile(column, row, frameIndex)`, `tileAt()`, and `copyTiles()` to edit or inspect the grid.

The component caches its sprite-sheet frames and skips empty cells while drawing. It inherits its owner's translation, rotation, and scale.

Current limits: regular grid sheets only, no editor/importer, autotiling, chunk streaming, per-tile collision generation, or GPU batching. Build collision geometry separately with `PhysicsWorld`.
