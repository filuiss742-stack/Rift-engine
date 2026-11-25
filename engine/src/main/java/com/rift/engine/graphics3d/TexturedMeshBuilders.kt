package com.rift.engine.graphics3d

import com.rift.engine.graphics.TextureResource

/**
 * Independent texture assignment for each cuboid face. Reusing a material
 * handle is intentional only when the physical material is actually shared.
 */
data class CuboidMaterialSet(
    val top: TextureResource,
    val bottom: TextureResource,
    val negativeX: TextureResource,
    val positiveX: TextureResource,
    val negativeZ: TextureResource,
    val positiveZ: TextureResource
)

/** Builds a six-face cuboid with independent UV-mapped material bindings. */
fun texturedCuboidMesh(
    center: Vector3,
    size: Vector3,
    materials: CuboidMaterialSet,
    uRepeat: Float = 1f,
    vRepeat: Float = 1f
): Mesh3D {
    require(size.x > 0f && size.y > 0f && size.z > 0f)
    require(uRepeat.isFinite() && vRepeat.isFinite() && uRepeat > 0f && vRepeat > 0f)
    val x0 = center.x - size.x * .5f
    val x1 = center.x + size.x * .5f
    val y0 = center.y - size.y * .5f
    val y1 = center.y + size.y * .5f
    val z0 = center.z - size.z * .5f
    val z1 = center.z + size.z * .5f
    val p000 = Vector3(x0,y0,z0); val p001 = Vector3(x0,y0,z1)
    val p010 = Vector3(x0,y1,z0); val p011 = Vector3(x0,y1,z1)
    val p100 = Vector3(x1,y0,z0); val p101 = Vector3(x1,y0,z1)
    val p110 = Vector3(x1,y1,z0); val p111 = Vector3(x1,y1,z1)
    val out = mutableListOf<MeshTriangle>()

    fun face(a: Vector3, b: Vector3, c: Vector3, d: Vector3, texture: TextureResource) {
        out += MeshTriangle(
            a, b, c, 0xFFFFFFFF.toInt(), texture,
            TextureUv(0f,0f), TextureUv(uRepeat,0f), TextureUv(uRepeat,vRepeat)
        )
        out += MeshTriangle(
            a, c, d, 0xFFFFFFFF.toInt(), texture,
            TextureUv(0f,0f), TextureUv(uRepeat,vRepeat), TextureUv(0f,vRepeat)
        )
    }

    face(p010,p110,p111,p011,materials.top)
    face(p000,p001,p101,p100,materials.bottom)
    face(p000,p010,p011,p001,materials.negativeX)
    face(p100,p101,p111,p110,materials.positiveX)
    face(p001,p011,p111,p101,materials.positiveZ)
    face(p000,p100,p110,p010,materials.negativeZ)
    return Mesh3D(out)
}

/** A textured ground surface with repeat-ready UV coordinates. */
fun texturedGroundPlaneMesh(
    center: Vector3,
    width: Float,
    depth: Float,
    texture: TextureResource,
    uRepeat: Float = 1f,
    vRepeat: Float = 1f
): Mesh3D {
    require(width > 0f && depth > 0f)
    require(uRepeat.isFinite() && vRepeat.isFinite() && uRepeat > 0f && vRepeat > 0f)
    check(!texture.isDisposed) { "Cannot build a mesh from a disposed texture." }
    val x0 = center.x - width * .5f
    val x1 = center.x + width * .5f
    val z0 = center.z - depth * .5f
    val z1 = center.z + depth * .5f
    val y = center.y
    val a = Vector3(x0,y,z0)
    val b = Vector3(x1,y,z0)
    val c = Vector3(x1,y,z1)
    val d = Vector3(x0,y,z1)
    val uvA = TextureUv(0f,0f)
    val uvB = TextureUv(uRepeat,0f)
    val uvC = TextureUv(uRepeat,vRepeat)
    val uvD = TextureUv(0f,vRepeat)
    return Mesh3D(listOf(
        MeshTriangle(a,b,c,0xFFFFFFFF.toInt(),texture,uvA,uvB,uvC),
        MeshTriangle(a,c,d,0xFFFFFFFF.toInt(),texture,uvA,uvC,uvD)
    ))
}
