package com.rift.engine.graphics3d

import com.rift.engine.graphics.TextureResource

/**
 * Directional sprite animation clip. Each entry is a separately authored
 * texture frame; frames are never synthesized by rotating another direction.
 */
data class DirectionalSpriteAnimation(
    val framesByDirection: Map<SpriteDirection8, List<TextureResource>>,
    val frameDurationSeconds: Float = 0.12f,
    val fallbackDirection: SpriteDirection8 = SpriteDirection8.FRONT,
    /** Optional per-frame atlas rectangles; omitted directions use the full texture. */
    val uvRectsByDirection: Map<SpriteDirection8, List<TextureUvRect>> = emptyMap()
) {
    init {
        require(frameDurationSeconds.isFinite() && frameDurationSeconds > 0f) {
            "Sprite frame duration must be finite and positive."
        }
        require(framesByDirection.isNotEmpty() && framesByDirection.values.all { it.isNotEmpty() }) {
            "Every supplied sprite direction must contain at least one frame."
        }
        require(framesByDirection.values.flatten().none { it.isDisposed }) {
            "Sprite animation cannot contain disposed textures."
        }
        require(fallbackDirection in framesByDirection) {
            "Configured fallback direction must have authored frames."
        }
        require(uvRectsByDirection.keys.all { it in framesByDirection }) {
            "Atlas UV regions cannot be configured for a missing sprite direction."
        }
        uvRectsByDirection.forEach { (direction, regions) ->
            require(regions.size == framesByDirection.getValue(direction).size) {
                "Every atlas-backed sprite frame must have exactly one UV region."
            }
        }
    }
}

/** Result keeps missing-view fallback visible to diagnostics and callers. */
data class DirectionalSpriteMeshResult(
    val mesh: Mesh3D,
    val selectedDirection: SpriteDirection8,
    val usedFallback: Boolean,
    val frameIndex: Int
)

/**
 * Builds a vertical, camera-facing textured quad anchored at the character's
 * feet. The mesh is fed through the normal 3D renderer, so world clipping and
 * the existing draw queue apply. It does not rotate or deform the source art.
 */
object DirectionalSpriteRenderer {
    fun buildMesh(
        position: Vector3,
        facingDirection: Vector3,
        camera: Camera3D,
        animation: DirectionalSpriteAnimation,
        elapsedSeconds: Float,
        worldWidth: Float,
        worldHeight: Float
    ): DirectionalSpriteMeshResult {
        require(elapsedSeconds.isFinite() && elapsedSeconds >= 0f) {
            "Sprite animation time must be finite and non-negative."
        }
        require(worldWidth.isFinite() && worldWidth > 0f &&
            worldHeight.isFinite() && worldHeight > 0f) {
            "Sprite world dimensions must be finite and positive."
        }

        val direction = DirectionalSpriteView.select(
            position, facingDirection, camera.position
        )
        val frames = animation.framesByDirection[direction]
        val usedFallback = frames == null
        val selectedDirection = if (usedFallback) animation.fallbackDirection else direction
        val selectedFrames = frames ?: animation.framesByDirection.getValue(animation.fallbackDirection)
        val frameIndex = (elapsedSeconds / animation.frameDurationSeconds)
            .toInt()
            .mod(selectedFrames.size)
        val texture = selectedFrames[frameIndex]
        check(!texture.isDisposed) { "Selected sprite frame texture has been disposed." }
        val uvRect = animation.uvRectsByDirection[selectedDirection]
            ?.get(frameIndex) ?: TextureUvRect(0f,0f,1f,1f)

        // Keep the sprite upright and horizontal width stable. The horizontal
        // billboard axis is derived from the camera's ground-plane direction.
        val toCamera = Vector3(
            camera.position.x - position.x,
            0f,
            camera.position.z - position.z
        )
        val horizontalFacing = if (toCamera.lengthSquared() > BILLBOARD_EPSILON) {
            toCamera.normalized()
        } else {
            Vector3(0f, 0f, 1f)
        }
        val right = Vector3.UP.cross(horizontalFacing).normalized()
        val halfWidth = worldWidth * 0.5f
        val leftBottom = position - right * halfWidth
        val rightBottom = position + right * halfWidth
        val leftTop = leftBottom + Vector3(0f, worldHeight, 0f)
        val rightTop = rightBottom + Vector3(0f, worldHeight, 0f)

        val uvTopLeft = uvRect.topLeft
        val uvTopRight = uvRect.topRight
        val uvBottomRight = uvRect.bottomRight
        val uvBottomLeft = uvRect.bottomLeft
        val triangles = listOf(
            MeshTriangle(leftTop, leftBottom, rightBottom, 0xFFFFFFFF.toInt(),
                texture, uvTopLeft, uvBottomLeft, uvBottomRight),
            MeshTriangle(leftTop, rightBottom, rightTop, 0xFFFFFFFF.toInt(),
                texture, uvTopLeft, uvBottomRight, uvTopRight)
        )
        return DirectionalSpriteMeshResult(
            Mesh3D(triangles), selectedDirection, usedFallback, frameIndex
        )
    }

    private const val BILLBOARD_EPSILON = 1e-8f
}
