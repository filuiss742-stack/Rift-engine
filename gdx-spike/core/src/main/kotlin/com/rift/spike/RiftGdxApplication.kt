package com.rift.spike

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector3

/**
 * Disposable renderer spike only. This does not replace Rift's production runtime.
 * It proves the basic 3D-world + camera-facing 2D-sprite rendering path on both hosts.
 */
class RiftGdxApplication : ApplicationAdapter() {
    private lateinit var camera: PerspectiveCamera
    private lateinit var modelBatch: ModelBatch
    private lateinit var decalBatch: DecalBatch
    private lateinit var environment: Environment
    private lateinit var floorModel: Model
    private lateinit var propModel: Model
    private lateinit var floor: ModelInstance
    private lateinit var prop: ModelInstance
    private lateinit var spriteTexture: Texture
    private lateinit var sprite: Decal

    override fun create() {
        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.position.set(5f, 4f, 7f)
        camera.lookAt(0f, 1f, 0f)
        camera.near = 0.1f
        camera.far = 100f
        camera.update()

        modelBatch = ModelBatch()
        environment = Environment().apply {
            set(ColorAttribute(ColorAttribute.AmbientLight, 0.72f, 0.72f, 0.72f, 1f))
            add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.3f))
        }

        val builder = ModelBuilder()
        val attributes = VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal
        floorModel = builder.createBox(
            16f, 0.2f, 16f,
            Material(ColorAttribute.createDiffuse(Color(0.28f, 0.30f, 0.32f, 1f))),
            attributes
        )
        propModel = builder.createBox(
            1.2f, 1.6f, 1.2f,
            Material(ColorAttribute.createDiffuse(Color(0.35f, 0.42f, 0.52f, 1f))),
            attributes
        )
        floor = ModelInstance(floorModel).apply { transform.setToTranslation(0f, -0.1f, 0f) }
        prop = ModelInstance(propModel).apply { transform.setToTranslation(2f, 0.8f, -1f) }

        spriteTexture = createPlaceholderSprite()
        decalBatch = DecalBatch(CameraGroupStrategy(camera))
        sprite = Decal.newDecal(0.7f, 1.4f, TextureRegion(spriteTexture), true).apply {
            setPosition(0f, 0.85f, 0f)
        }
    }

    override fun render() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0.10f, 0.13f, 0.17f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        camera.viewportWidth = Gdx.graphics.width.toFloat()
        camera.viewportHeight = Gdx.graphics.height.toFloat()
        camera.update()

        modelBatch.begin(camera)
        modelBatch.render(floor, environment)
        modelBatch.render(prop, environment)
        modelBatch.end()

        decalBatch.add(sprite)
        decalBatch.flush()
    }

    override fun resize(width: Int, height: Int) {
        if (height <= 0) return
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
    }

    override fun dispose() {
        modelBatch.dispose()
        decalBatch.dispose()
        floorModel.dispose()
        propModel.dispose()
        spriteTexture.dispose()
    }

    private fun createPlaceholderSprite(): Texture {
        val pixmap = Pixmap(32, 64, Pixmap.Format.RGBA8888)
        pixmap.setColor(0f, 0f, 0f, 0f)
        pixmap.fill()
        pixmap.setColor(0.88f, 0.72f, 0.62f, 1f)
        pixmap.fillCircle(16, 12, 7)
        pixmap.setColor(0.22f, 0.30f, 0.55f, 1f)
        pixmap.fillRectangle(8, 20, 16, 34)
        pixmap.setColor(0.16f, 0.18f, 0.22f, 1f)
        pixmap.fillRectangle(8, 54, 6, 10)
        pixmap.fillRectangle(18, 54, 6, 10)
        val texture = Texture(pixmap)
        pixmap.dispose()
        return texture
    }
}
