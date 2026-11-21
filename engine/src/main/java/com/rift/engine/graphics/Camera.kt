package com.rift.engine.graphics

class Camera(var x: Float = 0f, var y: Float = 0f) {
    fun worldToScreenX(worldX: Float): Float = worldX - x
    fun worldToScreenY(worldY: Float): Float = worldY - y
    fun screenToWorldX(screenX: Float): Float = screenX + x
    fun screenToWorldY(screenY: Float): Float = screenY + y
}
