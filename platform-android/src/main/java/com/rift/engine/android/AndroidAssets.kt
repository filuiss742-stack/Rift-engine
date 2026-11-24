package com.rift.engine.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.rift.engine.graphics.GeneratedTextureData
import com.rift.engine.graphics.TextureLoader
import com.rift.engine.graphics.TextureResource

class AndroidTexture internal constructor(
    internal val bitmap: Bitmap
) : TextureResource {
    override val width: Int
        get() = bitmap.width

    override val height: Int
        get() = bitmap.height

    override val isDisposed: Boolean
        get() = bitmap.isRecycled

    @Synchronized
    override fun close() {
        if (!bitmap.isRecycled) bitmap.recycle()
    }
}

/** Loads images from the host application's packaged assets directory. */
class AndroidTextureLoader(context: Context) : TextureLoader {
    private val applicationContext = context.applicationContext

    override fun load(path: String): TextureResource? {
        return applicationContext.assets.open(path).use { stream ->
            BitmapFactory.decodeStream(stream)?.let(::AndroidTexture)
        }
    }

    override fun create(data: GeneratedTextureData): TextureResource {
        val bitmap = Bitmap.createBitmap(data.copyPixels(), data.width, data.height, Bitmap.Config.ARGB_8888)
        return AndroidTexture(bitmap)
    }
}
