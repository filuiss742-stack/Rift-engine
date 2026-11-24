package com.rift.engine.graphics3d

import com.rift.engine.graphics.RenderContext
import com.rift.engine.graphics.TextureResource
import kotlin.math.tan

data class TextureUv(val u: Float, val v: Float) {
    init { require(u.isFinite() && v.isFinite()) { "Texture UV coordinates must be finite." } }
}

data class MeshTriangle(
    val a: Vector3,
    val b: Vector3,
    val c: Vector3,
    val color: Int,
    val texture: TextureResource? = null,
    val uvA: TextureUv = TextureUv(0f, 0f),
    val uvB: TextureUv = TextureUv(1f, 0f),
    val uvC: TextureUv = TextureUv(1f, 1f)
)

data class Mesh3D(val triangles: List<MeshTriangle>)

data class ScreenTriangle(
    val ax: Float, val ay: Float,
    val bx: Float, val by: Float,
    val cx: Float, val cy: Float,
    val depth: Float,
    val color: Int,
    val texture: TextureResource?,
    val uvA: TextureUv,
    val uvB: TextureUv,
    val uvC: TextureUv
)

/**
 * Deterministic CPU triangle renderer for the first hybrid-world vertical slice.
 *
 * Triangles are clipped in camera space against both camera planes before
 * projection. This prevents an entire polygon disappearing when only one
 * vertex crosses the near plane and avoids projecting geometry beyond far.
 * Painter sorting remains an approximation until a depth-buffered backend is
 * available; intersecting opaque geometry can still sort incorrectly.
 */
