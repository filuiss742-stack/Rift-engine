package com.rift.engine.graphics3d

/**
 * Platform-neutral perspective camera.
 *
 * The camera looks along its target direction using a right-handed coordinate
 * system. The renderer consumes [viewMatrix] and [projectionMatrix].
 */
class Camera3D(
    position: Vector3 = Vector3(0f, 2f, 5f),
    target: Vector3 = Vector3.ZERO,
    up: Vector3 = Vector3.UP,
    verticalFovRadians: Float = Math.toRadians(60.0).toFloat(),
    aspectRatio: Float = 16f / 9f,
    near: Float = 0.1f,
    far: Float = 1000f
) {
    var position: Vector3 = position
        set(value) {
            field = value
            validateView()
        }

    var target: Vector3 = target
        set(value) {
            field = value
            validateView()
        }

    var up: Vector3 = up
        set(value) {
            field = value
            validateView()
        }

    var verticalFovRadians: Float = verticalFovRadians
        set(value) {
            validateProjection(value, aspectRatio, near, far)
            field = value
        }

    var aspectRatio: Float = aspectRatio
        set(value) {
            validateProjection(verticalFovRadians, value, near, far)
            field = value
        }

    var near: Float = near
        set(value) {
            validateProjection(verticalFovRadians, aspectRatio, value, far)
            field = value
        }

    var far: Float = far
        set(value) {
            validateProjection(verticalFovRadians, aspectRatio, near, value)
            field = value
        }

    init {
        validateView()
        validateProjection(verticalFovRadians, aspectRatio, near, far)
    }

    val viewMatrix: Matrix4
        get() = Matrix4.lookAt(position, target, up)

    val projectionMatrix: Matrix4
        get() = Matrix4.perspective(verticalFovRadians, aspectRatio, near, far)

    val viewProjectionMatrix: Matrix4
        get() = projectionMatrix * viewMatrix

    fun lookAt(nextTarget: Vector3) {
        target = nextTarget
    }

    private fun validateView() {
        require((target - position).lengthSquared() > VIEW_EPSILON) {
            "Camera position and target must differ."
        }
        require(up.lengthSquared() > VIEW_EPSILON) {
            "Camera up vector must be non-zero."
        }
        require(
            (target - position).cross(up).lengthSquared() > VIEW_EPSILON
        ) {
            "Camera direction and up vector must not be parallel."
        }
    }

    private fun validateProjection(
        fov: Float,
        aspect: Float,
        nearPlane: Float,
        farPlane: Float
    ) {
        require(fov.isFinite() && fov > 0f && fov < Math.PI.toFloat()) {
            "Vertical field of view must be between 0 and PI radians."
        }
        require(aspect.isFinite() && aspect > 0f) {
            "Aspect ratio must be finite and positive."
        }
        require(nearPlane.isFinite() && farPlane.isFinite() &&
            nearPlane > 0f && farPlane > nearPlane) {
            "Camera planes must satisfy 0 < near < far."
        }
    }

    companion object {
        private const val VIEW_EPSILON = 1e-8f
    }
}
