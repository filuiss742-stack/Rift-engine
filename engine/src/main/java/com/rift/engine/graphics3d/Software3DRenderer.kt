package com.rift.engine.graphics3d

import com.rift.engine.graphics.RenderContext
import kotlin.math.tan

data class MeshTriangle(
    val a: Vector3,
    val b: Vector3,
    val c: Vector3,
    val color: Int
)

data class Mesh3D(val triangles: List<MeshTriangle>)

data class ScreenTriangle(
    val ax: Float, val ay: Float,
    val bx: Float, val by: Float,
    val cx: Float, val cy: Float,
    val depth: Float,
    val color: Int
)

/**
 * Deterministic CPU triangle renderer for the first hybrid-world vertical slice.
 * Geometry is projected from real 3D vertices with perspective; it is not a
 * collection of pre-rendered room/background images. Painter sorting is used
 * until the platform backends expose a depth-buffered GPU pipeline.
 */
class Software3DRenderer(
    private val nearPlane: Float = 0.08f
) {
    init {
        require(nearPlane.isFinite() && nearPlane > 0f)
    }

    fun project(
        point: Vector3,
        camera: Camera3D,
        viewportWidth: Int,
        viewportHeight: Int
    ): Pair<Float, Float>? {
        require(viewportWidth > 0 && viewportHeight > 0)
        val forward = (camera.target - camera.position).normalized()
        val right = forward.cross(camera.up).normalized()
        val cameraUp = right.cross(forward).normalized()
        val relative = point - camera.position
        val depth = relative.dot(forward)
        if (depth < nearPlane) return null
        val focal = (viewportHeight * 0.5f) / tan(camera.verticalFovRadians * 0.5f)
        val sx = viewportWidth * 0.5f + relative.dot(right) * focal / depth
        val sy = viewportHeight * 0.5f - relative.dot(cameraUp) * focal / depth
        if (!sx.isFinite() || !sy.isFinite()) return null
        return sx to sy
    }

    fun render(
        context: RenderContext,
        camera: Camera3D,
        meshes: Iterable<Mesh3D>
    ) {
        val forward = (camera.target - camera.position).normalized()
        val triangles = ArrayList<ScreenTriangle>()
        meshes.forEach { mesh ->
            mesh.triangles.forEach { tri ->
                val pa = project(tri.a, camera, context.width, context.height) ?: return@forEach
                val pb = project(tri.b, camera, context.width, context.height) ?: return@forEach
                val pc = project(tri.c, camera, context.width, context.height) ?: return@forEach
                val depth = ((tri.a - camera.position).dot(forward) +
                    (tri.b - camera.position).dot(forward) +
                    (tri.c - camera.position).dot(forward)) / 3f
                triangles += ScreenTriangle(
                    pa.first, pa.second, pb.first, pb.second, pc.first, pc.second,
                    depth, tri.color
                )
            }
        }
        triangles.sortByDescending { it.depth }
        triangles.forEach {
            context.drawTriangle(it.ax, it.ay, it.bx, it.by, it.cx, it.cy, it.color)
        }
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
