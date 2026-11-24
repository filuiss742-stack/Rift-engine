package com.rift.engine.graphics3d

/**
 * Immutable 4x4 matrix stored in column-major order for OpenGL-compatible
 * upload. Multiplication follows column-vector convention: (A * B) * v.
 */
class Matrix4 private constructor(values: FloatArray) {
    private val elements = values.copyOf()

    init {
        require(elements.size == ELEMENT_COUNT) {
            "Matrix4 requires exactly 16 values."
        }
        require(elements.all(Float::isFinite)) {
            "Matrix4 values must be finite."
        }
    }

    operator fun get(row: Int, column: Int): Float {
        require(row in 0..3 && column in 0..3) {
            "Matrix4 row and column must be in 0..3."
        }
        return elements[column * 4 + row]
    }

    fun toFloatArray(): FloatArray = elements.copyOf()

    operator fun times(other: Matrix4): Matrix4 {
        val result = FloatArray(ELEMENT_COUNT)
        for (column in 0..3) {
            for (row in 0..3) {
                var value = 0f
                for (index in 0..3) {
                    value += this[row, index] * other[index, column]
                }
                result[column * 4 + row] = value
            }
        }
        return Matrix4(result)
    }

    /** Transforms a point (w=1) and performs the perspective divide. */
    fun transformPoint(point: Vector3): Vector3 {
        val x = this[0, 0] * point.x + this[0, 1] * point.y +
            this[0, 2] * point.z + this[0, 3]
        val y = this[1, 0] * point.x + this[1, 1] * point.y +
            this[1, 2] * point.z + this[1, 3]
        val z = this[2, 0] * point.x + this[2, 1] * point.y +
            this[2, 2] * point.z + this[2, 3]
        val w = this[3, 0] * point.x + this[3, 1] * point.y +
            this[3, 2] * point.z + this[3, 3]
        require(w.isFinite() && kotlin.math.abs(w) > HOMOGENEOUS_EPSILON) {
            "Matrix4 transformed point has an invalid homogeneous coordinate."
        }
        return Vector3(x / w, y / w, z / w)
    }

    companion object {
        private const val ELEMENT_COUNT = 16
        private const val HOMOGENEOUS_EPSILON = 1e-6f

        fun identity(): Matrix4 = Matrix4(floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        ))

        /**
         * Right-handed OpenGL perspective projection. FOV is vertical and in
         * radians; near/far are positive camera-space distances.
         */
        fun perspective(
            verticalFovRadians: Float,
            aspectRatio: Float,
            near: Float,
            far: Float
        ): Matrix4 {
            require(verticalFovRadians.isFinite() &&
                verticalFovRadians > 0f && verticalFovRadians < Math.PI.toFloat()) {
                "Vertical field of view must be between 0 and PI radians."
            }
            require(aspectRatio.isFinite() && aspectRatio > 0f) {
                "Aspect ratio must be finite and positive."
            }
            require(near.isFinite() && far.isFinite() && near > 0f && far > near) {
                "Perspective planes must satisfy 0 < near < far."
            }

            val scale = 1f / kotlin.math.tan(verticalFovRadians / 2f)
            return Matrix4(floatArrayOf(
                scale / aspectRatio, 0f, 0f, 0f,
                0f, scale, 0f, 0f,
                0f, 0f, (far + near) / (near - far), -1f,
                0f, 0f, (2f * far * near) / (near - far), 0f
            ))
        }

        /** Right-handed view matrix looking from eye toward target. */
        fun lookAt(eye: Vector3, target: Vector3, up: Vector3): Matrix4 {
            val forward = (target - eye).normalized()
            val side = forward.cross(up).normalized()
            val correctedUp = side.cross(forward)

            return Matrix4(floatArrayOf(
                side.x, correctedUp.x, -forward.x, 0f,
                side.y, correctedUp.y, -forward.y, 0f,
                side.z, correctedUp.z, -forward.z, 0f,
                -side.dot(eye), -correctedUp.dot(eye), forward.dot(eye), 1f
            ))
        }
    }
}
