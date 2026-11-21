package com.rift.engine.core

import android.content.Context
import android.graphics.Canvas
import com.rift.engine.plugin.PluginManager
import com.rift.engine.storage.SaveStore

class GameEngine(context: Context) {
    val saves = SaveStore(context.applicationContext)
    val plugins = PluginManager()
    val scenes = SceneManager()

    fun update(deltaSeconds: Float) {
        plugins.onUpdate(deltaSeconds)
        scenes.update(deltaSeconds)
    }

    fun render(canvas: Canvas) {
        scenes.render(canvas)
    }

    fun onTouch(x: Float, y: Float, screenWidth: Float, screenHeight: Float) {
        scenes.onTouch(x, y, screenWidth, screenHeight)
    }
}
