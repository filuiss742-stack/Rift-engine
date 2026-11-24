package com.rift.engine.assets

import com.rift.engine.diagnostics.DiagnosticReporter
import com.rift.engine.graphics.TextureLoader
import com.rift.engine.graphics.TextureResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetManagerTest {
    private class FakeTexture : TextureResource {
        override val width = 16
        override val height = 16
        var closes = 0
        override val isDisposed: Boolean
            get() = closes > 0

        override fun close() {
            closes += 1
        }
    }


    @Test
    fun failedTextureDisposalIsReportedWithoutRestoringTheCacheEntry() {
        val diagnostics = DiagnosticReporter()
        val texture = object : FakeTexture() {
            override fun close() {
                throw IllegalStateException("native texture release failed")
            }
        }
        val assets = AssetManager(TextureLoader { texture }, diagnostics)
        assets.loadTexture("hero", "hero.png")

        assertTrue(assets.unloadTexture("hero"))
        assertNull(assets.texture("hero"))
        assertEquals("RIFT_ASSET_TEXTURE_DISPOSE_FAILED", diagnostics.latestError()?.code)
    }

    @Test
    fun cachesTextureByStableIdAndPath() {
        val texture = FakeTexture()
        var loads = 0
        val assets = AssetManager(TextureLoader {
            loads += 1
            texture
        })

        val first = assets.loadTexture("hero", "sprites/hero.png")
        val second = assets.loadTexture("hero", "sprites/hero.png")

        assertSame(texture, first)
        assertSame(first, second)
        assertEquals(1, loads)
        assertSame(texture, assets.texture("hero"))
    }

    @Test
    fun rejectsIdReuseWithDifferentPath() {
        val diagnostics = DiagnosticReporter()
        val assets = AssetManager(TextureLoader { FakeTexture() }, diagnostics)
        assets.loadTexture("hero", "sprites/hero.png")

        assertNull(assets.loadTexture("hero", "sprites/other.png"))
        assertEquals("RIFT_ASSET_ID_CONFLICT", diagnostics.latestError()?.code)
    }

    @Test
    fun rejectsUnsafePathsBeforeCallingLoader() {
        var loads = 0
        val assets = AssetManager(TextureLoader {
            loads += 1
            FakeTexture()
        })

        assertNull(assets.loadTexture("hero", "../outside.png"))
        assertNull(assets.loadTexture("hero2", "/absolute.png"))
        assertEquals(0, loads)
    }

    @Test
    fun unloadDisposesResourceAndRemovesId() {
        val texture = FakeTexture()
        val assets = AssetManager(TextureLoader { texture })
        assets.loadTexture("hero", "hero.png")

        assertTrue(assets.unloadTexture("hero"))
        assertEquals(1, texture.closes)
        assertNull(assets.texture("hero"))
        assertFalse(assets.unloadTexture("hero"))
    }


    @Test
    fun rejectsTextureHandleAlreadyOwnedByAnotherId() {
        val texture = FakeTexture()
        val assets = AssetManager(TextureLoader { texture })
        assets.loadTexture("first", "first.png")

        assertNull(assets.loadTexture("second", "second.png"))
        assertEquals(0, texture.closes)

        assets.close()
        assertEquals(1, texture.closes)
    }

    @Test
    fun closeDisposesAllTexturesOnlyOnce() {
        val first = FakeTexture()
        val second = FakeTexture()
        val textures = listOf(first, second)
        var index = 0
        val assets = AssetManager(TextureLoader { textures[index++] })
        assets.loadTexture("first", "first.png")
        assets.loadTexture("second", "second.png")

        assets.close()
        assets.close()

        assertEquals(1, first.closes)
        assertEquals(1, second.closes)
        assertTrue(assets.loadedTextureIds().isEmpty())
    }

    @Test
    fun missingTextureProducesStructuredDiagnostic() {
        val diagnostics = DiagnosticReporter()
        val assets = AssetManager(TextureLoader { null }, diagnostics)

        assertNull(assets.loadTexture("missing", "missing.png"))
        assertEquals("RIFT_ASSET_TEXTURE_NOT_FOUND", diagnostics.latestError()?.code)
    }
}