class Software3DRenderer(
    private val nearPlane: Float = 0.08f
) {
    init {
        require(nearPlane.isFinite() && nearPlane > 0f) {
            "Renderer near plane must be finite and positive."
        }
    }

    private data class CameraVertex(
        val x: Float,
        val y: Float,
        val depth: Float,
        val uv: TextureUv
    )

    private data class ProjectedPoint(val x: Float, val y: Float)

    fun project(
        point: Vector3,
        camera: Camera3D,
        viewportWidth: Int,
        viewportHeight: Int
    ): Pair<Float, Float>? {
        require(viewportWidth > 0 && viewportHeight > 0)
        val basis = cameraBasis(camera)
        val relative = point - camera.position
        val depth = relative.dot(basis.forward)
        if (depth < effectiveNear(camera) || depth > camera.far) return null
        return projectCameraPoint(
            relative.dot(basis.right),
            relative.dot(basis.up),
            depth,
            camera,
            viewportWidth,
            viewportHeight
        )?.let { it.x to it.y }
    }

    fun render(
        context: RenderContext,
        camera: Camera3D,
        meshes: Iterable<Mesh3D>
    ) {
        require(context.width > 0 && context.height > 0) {
            "RenderContext viewport dimensions must be positive."
        }
        val basis = cameraBasis(camera)
        val triangles = ArrayList<ScreenTriangle>()

        for (mesh in meshes) {
            for (tri in mesh.triangles) {
                val cameraPolygon = listOf(
                    toCameraVertex(tri.a, tri.uvA, camera, basis),
                    toCameraVertex(tri.b, tri.uvB, camera, basis),
                    toCameraVertex(tri.c, tri.uvC, camera, basis)
                )
                val bounded = clipToFrustum(cameraPolygon, camera)
                if (bounded.size < 3) continue

                val projected = bounded.mapNotNull { vertex ->
                    projectCameraPoint(
                        vertex.x, vertex.y, vertex.depth, camera, context.width, context.height
                    )?.let { point ->
                        ProjectedVertexWithUv(point.x, point.y, vertex.depth, vertex.uv)
                    }
                }
                if (projected.size < 3) continue

                // Clipping a triangle against two parallel planes produces a
                // convex polygon. A fan preserves its coverage without gaps.
                for (index in 1 until projected.lastIndex) {
                    val a = projected[0]
                    val b = projected[index]
                    val c = projected[index + 1]
                    triangles += ScreenTriangle(
                        a.x, a.y, b.x, b.y, c.x, c.y,
                        (a.depth + b.depth + c.depth) / 3f,
                        tri.color, tri.texture, a.uv, b.uv, c.uv
                    )
                }
            }
        }

        triangles.sortByDescending { it.depth }
        triangles.forEach { triangle ->
            val texture = triangle.texture
            if (texture == null) {
                context.drawTriangle(
                    triangle.ax, triangle.ay, triangle.bx, triangle.by,
                    triangle.cx, triangle.cy, triangle.color
                )
            } else {
                context.drawTexturedTriangle(
                    texture,
                    triangle.ax, triangle.ay, triangle.uvA.u, triangle.uvA.v,
                    triangle.bx, triangle.by, triangle.uvB.u, triangle.uvB.v,
                    triangle.cx, triangle.cy, triangle.uvC.u, triangle.uvC.v
                )
            }
        }
    }

    private data class ProjectedVertexWithUv(
        val x: Float,
        val y: Float,
        val depth: Float,
        val uv: TextureUv
    )

    private data class CameraBasis(
        val right: Vector3,
        val up: Vector3,
        val forward: Vector3
    )

    private fun cameraBasis(camera: Camera3D): CameraBasis {
        val forward = (camera.target - camera.position).normalized()
        val right = forward.cross(camera.up).normalized()
        val up = right.cross(forward).normalized()
        return CameraBasis(right, up, forward)
    }

    private fun toCameraVertex(
        point: Vector3,
        uv: TextureUv,
        camera: Camera3D,
        basis: CameraBasis
    ): CameraVertex {
        val relative = point - camera.position
        return CameraVertex(
            relative.dot(basis.right),
            relative.dot(basis.up),
            relative.dot(basis.forward),
            uv
        )
    }

    /**
     * Clips a polygon against all six perspective-frustum planes in camera
     * space. UVs are interpolated at every generated intersection.
     */
    private fun clipToFrustum(
        input: List<CameraVertex>,
        camera: Camera3D
    ): List<CameraVertex> {
        val halfVertical = kotlin.math.tan(camera.verticalFovRadians * 0.5f)
        val halfHorizontal = halfVertical * camera.aspectRatio
        var polygon = clipAgainstPlane(input) { it.depth - effectiveNear(camera) }
        polygon = clipAgainstPlane(polygon) { camera.far - it.depth }
        polygon = clipAgainstPlane(polygon) { it.x + it.depth * halfHorizontal }
        polygon = clipAgainstPlane(polygon) { it.depth * halfHorizontal - it.x }
        polygon = clipAgainstPlane(polygon) { it.y + it.depth * halfVertical }
        polygon = clipAgainstPlane(polygon) { it.depth * halfVertical - it.y }
        return polygon
    }

    /** Sutherland-Hodgman clipping; non-negative signed distance is inside. */
    private fun clipAgainstPlane(
        polygon: List<CameraVertex>,
        signedDistance: (CameraVertex) -> Float
    ): List<CameraVertex> {
        if (polygon.isEmpty()) return emptyList()
        val output = ArrayList<CameraVertex>(polygon.size + 2)
        var previous = polygon.last()
        var previousDistance = signedDistance(previous)
        var previousInside = previousDistance >= 0f

        for (current in polygon) {
            val currentDistance = signedDistance(current)
            val currentInside = currentDistance >= 0f
            if (currentInside != previousInside) {
                val denominator = previousDistance - currentDistance
                if (kotlin.math.abs(denominator) > CLIP_EPSILON) {
                    val t = (previousDistance / denominator).coerceIn(0f, 1f)
                    output += interpolate(previous, current, t)
                }
            }
            if (currentInside) output += current
            previous = current
            previousDistance = currentDistance
            previousInside = currentInside
        }
        return output
    }

    private fun interpolate(
        from: CameraVertex,
        to: CameraVertex,
        t: Float
    ): CameraVertex = CameraVertex(
        from.x + (to.x - from.x) * t,
        from.y + (to.y - from.y) * t,
        from.depth + (to.depth - from.depth) * t,
        TextureUv(
            from.uv.u + (to.uv.u - from.uv.u) * t,
            from.uv.v + (to.uv.v - from.uv.v) * t
        )
    )

    private fun projectCameraPoint(
        cameraX: Float,
        cameraY: Float,
        depth: Float,
        camera: Camera3D,
        viewportWidth: Int,
        viewportHeight: Int
    ): ProjectedPoint? {
        val near = effectiveNear(camera)
        if (!depth.isFinite() ||
            depth < near - CLIP_EPSILON ||
            depth > camera.far + CLIP_EPSILON
        ) return null
        // Plane intersections can land a few ULPs outside the exact boundary.
        // Clamp only the projection denominator so clipped edge vertices survive.
        val safeDepth = depth.coerceIn(near, camera.far)
        val focal = (viewportHeight * 0.5f) / tan(camera.verticalFovRadians * 0.5f)
        val sx = viewportWidth * 0.5f + cameraX * focal / safeDepth
        val sy = viewportHeight * 0.5f - cameraY * focal / safeDepth
        if (!sx.isFinite() || !sy.isFinite()) return null
        return ProjectedPoint(sx, sy)
    }

    private fun effectiveNear(camera: Camera3D): Float = maxOf(camera.near, nearPlane)

    companion object {
        private const val CLIP_EPSILON = 1e-7f
    }
}

