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
}
