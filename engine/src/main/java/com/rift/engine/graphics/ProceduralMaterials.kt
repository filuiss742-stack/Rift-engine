package com.rift.engine.graphics

import kotlin.math.abs
import kotlin.math.sin

/**
 * CPU-generated, deterministic material pixels. No platform graphics APIs,
 * external scripts, or runtime network access are required.
 */
class GeneratedTextureData(
    val width: Int,
    val height: Int,
    pixels: IntArray
) {
    private val data = pixels.copyOf()
    init {
        require(width > 0 && height > 0) { "Texture dimensions must be positive." }
        require(width.toLong() * height == data.size.toLong()) {
            "Pixel count must equal width * height."
        }
    }
    fun copyPixels(): IntArray = data.copyOf()
    fun pixel(x: Int, y: Int): Int {
        require(x in 0 until width && y in 0 until height)
        return data[y * width + x]
    }
}

/** Deterministic material generator for the prologue's 3D environments. */
object ProceduralMaterials {
    fun oakFloor(width: Int = 256, height: Int = 256, seed: Int = 17): GeneratedTextureData {
        require(width >= 32 && height >= 32)
        val pixels = IntArray(width * height)
        val plankHeight = maxOf(16, height / 4)
        for (y in 0 until height) {
            val row = y / plankHeight
            val localY = y % plankHeight
            for (x in 0 until width) {
                val seamX = if (row % 2 == 0) width / 3 else width * 2 / 3
                val onSeam = x == seamX
                val edge = localY < 2 || localY >= plankHeight - 2
                val grain = sin((x * 0.105 + sin(y * 0.13) * 2.2) + row * 1.7) * 8.0 +
                    sin(x * 0.031 + y * 0.017) * 5.0
                val noise = hashNoise(x, y, seed) * 9.0 - 4.5
                val knotX = (x - (width * (0.22 + row * 0.17))).toDouble()
                val knotY = (localY - plankHeight * 0.52).toDouble()
                val knot = if (knotX * knotX / 90.0 + knotY * knotY / 18.0 < 1.0) -17.0 else 0.0
                val shade = if (edge || onSeam) -28.0 else grain + noise + knot
                val base = 154.0 + sin(localY * 0.08) * 4.0 + shade
                pixels[y * width + x] = argb(
                    (base + 25).toInt().coerceIn(0,255),
                    (base + 4).toInt().coerceIn(0,255),
                    (base - 28).toInt().coerceIn(0,255)
                )
            }
        }
        return GeneratedTextureData(width, height, pixels)
    }

    fun plaster(width: Int = 256, height: Int = 256, seed: Int = 31): GeneratedTextureData {
        require(width >= 8 && height >= 8)
        val pixels = IntArray(width * height)
        for (y in 0 until height) for (x in 0 until width) {
            val n = hashNoise(x, y, seed) * 12.0 - 6.0
            val broad = sin(x * .035) * 2.5 + sin(y * .027) * 2.0
            val value = (190.0 + n + broad).toInt().coerceIn(0,255)
            pixels[y * width + x] = argb(value, (value - 3).coerceIn(0,255), (value + 2).coerceIn(0,255))
        }
        return GeneratedTextureData(width, height, pixels)
    }

    fun concrete(width: Int = 256, height: Int = 256, seed: Int = 53): GeneratedTextureData {
        require(width >= 8 && height >= 8)
        val pixels = IntArray(width * height)
        for (y in 0 until height) for (x in 0 until width) {
            val n = hashNoise(x, y, seed) * 24.0 - 12.0
            val value = (112.0 + n + sin((x + y) * .04) * 3.0).toInt().coerceIn(0,255)
            pixels[y * width + x] = argb(value, value, (value + 2).coerceIn(0,255))
        }
        return GeneratedTextureData(width, height, pixels)
    }

    private fun hashNoise(x: Int, y: Int, seed: Int): Double {
        var n = x * 374761393 + y * 668265263 + seed * 1442695041
        n = (n xor (n ushr 13)) * 1274126177
        n = n xor (n ushr 16)
        return (n.toLong() and 0x7fffffffL).toDouble() / 2147483647.0
    }

    private fun argb(r: Int, g: Int, b: Int) =
        (0xFF shl 24) or (r shl 16) or (g shl 8) or b
}
