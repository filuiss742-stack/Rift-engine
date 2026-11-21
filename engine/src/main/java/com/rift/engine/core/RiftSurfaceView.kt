package com.rift.engine.core

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.rift.engine.scene.Scene
import java.util.concurrent.locks.LockSupport

/**
 * Android rendering host for a game supplied by the application.
 * The engine module does not select or depend on a demo scene.
 */
class RiftSurfaceView(
    context: Context,
    initialScene: Scene
) : SurfaceView(context), SurfaceHolder.Callback {
    private val engine = GameEngine(context)
    private val fixedStep = FixedStepAccumulator()

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

            fixedStep.reset()
            running = true
            val thread = Thread({ runGameLoop() }, "RiftEngine-GameLoop")
            loopThread = thread
            thread.start()
        }
    }

    private fun runGameLoop() {
        var previousFrameNanos = System.nanoTime()
        val targetFrameNanos = 1_000_000_000L / 60L

        while (running && !Thread.currentThread().isInterrupted) {
            val frameStartNanos = System.nanoTime()

            try {
                val frameDeltaSeconds = (
                    (frameStartNanos - previousFrameNanos) / 1_000_000_000.0
                    ).toFloat()
                previousFrameNanos = frameStartNanos

                fixedStep.advance(frameDeltaSeconds) { fixedDeltaSeconds ->
                    engine.update(fixedDeltaSeconds)
                }

                var canvas: Canvas? = null
                try {
                    if (holder.surface.isValid) {
                        canvas = holder.lockCanvas()
                        if (canvas != null) engine.render(canvas)
                    }
                } finally {
                    if (canvas != null) holder.unlockCanvasAndPost(canvas)
                }
            } catch (exception: Exception) {
                engine.diagnostics.reportException("gameLoop", exception)
            }

            val remainingNanos = targetFrameNanos - (System.nanoTime() - frameStartNanos)
            if (remainingNanos > 0L) {
                LockSupport.parkNanos(remainingNanos)
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
