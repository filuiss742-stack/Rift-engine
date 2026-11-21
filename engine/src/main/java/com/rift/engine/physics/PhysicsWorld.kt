package com.rift.engine.physics

import com.rift.engine.entity.GameObject
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

enum class PhysicsBodyType {
    STATIC,
    KINEMATIC,
    DYNAMIC
}

sealed interface CollisionShape {
    data object Box : CollisionShape

    data class Circle(
        val radius: Float,
        val offsetX: Float = 0f,
        val offsetY: Float = 0f
    ) : CollisionShape {
        init {
            require(radius.isFinite() && radius > 0f) {
                "Circle radius must be finite and greater than zero."
            }
            require(offsetX.isFinite() && offsetY.isFinite()) {
                "Circle offsets must be finite."
            }
        }
    }
}

/** Collision category and mask filtering. Bits 0..30 are available. */
data class CollisionFilter(
    val categoryBits: Int = 1,
    val maskBits: Int = Int.MAX_VALUE,
    val groupIndex: Int = 0
) {
    init {
        require(categoryBits > 0) { "categoryBits must use one or more of bits 0..30." }
        require(maskBits >= 0) { "maskBits must use only bits 0..30." }
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
    collisionFilter: CollisionFilter,
    val shape: CollisionShape
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
 * Deterministic 2D box/circle physics with a spatial-hash broad phase.
 *
 * Candidate pairs are deduplicated and processed in stable body-registration
 * order. Very large bodies that span too many grid cells fall back to checking
 * against all bodies so they cannot disappear from collision detection.
 */
class PhysicsWorld(
    gravityX: Float = 0f,
    gravityY: Float = 980f,
    private val maxSubstepDistance: Float = 16f,
    private val maxSubsteps: Int = 16,
    private val solverIterations: Int = 16,
    private val broadPhaseCellSize: Float = 128f,
    private val maxCellsPerBody: Int = 256
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

    var lastBroadPhaseCandidateCount: Int = 0
        private set

    init {
        require(gravityX.isFinite() && gravityY.isFinite()) {
            "Gravity must be finite."
        }
        require(maxSubstepDistance.isFinite() && maxSubstepDistance > 0f) {
            "maxSubstepDistance must be finite and greater than zero."
        }
        require(maxSubsteps > 0) { "maxSubsteps must be greater than zero." }
        require(solverIterations > 0) { "solverIterations must be greater than zero." }
        require(broadPhaseCellSize.isFinite() && broadPhaseCellSize > 0f) {
            "broadPhaseCellSize must be finite and greater than zero."
        }
        require(maxCellsPerBody > 0) { "maxCellsPerBody must be greater than zero." }
    }

    fun addBody(
        gameObject: GameObject,
        type: PhysicsBodyType = PhysicsBodyType.DYNAMIC,
        mass: Float = 1f,
        gravityScale: Float = 1f,
        isSensor: Boolean = false,
        collisionFilter: CollisionFilter = CollisionFilter(),
        shape: CollisionShape = CollisionShape.Box
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
            collisionFilter,
            shape
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
            lastBroadPhaseCandidateCount = 0
            return
        }

        val activeBodies = bodies.filter { it.enabled && it.gameObject.active }
        if (activeBodies.isEmpty()) {
            currentContacts = emptyList()
            lastBroadPhaseCandidateCount = 0
            return
        }

        activeBodies.forEach { body ->
            if (body.type == PhysicsBodyType.DYNAMIC) {
                body.gameObject.isGrounded = false
            }
        }

        val maxDistance = activeBodies
            .filter { it.type == PhysicsBodyType.DYNAMIC }
            .maxOfOrNull { body ->
                val gameObject = body.gameObject
                val vx = abs(gameObject.velocityX + gravityX * body.gravityScale * deltaSeconds)
                val vy = abs(gameObject.velocityY + gravityY * body.gravityScale * deltaSeconds)
                max(vx, vy) * deltaSeconds
            } ?: 0f

        val minimumDynamicExtent = activeBodies
            .filter { it.type == PhysicsBodyType.DYNAMIC }
            .minOfOrNull { body ->
                when (val shape = body.shape) {
                    CollisionShape.Box -> min(body.gameObject.width, body.gameObject.height) *
                        min(abs(body.gameObject.scaleX), abs(body.gameObject.scaleY))
                    is CollisionShape.Circle -> shape.radius * 2f *
                        min(abs(body.gameObject.scaleX), abs(body.gameObject.scaleY))
                }
            } ?: maxSubstepDistance
        val safeSubstepDistance = min(
            maxSubstepDistance,
            (minimumDynamicExtent * 0.5f).coerceAtLeast(1f)
        )
        val substeps = ceil(maxDistance / safeSubstepDistance)
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

            val candidates = candidatePairs(activeBodies)
            lastBroadPhaseCandidateCount = candidates.size
            repeat(solverIterations) {
                candidates.forEach { (firstIndex, secondIndex) ->
                    val first = activeBodies[firstIndex]
                    val second = activeBodies[secondIndex]
                    if (first.collisionFilter.allows(second.collisionFilter)) {
                        val contact = detectContact(first, second)
                        if (contact != null) {
                            contactsByPair[first to second] = contact
                            if (!contact.isSensor) resolve(contact)
                        }
                    }
                }
            }
        }

        currentContacts = contactsByPair.values.toList()
    }

    fun queryAabb(
        area: Aabb,
        filter: CollisionFilter? = null
    ): List<PhysicsBody> {
        return bodies.filter { body ->
            body.enabled && body.gameObject.active &&
                (filter == null || filter.allows(body.collisionFilter)) &&
                Collision.intersects(area, bodyBounds(body))
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
            if (!body.enabled || !body.gameObject.active ||
                (filter != null && !filter.allows(body.collisionFilter))
            ) {
                return@forEach
            }
            val hit = when (val shape = body.shape) {
                CollisionShape.Box -> intersectSegment(
                    body,
                    bodyBounds(body),
                    startX,
                    startY,
                    dx,
                    dy
                )
                is CollisionShape.Circle -> intersectCircle(
                    body,
                    shape,
                    startX,
                    startY,
                    dx,
                    dy
                )
            } ?: return@forEach
            if (closest == null || hit.fraction < closest!!.fraction) {
                closest = hit
            }
        }
        return closest
    }

    private fun candidatePairs(activeBodies: List<PhysicsBody>): List<Pair<Int, Int>> {
        val buckets = linkedMapOf<Long, MutableList<Int>>()
        val oversized = mutableListOf<Int>()

        activeBodies.forEachIndexed { index, body ->
            val bounds = bodyBounds(body)
            val minCellX = floor(bounds.left / broadPhaseCellSize).toInt()
            val maxCellX = floor(bounds.right / broadPhaseCellSize).toInt()
            val minCellY = floor(bounds.top / broadPhaseCellSize).toInt()
            val maxCellY = floor(bounds.bottom / broadPhaseCellSize).toInt()
            val cellsX = maxCellX.toLong() - minCellX.toLong() + 1L
            val cellsY = maxCellY.toLong() - minCellY.toLong() + 1L
            val tooManyCells = cellsX > maxCellsPerBody ||
                cellsY > maxCellsPerBody ||
                cellsX * cellsY > maxCellsPerBody.toLong()

            if (tooManyCells) {
                oversized += index
            } else {
                for (cellX in minCellX..maxCellX) {
                    for (cellY in minCellY..maxCellY) {
                        val key = (cellX.toLong() shl 32) xor (cellY.toLong() and 0xffffffffL)
                        buckets.getOrPut(key) { mutableListOf() }.add(index)
                    }
                }
            }
        }

        val pairs = linkedSetOf<Long>()
        fun addPair(first: Int, second: Int) {
            if (first == second) return
            val low = min(first, second)
            val high = max(first, second)
            pairs.add((low.toLong() shl 32) or (high.toLong() and 0xffffffffL))
        }

        buckets.values.forEach { indices ->
            var i = 0
            while (i < indices.size) {
                var j = i + 1
                while (j < indices.size) {
                    addPair(indices[i], indices[j])
                    j += 1
                }
                i += 1
            }
        }

        oversized.forEach { largeIndex ->
            activeBodies.indices.forEach { otherIndex ->
                addPair(largeIndex, otherIndex)
            }
        }

        return pairs.sorted().map { key ->
            (key ushr 32).toInt() to key.toInt()
        }
    }

    private fun detectContact(
        first: PhysicsBody,
        second: PhysicsBody
    ): CollisionContact? {
        if (first.inverseMass == 0f && second.inverseMass == 0f) return null
        val sensor = first.isSensor || second.isSensor
        return when (val firstShape = first.shape) {
            CollisionShape.Box -> when (val secondShape = second.shape) {
                CollisionShape.Box -> boxBoxContact(first, second, sensor)
                is CollisionShape.Circle -> circleBoxContact(second, first, secondShape, sensor, circleIsFirst = false)
            }
            is CollisionShape.Circle -> when (val secondShape = second.shape) {
                CollisionShape.Box -> circleBoxContact(first, second, firstShape, sensor, circleIsFirst = true)
                is CollisionShape.Circle -> circleCircleContact(first, second, firstShape, secondShape, sensor)
            }
        }
    }

    private fun boxBoxContact(
        first: PhysicsBody,
        second: PhysicsBody,
        sensor: Boolean
    ): CollisionContact? {
        val a = bodyBounds(first)
        val b = bodyBounds(second)
        if (!Collision.intersects(a, b)) return null

        val overlapX = min(a.right, b.right) - max(a.left, b.left)
        val overlapY = min(a.bottom, b.bottom) - max(a.top, b.top)
        val centerAX = (a.left + a.right) * 0.5f
        val centerAY = (a.top + a.bottom) * 0.5f
        val centerBX = (b.left + b.right) * 0.5f
        val centerBY = (b.top + b.bottom) * 0.5f

        return if (overlapX < overlapY) {
            CollisionContact(first, second, if (centerBX >= centerAX) 1f else -1f, 0f, overlapX, sensor)
        } else {
            CollisionContact(first, second, 0f, if (centerBY >= centerAY) 1f else -1f, overlapY, sensor)
        }
    }

    private fun circleCircleContact(
        first: PhysicsBody,
        second: PhysicsBody,
        firstShape: CollisionShape.Circle,
        secondShape: CollisionShape.Circle,
        sensor: Boolean
    ): CollisionContact? {
        val firstCenter = circleCenter(first, firstShape)
        val secondCenter = circleCenter(second, secondShape)
        val dx = secondCenter.first - firstCenter.first
        val dy = secondCenter.second - firstCenter.second
        val distanceSquared = dx * dx + dy * dy
        val radiusSum = circleRadius(first, firstShape) + circleRadius(second, secondShape)
        if (distanceSquared >= radiusSum * radiusSum) return null
        val distance = kotlin.math.sqrt(distanceSquared)
        val normalX = if (distance > 0.000001f) dx / distance else 1f
        val normalY = if (distance > 0.000001f) dy / distance else 0f
        return CollisionContact(first, second, normalX, normalY, radiusSum - distance, sensor)
    }

    private fun circleBoxContact(
        circleBody: PhysicsBody,
        boxBody: PhysicsBody,
        circleShape: CollisionShape.Circle,
        sensor: Boolean,
        circleIsFirst: Boolean
    ): CollisionContact? {
        val center = circleCenter(circleBody, circleShape)
        val radius = circleRadius(circleBody, circleShape)
        val box = bodyBounds(boxBody)
        val closestX = center.first.coerceIn(box.left, box.right)
        val closestY = center.second.coerceIn(box.top, box.bottom)
        val dx = closestX - center.first
        val dy = closestY - center.second
        val distanceSquared = dx * dx + dy * dy

        val normalFromCircleX: Float
        val normalFromCircleY: Float
        val penetration: Float

        if (distanceSquared > 0.000001f) {
            val distance = kotlin.math.sqrt(distanceSquared)
            if (distance >= radius) return null
            normalFromCircleX = dx / distance
            normalFromCircleY = dy / distance
            penetration = radius - distance
        } else {
            val nearest = listOf(
                abs(center.first - box.left) to (1f to 0f),
                abs(box.right - center.first) to (-1f to 0f),
                abs(center.second - box.top) to (0f to 1f),
                abs(box.bottom - center.second) to (0f to -1f)
            ).minBy { it.first }
            normalFromCircleX = nearest.second.first
            normalFromCircleY = nearest.second.second
            penetration = radius + nearest.first
        }

        return if (circleIsFirst) {
            CollisionContact(
                circleBody,
                boxBody,
                normalFromCircleX,
                normalFromCircleY,
                penetration,
                sensor
            )
        } else {
            CollisionContact(
                boxBody,
                circleBody,
                -normalFromCircleX,
                -normalFromCircleY,
                penetration,
                sensor
            )
        }
    }

    private fun circleCenter(
        body: PhysicsBody,
        shape: CollisionShape.Circle
    ): Pair<Float, Float> {
        val objectUnderTest = body.gameObject
        val point = objectUnderTest.transform.transformPoint(
            objectUnderTest.width * 0.5f + shape.offsetX,
            objectUnderTest.height * 0.5f + shape.offsetY,
            objectUnderTest.x,
            objectUnderTest.y
        )
        return point.x to point.y
    }

    private fun circleRadius(
        body: PhysicsBody,
        shape: CollisionShape.Circle
    ): Float = shape.radius * max(abs(body.gameObject.scaleX), abs(body.gameObject.scaleY))

    private fun bodyBounds(body: PhysicsBody): Aabb {
        return when (val shape = body.shape) {
            CollisionShape.Box -> body.gameObject.bounds()
            is CollisionShape.Circle -> {
                val center = circleCenter(body, shape)
                val radius = circleRadius(body, shape)
                Aabb(
                    center.first - radius,
                    center.second - radius,
                    center.first + radius,
                    center.second + radius
                )
            }
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

        val relativeVelocityX = second.gameObject.velocityX - first.gameObject.velocityX
        val relativeVelocityY = second.gameObject.velocityY - first.gameObject.velocityY
        val velocityAlongNormal =
            relativeVelocityX * contact.normalX + relativeVelocityY * contact.normalY
        if (velocityAlongNormal < 0f) {
            val impulse = -velocityAlongNormal / inverseMassTotal
            first.gameObject.velocityX -= impulse * first.inverseMass * contact.normalX
            first.gameObject.velocityY -= impulse * first.inverseMass * contact.normalY
            second.gameObject.velocityX += impulse * second.inverseMass * contact.normalX
            second.gameObject.velocityY += impulse * second.inverseMass * contact.normalY
        }

        if (contact.normalY > 0.5f && first.type == PhysicsBodyType.DYNAMIC) {
            first.gameObject.isGrounded = true
        } else if (contact.normalY < -0.5f && second.type == PhysicsBodyType.DYNAMIC) {
            second.gameObject.isGrounded = true
        }
    }

    private fun intersectCircle(
        body: PhysicsBody,
        shape: CollisionShape.Circle,
        startX: Float,
        startY: Float,
        dx: Float,
        dy: Float
    ): RaycastHit? {
        val center = circleCenter(body, shape)
        val radius = circleRadius(body, shape)
        val offsetX = startX - center.first
        val offsetY = startY - center.second
        val a = dx * dx + dy * dy
        if (a <= 0.000001f) return null
        val b = 2f * (offsetX * dx + offsetY * dy)
        val c = offsetX * offsetX + offsetY * offsetY - radius * radius
        val discriminant = b * b - 4f * a * c
        if (discriminant < 0f) return null
        val root = kotlin.math.sqrt(discriminant)
        val first = (-b - root) / (2f * a)
        val second = (-b + root) / (2f * a)
        val fraction = when {
            first in 0f..1f -> first
            second in 0f..1f -> second
            else -> return null
        }
        val pointX = startX + dx * fraction
        val pointY = startY + dy * fraction
        val normalX = (pointX - center.first) / radius
        val normalY = (pointY - center.second) / radius
        return RaycastHit(body, pointX, pointY, normalX, normalY, fraction)
    }

    private fun intersectSegment(
        body: PhysicsBody,
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
            body = body,
            pointX = startX + dx * tMin,
            pointY = startY + dy * tMin,
            normalX = normalX,
            normalY = normalY,
            fraction = tMin
        )
    }
}
