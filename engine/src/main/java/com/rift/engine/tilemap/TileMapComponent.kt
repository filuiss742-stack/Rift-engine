package com.rift.engine.tilemap

import com.rift.engine.entity.GameComponent
import com.rift.engine.entity.GameObject
import com.rift.engine.graphics.RenderContext
import com.rift.engine.graphics.Sprite
import com.rift.engine.graphics.SpriteSheet

/**
 * A compact grid of cached sprite-sheet frames attached to a GameObject.
 *
 * Tile index -1 is empty. Coordinates are local to the owning GameObject, so
 * the map inherits its owner's translation, rotation, and scale.
 */
class TileMapComponent(
    val columns: Int,
    val rows: Int,
    val tileWidth: Float,
    val tileHeight: Float,
    spriteSheet: SpriteSheet,
    initialTiles: IntArray = IntArray(columns * rows) { -1 }
) : GameComponent {
    private val sprites: List<Sprite> =
        (0 until spriteSheet.frameCount).map(spriteSheet::sprite)
    private val tiles: IntArray = initialTiles.copyOf()

    init {
        require(columns > 0 && rows > 0) {
            "Tile map dimensions must be positive."
        }
        require(tileWidth.isFinite() && tileWidth > 0f) {
            "tileWidth must be finite and greater than zero."
        }
        require(tileHeight.isFinite() && tileHeight > 0f) {
            "tileHeight must be finite and greater than zero."
        }
        require(initialTiles.size == columns * rows) {
            "Initial tile array size must equal columns * rows."
        }
        require(initialTiles.all { it == EMPTY || it in sprites.indices }) {
            "Tile indices must be EMPTY or a valid sprite-sheet frame index."
        }
    }

    fun tileAt(column: Int, row: Int): Int {
        requireInside(column, row)
        return tiles[row * columns + column]
    }

    fun setTile(column: Int, row: Int, tileIndex: Int) {
        requireInside(column, row)
        require(tileIndex == EMPTY || tileIndex in sprites.indices) {
            "Tile index must be EMPTY or a valid sprite-sheet frame index."
        }
        tiles[row * columns + column] = tileIndex
    }

    fun copyTiles(): IntArray = tiles.copyOf()

    override fun render(owner: GameObject, context: RenderContext) {
        var row = 0
        while (row < rows) {
            var column = 0
            while (column < columns) {
                val tileIndex = tiles[row * columns + column]
                if (tileIndex != EMPTY) {
                    sprites[tileIndex].draw(
                        context,
                        column * tileWidth,
                        row * tileHeight,
                        tileWidth,
                        tileHeight
                    )
                }
                column += 1
            }
            row += 1
        }
    }

    private fun requireInside(column: Int, row: Int) {
        require(column in 0 until columns && row in 0 until rows) {
            "Tile coordinate ($column, $row) is outside the map."
        }
    }

    companion object {
        const val EMPTY = -1
    }
}
