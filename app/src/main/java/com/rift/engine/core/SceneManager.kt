package com.rift.engine.core

import android.graphics.Canvas

class SceneManager {
    var current: Scene? = null
        private set

    fun setInitialScene(scene: Scene) {
        if (current == null) {
            current = scene
            scene.onEnter()
        }
    }

    fun changeScene(scene: Scene) {
        current?.onExit()
        current = scene
        scene.onEnter()
    }

    fun update(deltaSeconds: Float) = current?.update(deltaSeconds) ?: Unit
    fun render(canvas: Canvas) = current?.render(canvas) ?: Unit

    fun onTouch(x: Float, y: Float, screenWidth: Float, screenHeight: Float) {
        current?.onTouch(x, y, screenWidth, screenHeight)
    }
}
