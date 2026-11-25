package com.rift.engine.graphics3d

/**
 * Normalized texture rectangle for one cell in a regular sprite atlas.
 * Coordinates use the same top-left origin as TextureUv.
 */
data class TextureUvRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    init {
        require(left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite()) {
            "Sprite atlas UV coordinates must be finite."
        }
        require(left >= 0f && top >= 0f && right <= 1f && bottom <= 1f &&
            left < right && top < bottom) {
            "Sprite atlas UV rectangle must be a non-empty region inside [0, 1]."
        }
    }

    val topLeft get() = TextureUv(left, top)
    val topRight get() = TextureUv(right, top)
    val bottomRight get() = TextureUv(right, bottom)
    val bottomLeft get() = TextureUv(left, bottom)
}

/**
 * Resolves a row-major atlas frame to normalized UV coordinates.
 *
 * This keeps frame selection independent from image dimensions and lets a
 * renderer animate individual atlas cells without copying/cropping textures.
 */
fun spriteAtlasFrameUv(frameIndex: Int, columns: Int, rows: Int): TextureUvRect {
    require(columns > 0 && rows > 0) { "Sprite atlas grid dimensions must be positive." }
    val frameCount = columns.toLong() * rows.toLong()
    require(frameIndex >= 0 && frameIndex.toLong() < frameCount) {
        "Sprite atlas frame index is outside the configured grid."
    }
    val column = frameIndex % columns
    val row = frameIndex / columns
    return TextureUvRect(
        left = column.toFloat() / columns,
        top = row.toFloat() / rows,
        right = (column + 1).toFloat() / columns,
        bottom = (row + 1).toFloat() / rows
    )
}