/** Creates a closed cuboid from actual world-space vertices. */
fun cuboidMesh(
    center: Vector3,
    size: Vector3,
    top: Int,
    sides: Int = top,
    bottom: Int = sides
): Mesh3D {
    require(size.x > 0f && size.y > 0f && size.z > 0f)
    val x0 = center.x - size.x * 0.5f
    val x1 = center.x + size.x * 0.5f
    val y0 = center.y - size.y * 0.5f
    val y1 = center.y + size.y * 0.5f
    val z0 = center.z - size.z * 0.5f
    val z1 = center.z + size.z * 0.5f
    val p000 = Vector3(x0,y0,z0); val p001 = Vector3(x0,y0,z1)
    val p010 = Vector3(x0,y1,z0); val p011 = Vector3(x0,y1,z1)
    val p100 = Vector3(x1,y0,z0); val p101 = Vector3(x1,y0,z1)
    val p110 = Vector3(x1,y1,z0); val p111 = Vector3(x1,y1,z1)
    val out = mutableListOf<MeshTriangle>()
    fun face(a: Vector3,b: Vector3,c: Vector3,d: Vector3,color: Int) {
        out += MeshTriangle(a,b,c,color); out += MeshTriangle(a,c,d,color)
    }
    face(p010,p110,p111,p011,top)
    face(p000,p001,p101,p100,bottom)
    face(p000,p010,p011,p001,sides)
    face(p100,p101,p111,p110,sides)
    face(p001,p011,p111,p101,sides)
    face(p000,p100,p110,p010,sides)
    return Mesh3D(out)
}

/** Flat rectangular surface made from two triangles in the X/Z ground plane. */
fun groundPlaneMesh(
    center: Vector3,
    width: Float,
    depth: Float,
    color: Int
): Mesh3D {
    require(width > 0f && depth > 0f)
    val x0 = center.x - width * .5f
    val x1 = center.x + width * .5f
    val z0 = center.z - depth * .5f
    val z1 = center.z + depth * .5f
    val y = center.y
    val a = Vector3(x0,y,z0); val b = Vector3(x1,y,z0)
    val c = Vector3(x1,y,z1); val d = Vector3(x0,y,z1)
    return Mesh3D(listOf(MeshTriangle(a,b,c,color), MeshTriangle(a,c,d,color)))
}
