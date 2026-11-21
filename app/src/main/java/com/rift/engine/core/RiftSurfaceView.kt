package com.rift.engine.core

import android.content.Context
import android.graphics.Canvas
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.MotionEvent
import com.rift.engine.demo.SandboxScene

class RiftSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    private val engine = GameEngine(context)
    @Volatile private var running = false
    private var loopThread: Thread? = null

    init {
        holder.addCallback(this)
        isFocusable = true
        engine.scenes.setInitialScene(SandboxScene())
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        resume()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        pause()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    fun resume() {
        if (running) return
        running = true
        loopThread = Thread {
            var previous = System.nanoTime()
            while (running) {
                val now = System.nanoTime()
                val delta = ((now - previous) / 1_000_000_000.0).toFloat().coerceAtMost(0.05f)
                previous = now
                engine.update(delta)
                var canvas: Canvas? = null
                try {
                    if (holder.surface.isValid) {
                        canvas = holder.lockCanvas()
                        if (canvas != null) engine.render(canvas)
                    }
                } finally {
                    if (canvas != null) holder.unlockCanvasAndPost(canvas)
                }
                try {
                    Thread.sleep(2)
                } catch (_: InterruptedException) {
                    // Loop is stopping.
                }
            }
        }.also { it.name = "RiftEngine-GameLoop"; it.start() }
    }

    fun pause() {
        running = false
        loopThread?.join(500)
        loopThread = null
    }

    fun stop() = pause()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            engine.onTouch(event.x, event.y, width.toFloat(), height.toFloat())
        }
        return true
    }
}
