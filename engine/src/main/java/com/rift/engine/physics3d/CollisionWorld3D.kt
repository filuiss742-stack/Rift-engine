package com.rift.engine.physics3d

import com.rift.engine.graphics3d.Vector3
import kotlin.math.sqrt

/** A normalized world-space ray. */
class Ray3(val origin: Vector3, direction: Vector3) {
    val direction: Vector3 = direction.normalized()
}

/** Result of a 3D ray query. Distance is measured in world units. */
data class RaycastHit3D(
    val colliderId: String,
    val distance: Float,
    val point: Vector3,
    val normal: Vector3
)

sealed interface Collider3D {
    val id: String
    val layerMask: Int
    val enabled: Boolean
    fun raycast(ray: Ray3, maxDistance: Float): RaycastHit3D?
}

/** Axis-aligned box collider. Visual mesh geometry is intentionally separate. */
data class AabbCollider3D(
    override val id: String,
    val center: Vector3,
    val halfExtents: Vector3,
    override val layerMask: Int = DEFAULT_COLLISION_LAYER,
    override val enabled: Boolean = true
) : Collider3D {
    init {
        require(id.isNotBlank())
        require(halfExtents.x > 0f && halfExtents.y > 0f && halfExtents.z > 0f)
        require(layerMask != 0)
    }

    override fun raycast(ray: Ray3, maxDistance: Float): RaycastHit3D? {
        var near = Float.NEGATIVE_INFINITY
        var far = Float.POSITIVE_INFINITY
        var nearNormal = Vector3.ZERO
        var farNormal = Vector3.ZERO

        fun slab(origin: Float, direction: Float, minimum: Float, maximum: Float,
                 negativeNormal: Vector3, positiveNormal: Vector3): Boolean {
            if (kotlin.math.abs(direction) <= DIRECTION_EPSILON) {
                return origin in minimum..maximum
            }
            var first = (minimum - origin) / direction
            var second = (maximum - origin) / direction
            var firstNormal = negativeNormal
            var secondNormal = positiveNormal
            if (first > second) {
                val swap = first
                first = second
                second = swap
                val normalSwap = firstNormal
                firstNormal = secondNormal
                secondNormal = normalSwap
            }
            if (first > near) {
                near = first
                nearNormal = firstNormal
            }
            if (second < far) {
                far = second
                farNormal = secondNormal
            }
            return near <= far
        }

        val min = center - halfExtents
        val max = center + halfExtents
        if (!slab(ray.origin.x, ray.direction.x, min.x, max.x, -Vector3.RIGHT, Vector3.RIGHT)) return null
        if (!slab(ray.origin.y, ray.direction.y, min.y, max.y, -Vector3.UP, Vector3.UP)) return null
        if (!slab(ray.origin.z, ray.direction.z, min.z, max.z, Vector3.FORWARD, -Vector3.FORWARD)) return null

        val inside = near < 0f
        val distance = if (inside) far else near
        val normal = if (inside) farNormal else nearNormal
        if (!distance.isFinite() || distance < 0f || distance > maxDistance) return null
        return RaycastHit3D(id, distance, ray.origin + ray.direction * distance, normal)
    }
}

/** Sphere collider. */
data class SphereCollider3D(
    override val id: String,
    val center: Vector3,
    val radius: Float,
    override val layerMask: Int = DEFAULT_COLLISION_LAYER,
    override val enabled: Boolean = true
) : Collider3D {
    init {
        require(id.isNotBlank())
        require(radius.isFinite() && radius > 0f)
        require(layerMask != 0)
    }

    override fun raycast(ray: Ray3, maxDistance: Float): RaycastHit3D? {
        val offset = ray.origin - center
        val b = offset.dot(ray.direction)
        val c = offset.dot(offset) - radius * radius
        val discriminant = b * b - c
        if (discriminant < 0f) return null
        val root = sqrt(discriminant)
        val near = -b - root
        val far = -b + root
        val distance = if (near >= 0f) near else far
        if (!distance.isFinite() || distance < 0f || distance > maxDistance) return null
        val point = ray.origin + ray.direction * distance
        val normal = (point - center).normalized()
        return RaycastHit3D(id, distance, point, normal)
    }
}

/**
 * Small general-purpose 3D query world.
 *
 * This initial implementation is linear in collider count. A spatial broad
 * phase can replace the internal storage without changing the query contract.
 */
class CollisionWorld3D {
    private val colliders = linkedMapOf<String, Collider3D>()

    val size: Int get() = colliders.size

    fun add(collider: Collider3D) {
        require(!colliders.containsKey(collider.id)) {
            "Duplicate 3D collider id: ${collider.id}"
        }
        colliders[collider.id] = collider
    }

    fun remove(id: String): Boolean = colliders.remove(id) != null

    fun collider(id: String): Collider3D? = colliders[id]

    fun raycastAll(
        ray: Ray3,
        maxDistance: Float = Float.POSITIVE_INFINITY,
        queryLayerMask: Int = ALL_COLLISION_LAYERS
    ): List<RaycastHit3D> {
        require(maxDistance > 0f && !maxDistance.isNaN())
        require(queryLayerMask != 0)
        return colliders.values.asSequence()
            .filter { it.enabled && (it.layerMask and queryLayerMask) != 0 }
            .mapNotNull { it.raycast(ray, maxDistance) }
            .sortedBy { it.distance }
            .toList()
    }

    fun raycastFirst(
        ray: Ray3,
        maxDistance: Float = Float.POSITIVE_INFINITY,
        queryLayerMask: Int = ALL_COLLISION_LAYERS
    ): RaycastHit3D? = raycastAll(ray, maxDistance, queryLayerMask).firstOrNull()

    companion object {
        const val DEFAULT_COLLISION_LAYER: Int = 1
        const val ALL_COLLISION_LAYERS: Int = -1
        private const val DIRECTION_EPSILON = 1e-7f
    }
}
