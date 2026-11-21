package com.rift.engine.physics

import com.rift.engine.entity.GameObject
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

enum class PhysicsBodyType {
    STATIC,
    KINEMATIC,
    DYNAMIC
}

/** Collision category and mask filtering. Bits 0..30 are available. */
data class CollisionFilter(
    val categoryBits: Int = 1,
    val maskBits: Int = -1,
    val groupIndex: Int = 0
) {
    init {
        require(categoryBits != 0) { "categoryBits must contain at least one bit." }
    }

    fun allows(other: CollisionFilter): Boolean {
        if (groupIndex != 0 && groupIndex == other.groupIndex) {
            return groupIndex > 0
        }
        return (maskBits and other.categoryBits) != 0 &&
            (other.maskBits and categoryBits) != 0
    }
}

/**
 * An AABB physics body attached to a GameObject.
 *
 * This first physics implementation supports axis-aligned box collision,
 * gravity, basic velocity integration, sensors, and collision filtering.
 */
class PhysicsBody internal constructor(
    val gameObject: GameObject,
    val type: PhysicsBodyType,
    mass: Float,
    gravityScale: Float,
    isSensor: Boolean,
    collisionFilter: CollisionFilter
) {
    var mass: Float = mass
        set(value) {
            require(value.isFinite() && value > 0f) {
                "Dynamic body mass must be finite and greater than zero."
            }
            field = value
        }

    var gravityScale: Float = gravityScale
        set(value) {
            require(value.isFinite()) { "gravityScale must be finite." }
            field = value
        }

    var isSensor: Boolean = isSensor
    var collisionFilter: CollisionFilter = collisionFilter
    var enabled: Boolean = true

    internal val inverseMass: Float
        get() = if (type == PhysicsBodyType.DYNAMIC) 1f / mass else 0f

    init {
        require(mass.isFinite() && mass > 0f) {
            "mass must be finite and greater than zero."
        }
        require(gravityScale.isFinite()) { "gravityScale must be finite." }
    }
}

/** A contact normal points from bodyA toward bodyB. */
data class CollisionContact(
    val bodyA: PhysicsBody,
    val bodyB: PhysicsBody,
    val normalX: Float,
    val normalY: Float,
    val penetration: Float,
    val isSensor: Boolean
)

data class RaycastHit(
    val body: PhysicsBody,
    val pointX: Float,
    val pointY: Float,
    val normalX: Float,
    val normalY: Float,
    val fraction: Float
)

/**
 * Deterministic small-world 2D AABB physics.
 *
 * The initial broad phase intentionally uses a stable O(n²) pair scan. This is
 * suitable for small and medium scenes and keeps behavior easy to inspect.
 * A spatial-hash broad phase can replace it without changing the public API.
 */
