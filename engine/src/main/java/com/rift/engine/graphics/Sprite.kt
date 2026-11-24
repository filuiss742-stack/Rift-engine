package com.rift.engine.graphics

/** A validated rectangular region inside a loaded texture. */
data class TextureRegion(
    val texture: TextureResource,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    init {
        require(!texture.isDisposed) { "TextureRegion cannot reference a disposed texture." }
        require(
            left >= 0 && top >= 0 &&
                right <= texture.width && bottom <= texture.height &&
                left < right && top < bottom
        ) { "TextureRegion must be a non-empty rectangle inside the texture." }
    }

    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

/** A drawable texture region with a default logical size. */
data class Sprite(
    val region: TextureRegion,
    val width: Float = region.width.toFloat(),
    val height: Float = region.height.toFloat()
) {
    init {
        require(width.isFinite() && width >= 0f) { "Sprite width must be finite and non-negative." }
        require(height.isFinite() && height >= 0f) { "Sprite height must be finite and non-negative." }
    }

    fun draw(
        context: RenderContext,
        x: Float,
        y: Float,
        destinationWidth: Float = width,
        destinationHeight: Float = height,
        alpha: Float = 1f
    ) {
        require(destinationWidth.isFinite() && destinationWidth >= 0f)
        require(destinationHeight.isFinite() && destinationHeight >= 0f)
        context.drawTextureRegion(
            region.texture,
            region.left,
            region.top,
            region.right,
            region.bottom,
            x,
            y,
            x + destinationWidth,
            y + destinationHeight,
            alpha
        )
    }
}

/** Grid helper for equally sized sprite-sheet frames. */
class SpriteSheet(
    private val texture: TextureResource,
    val frameWidth: Int,
    val frameHeight: Int,
    private val margin: Int = 0,
    private val spacing: Int = 0
) {
    val columns: Int
    val rows: Int
    val frameCount: Int

    init {
        require(!texture.isDisposed) { "SpriteSheet texture is disposed." }
        require(frameWidth > 0 && frameHeight > 0) { "Frame dimensions must be greater than zero." }
        require(margin >= 0 && spacing >= 0) { "Margin and spacing must be non-negative." }
        val usableWidth = texture.width - margin * 2 + spacing
        val usableHeight = texture.height - margin * 2 + spacing
        require(usableWidth >= frameWidth && usableHeight >= frameHeight) {
            "Sprite frame does not fit inside the texture."
        }
        columns = (usableWidth / (frameWidth + spacing)).coerceAtLeast(1)
        rows = (usableHeight / (frameHeight + spacing)).coerceAtLeast(1)
        frameCount = columns * rows
    }

    fun sprite(index: Int): Sprite {
        require(index in 0 until frameCount) { "Frame index is outside the sprite sheet." }
        val column = index % columns
        val row = index / columns
        val left = margin + column * (frameWidth + spacing)
        val top = margin + row * (frameHeight + spacing)
        return Sprite(TextureRegion(texture, left, top, left + frameWidth, top + frameHeight))
    }
}
