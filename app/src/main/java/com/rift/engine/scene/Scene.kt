package com.rift.engine.scene

import android.graphics.Canvas
import com.rift.engine.entity.GameObject

abstract class Scene {
    protected val objects = mutableListOf<GameObject>()

    protected fun add(gameObject: GameObject) {
        objects += gameObject
    }

    open fun onEnter() = Unit
    open fun onExit() = Unit

    open fun update(deltaSeconds: Float) {
        objects.toList().forEach { it.update(deltaSeconds) }
    }

    open fun render(canvas: Canvas) {
        objects.forEach { it.render(canvas) }
    }

    open fun onTouch(x: Float, y: Float, screenWidth: Float, screenHeight: Float) = Unit
}