class PhysicsWorld(
    gravityX: Float = 0f,
    gravityY: Float = 980f,
    private val maxSubstepDistance: Float = 16f,
    private val maxSubsteps: Int = 16
) {
    var gravityX: Float = gravityX
        set(value) {
            require(value.isFinite()) { "gravityX must be finite." }
            field = value
        }

    var gravityY: Float = gravityY
        set(value) {
            require(value.isFinite()) { "gravityY must be finite." }
            field = value
        }

    private val bodies = mutableListOf<PhysicsBody>()
    private var currentContacts: List<CollisionContact> = emptyList()

    init {
        require(gravityX.isFinite() && gravityY.isFinite()) {
            "Gravity must be finite."
        }
        require(maxSubstepDistance.isFinite() && maxSubstepDistance > 0f) {
            "maxSubstepDistance must be finite and greater than zero."
        }
        require(maxSubsteps > 0) { "maxSubsteps must be greater than zero." }
    }

    fun addBody(
        gameObject: GameObject,
        type: PhysicsBodyType = PhysicsBodyType.DYNAMIC,
        mass: Float = 1f,
        gravityScale: Float = 1f,
        isSensor: Boolean = false,
        collisionFilter: CollisionFilter = CollisionFilter()
    ): PhysicsBody {
        require(bodies.none { it.gameObject === gameObject }) {
            "This GameObject already has a body in this PhysicsWorld."
        }
        require(!gameObject.physicsControlled) {
            "This GameObject is already controlled by a PhysicsWorld."
        }
        val body = PhysicsBody(
            gameObject,
            type,
            mass,
            gravityScale,
            isSensor,
            collisionFilter
        )
        bodies.add(body)
        gameObject.physicsControlled = true
        if (type != PhysicsBodyType.DYNAMIC) {
            gameObject.velocityX = 0f
            gameObject.velocityY = 0f
        }
        return body
    }

    fun removeBody(gameObject: GameObject): Boolean {
        val index = bodies.indexOfFirst { it.gameObject === gameObject }
        if (index < 0) return false
        bodies.removeAt(index).gameObject.physicsControlled = false
        currentContacts = currentContacts.filterNot {
            it.bodyA.gameObject === gameObject || it.bodyB.gameObject === gameObject
        }
        return true
    }

    fun bodyOf(gameObject: GameObject): PhysicsBody? {
        return bodies.firstOrNull { it.gameObject === gameObject }
    }

    fun bodies(): List<PhysicsBody> = bodies.toList()

    fun contacts(): List<CollisionContact> = currentContacts

    fun step(deltaSeconds: Float) {
        require(deltaSeconds.isFinite() && deltaSeconds >= 0f) {
            "deltaSeconds must be finite and non-negative."
        }
        if (deltaSeconds == 0f || bodies.isEmpty()) {
            currentContacts = emptyList()
            return
        }

        val activeBodies = bodies.filter { it.enabled && it.gameObject.active }
        if (activeBodies.isEmpty()) {
            currentContacts = emptyList()
            return
        }

        val maxDistance = activeBodies
            .filter { it.type == PhysicsBodyType.DYNAMIC }
            .maxOfOrNull { body ->
                val gameObject = body.gameObject
                val vx = abs(gameObject.velocityX + gravityX * body.gravityScale * deltaSeconds)
                val vy = abs(gameObject.velocityY + gravityY * body.gravityScale * deltaSeconds)
                max(vx, vy) * deltaSeconds
            } ?: 0f

        val substeps = ceil(maxDistance / maxSubstepDistance)
            .toInt()
            .coerceIn(1, maxSubsteps)
        val subDelta = deltaSeconds / substeps
        val contactsByPair = linkedMapOf<Pair<PhysicsBody, PhysicsBody>, CollisionContact>()

        repeat(substeps) {
            activeBodies.forEach { body ->
                if (body.type == PhysicsBodyType.DYNAMIC) {
                    val gameObject = body.gameObject
                    gameObject.velocityX += gravityX * body.gravityScale * subDelta
                    gameObject.velocityY += gravityY * body.gravityScale * subDelta
                    gameObject.x += gameObject.velocityX * subDelta
                    gameObject.y += gameObject.velocityY * subDelta
                } else if (body.type == PhysicsBodyType.KINEMATIC) {
                    val gameObject = body.gameObject
                    gameObject.x += gameObject.velocityX * subDelta
                    gameObject.y += gameObject.velocityY * subDelta
                }
            }

            var i = 0
            while (i < activeBodies.size) {
                var j = i + 1
                while (j < activeBodies.size) {
                    val first = activeBodies[i]
                    val second = activeBodies[j]
                    if (first.collisionFilter.allows(second.collisionFilter)) {
                        val contact = detectContact(first, second)
                        if (contact != null) {
                            contactsByPair[first to second] = contact
                            if (!contact.isSensor) resolve(contact)
                        }
                    }
                    j += 1
                }
                i += 1
            }
        }

        currentContacts = contactsByPair.values.toList()
    }

    fun queryAabb(
        area: Aabb,
        filter: CollisionFilter? = null
    ): List<PhysicsBody> {
        return bodies.filter { body ->
            body.enabled &&
                (filter == null || filter.allows(body.collisionFilter)) &&
                Collision.intersects(area, body.gameObject.bounds())
        }
    }

    fun raycast(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        filter: CollisionFilter? = null
    ): RaycastHit? {
        require(
            startX.isFinite() && startY.isFinite() &&
                endX.isFinite() && endY.isFinite()
        ) { "Ray coordinates must be finite." }

        val dx = endX - startX
        val dy = endY - startY
        var closest: RaycastHit? = null

        bodies.forEach { body ->
            if (!body.enabled || (filter != null && !filter.allows(body.collisionFilter))) {
                return@forEach
            }
            val bounds = body.gameObject.bounds()
            val hit = intersectSegment(bounds, startX, startY, dx, dy) ?: return@forEach
            if (closest == null || hit.fraction < closest!!.fraction) {
                closest = hit.copy(body = body)
            }
        }
        return closest
    }

    private fun detectContact(
        first: PhysicsBody,
        second: PhysicsBody
    ): CollisionContact? {
        if (first.inverseMass == 0f && second.inverseMass == 0f) return null
        val a = first.gameObject.bounds()
        val b = second.gameObject.bounds()
        if (!Collision.intersects(a, b)) return null

        val overlapX = min(a.right, b.right) - max(a.left, b.left)
        val overlapY = min(a.bottom, b.bottom) - max(a.top, b.top)
        val centerAX = (a.left + a.right) * 0.5f
        val centerAY = (a.top + a.bottom) * 0.5f
        val centerBX = (b.left + b.right) * 0.5f
        val centerBY = (b.top + b.bottom) * 0.5f

        return if (overlapX < overlapY) {
            CollisionContact(
                first,
                second,
                if (centerBX >= centerAX) 1f else -1f,
                0f,
                overlapX,
                first.isSensor || second.isSensor
            )
        } else {
            CollisionContact(
                first,
                second,
                0f,
                if (centerBY >= centerAY) 1f else -1f,
                overlapY,
                first.isSensor || second.isSensor
            )
        }
    }

    private fun resolve(contact: CollisionContact) {
        val first = contact.bodyA
        val second = contact.bodyB
        val inverseMassTotal = first.inverseMass + second.inverseMass
        if (inverseMassTotal <= 0f) return

        val correction = contact.penetration / inverseMassTotal
        first.gameObject.x -= contact.normalX * correction * first.inverseMass
        first.gameObject.y -= contact.normalY * correction * first.inverseMass
        second.gameObject.x += contact.normalX * correction * second.inverseMass
        second.gameObject.y += contact.normalY * correction * second.inverseMass

        if (contact.normalX > 0f) {
            if (first.gameObject.velocityX > 0f) first.gameObject.velocityX = 0f
            if (second.gameObject.velocityX < 0f) second.gameObject.velocityX = 0f
        } else if (contact.normalX < 0f) {
            if (first.gameObject.velocityX < 0f) first.gameObject.velocityX = 0f
            if (second.gameObject.velocityX > 0f) second.gameObject.velocityX = 0f
        }

        if (contact.normalY > 0f) {
            if (first.gameObject.velocityY > 0f) first.gameObject.velocityY = 0f
            if (second.gameObject.velocityY < 0f) second.gameObject.velocityY = 0f
            if (first.type == PhysicsBodyType.DYNAMIC) first.gameObject.isGrounded = true
        } else if (contact.normalY < 0f) {
            if (first.gameObject.velocityY < 0f) first.gameObject.velocityY = 0f
            if (second.gameObject.velocityY > 0f) second.gameObject.velocityY = 0f
            if (second.type == PhysicsBodyType.DYNAMIC) second.gameObject.isGrounded = true
        }
    }

    private fun intersectSegment(
        bounds: Aabb,
        startX: Float,
        startY: Float,
        dx: Float,
        dy: Float
    ): RaycastHit? {
        var tMin = 0f
        var tMax = 1f
        var normalX = 0f
        var normalY = 0f

        fun axis(
            start: Float,
            delta: Float,
            minimum: Float,
            maximum: Float,
            isX: Boolean
        ): Boolean {
            if (abs(delta) < 0.000001f) return start in minimum..maximum
            var near = (minimum - start) / delta
            var far = (maximum - start) / delta
            var sign = -1f
            if (near > far) {
                val swap = near
                near = far
                far = swap
                sign = 1f
            }
            if (near > tMin) {
                tMin = near
                if (isX) {
                    normalX = sign
                    normalY = 0f
                } else {
                    normalX = 0f
                    normalY = sign
                }
            }
            tMax = min(tMax, far)
            return tMin <= tMax
        }

        if (!axis(startX, dx, bounds.left, bounds.right, true)) return null
        if (!axis(startY, dy, bounds.top, bounds.bottom, false)) return null
        if (tMin !in 0f..1f) return null

        return RaycastHit(
            body = bodies.firstOrNull { it.gameObject.bounds() == bounds } ?: return null,
            pointX = startX + dx * tMin,
            pointY = startY + dy * tMin,
            normalX = normalX,
            normalY = normalY,
            fraction = tMin
        )
    }
}
