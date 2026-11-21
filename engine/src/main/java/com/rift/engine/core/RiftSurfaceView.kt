package com.rift.engine.core

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.rift.engine.scene.Scene

/**
 * Android rendering host for a game supplied by the application.
 * The engine module does not select or depend on a demo scene.
 */
class RiftSurfaceView(
    context: Context,
    initialScene: Scene
) : SurfaceView(context), SurfaceHolder.Callback {
    private val engine = GameEngine(context)

    @Volatile
    private var running = false
    private var loopThread: Thread? = null

    init {
        holder.addCallback(this)
        isFocusable = true
        engine.scenes.setInitialScene(initialScene)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        resume()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        pause()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    fun resume() {
        synchronized(this) {
            if (running || loopThread?.isAlive == true) return

            running = true
            val thread = Thread({ runGameLoop() }, "RiftEngine-GameLoop")
            loopThread = thread
            thread.start()
        }
    }

    private fun runGameLoop() {
        var previous = System.nanoTime()

        while (running && !Thread.currentThread().isInterrupted) {
            try {
                val now = System.nanoTime()
                val delta = ((now - previous) / 1_000_000_000.0)
                    .toFloat()
                    .coerceAtMost(0.05f)
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

                Thread.sleep(2)
            } catch (_: InterruptedException) {
                break
            } catch (exception: Exception) {
                engine.diagnostics.reportException("gameLoop", exception)
            }
        }
    }

    fun pause() {
        val threadToJoin = synchronized(this) {
            running = false
            loopThread?.also { it.interrupt() }
        }

        if (threadToJoin != null && threadToJoin !== Thread.currentThread()) {
            try {
                threadToJoin.join(1_000)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        synchronized(this) {
            if (loopThread === threadToJoin && threadToJoin?.isAlive != true) {
                loopThread = null
            }
        }
    }

    fun stop() = pause()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            engine.onTouch(event.x, event.y, width.toFloat(), height.toFloat())
        }
        return true
    }
}
