package com.rift.engine

import android.app.Activity
import android.os.Bundle
import com.rift.engine.core.RiftSurfaceView

class MainActivity : Activity() {
    private lateinit var gameView: RiftSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameView = RiftSurfaceView(this)
        setContentView(gameView)
    }

    override fun onPause() {
        gameView.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        gameView.resume()
    }

    override fun onDestroy() {
        gameView.stop()
        super.onDestroy()
    }
}
