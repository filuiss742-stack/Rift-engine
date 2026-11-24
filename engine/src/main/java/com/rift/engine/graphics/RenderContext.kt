package com.rift.engine.graphics

/**
 * Platform-neutral renderer API.
 *
 * Coordinates are logical pixels and colors are ARGB ints. State operations
 * are stack-based: every pushState() must be paired with popState().
 */
interface RenderContext {
    val width: Int
    val height: Int

    fun clear(color: Int)
    fun drawRect(left: Float, top: Float, right: Float, bottom: Float, color: Int)
    fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radius: Float,
        color: Int
    )
    /**
     * Optional filled-triangle primitive. Legacy/test render contexts may omit it;
     * concrete 3D-capable platform backends should override it.
     */
    fun drawTriangle(
        x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, color: Int
    ) = Unit

    /**
     * Draws a texture-mapped triangle. UV coordinates are normalized to the
     * texture dimensions. Backends should clip to the destination triangle.
     * The default keeps legacy render contexts source-compatible.
     */
    fun drawTexturedTriangle(
        texture: TextureResource,
        x1: Float, y1: Float, u1: Float, v1: Float,
        x2: Float, y2: Float, u2: Float, v2: Float,
        x3: Float, y3: Float, u3: Float, v3: Float,
        alpha: Float = 1f
    ) {
        require(!texture.isDisposed) { "Cannot draw a disposed texture." }
        require(alpha.isFinite() && alpha in 0f..1f) { "alpha must be between zero and one." }
        require(listOf(x1,y1,u1,v1,x2,y2,u2,v2,x3,y3,u3,v3).all(Float::isFinite)) {
            "Triangle coordinates and UVs must be finite."
        }
        throw UnsupportedOperationException("This RenderContext does not support textured triangles.")
    }

    fun drawLine(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        color: Int,
        strokeWidth: Float = 1f
    )
    fun drawText(
        text: String,
        x: Float,
        y: Float,
        color: Int,
        size: Float,
        bold: Boolean = false
    )
    fun measureText(text: String, size: Float, bold: Boolean = false): Float

    fun pushState()
    fun popState()
    fun translate(x: Float, y: Float)
    fun rotate(degrees: Float)
    fun scale(x: Float, y: Float)
    fun clipRect(left: Float, top: Float, right: Float, bottom: Float)

    fun drawTexture(
        texture: TextureResource,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        alpha: Float = 1f
    ) {
        require(!texture.isDisposed) { "Cannot draw a disposed texture." }
        require(right > left && bottom > top) { "Texture destination rectangle must have positive dimensions." }
        drawTextureRegion(
            texture,
            0,
            0,
            texture.width,
            texture.height,
            left,
            top,
            right,
            bottom,
            alpha
        )
    }

    fun drawTextureRegion(
        texture: TextureResource,
        sourceLeft: Int,
        sourceTop: Int,
        sourceRight: Int,
        sourceBottom: Int,
        destinationLeft: Float,
        destinationTop: Float,
        destinationRight: Float,
        destinationBottom: Float,
        alpha: Float = 1f
    ) {
        require(!texture.isDisposed) { "Cannot draw a disposed texture." }
        require(alpha.isFinite() && alpha in 0f..1f) { "alpha must be between zero and one." }
        require(
            destinationRight.isFinite() && destinationBottom.isFinite() &&
                destinationLeft.isFinite() && destinationTop.isFinite() &&
                destinationRight > destinationLeft && destinationBottom > destinationTop
        ) { "Texture destination rectangle must have positive finite dimensions." }
        require(
            sourceLeft >= 0 && sourceTop >= 0 &&
                sourceRight <= texture.width && sourceBottom <= texture.height &&
                sourceLeft < sourceRight && sourceTop < sourceBottom
        ) { "Texture source rectangle is outside the texture bounds." }
        throw UnsupportedOperationException("This RenderContext does not support texture drawing.")
    }
}
