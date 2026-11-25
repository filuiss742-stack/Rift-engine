package com.rift.engine

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.FrameMetrics
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File
import java.util.Collections
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrologueLaunchSmokeTest {
    @Test
    fun launchesRendersAndRecordsFrameDiagnostics() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val framesNanos = Collections.synchronizedList(mutableListOf<Long>())
        var frameListenerInstalled = false

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull("Main activity must have a content view", activity.window.decorView)
                val contentRoot = activity.findViewById<ViewGroup>(android.R.id.content)
                assertTrue("The game view must be attached to the Activity", contentRoot.childCount > 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    activity.window.addOnFrameMetricsAvailableListener(
                        { _, metrics, _ ->
                            val duration = metrics.getMetric(FrameMetrics.TOTAL_DURATION)
                            if (duration > 0L) framesNanos.add(duration)
                        },
                        Handler(Looper.getMainLooper())
                    )
                    frameListenerInstalled = true
                }
            }

            // Allow the actual game loop and SurfaceView to render, not just the Activity shell.
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val gameView = activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
                assertTrue("Game surface must be visible", gameView.isShown)
                assertTrue("Game surface must have a measured width", gameView.width > 0)
                assertTrue("Game surface must have a measured height", gameView.height > 0)
            }
            Thread.sleep(12_000L)
            instrumentation.waitForIdleSync()

            val screenshotDir = File(context.getExternalFilesDir(null), "rift-autotest")
            assertTrue("Could not create screenshot directory", screenshotDir.mkdirs() || screenshotDir.isDirectory)
            val screenshot = File(screenshotDir, "prologue-launch.png")
            assertTrue("Could not capture emulator screenshot", UiDevice.getInstance(instrumentation).takeScreenshot(screenshot))
            assertTrue("Screenshot is empty", screenshot.length() > 10_000L)

            val samples = synchronized(framesNanos) { framesNanos.toList().sorted() }
            if (samples.isNotEmpty()) {
                val totalSeconds = samples.sum().toDouble() / 1_000_000_000.0
                val averageFps = if (totalSeconds > 0.0) samples.size / totalSeconds else 0.0
                val p95 = samples[((samples.size - 1) * 95) / 100] / 1_000_000.0
                val slowFrames = samples.count { it > 16_666_667L }
                Log.i(
                    "RiftAutoTest",
                    "window_frames=${samples.size} average_fps=${"%.1f".format(averageFps)} " +
                        "p95_frame_ms=${"%.2f".format(p95)} over_16_7ms=${slowFrames} " +
                        "listener_installed=${frameListenerInstalled}"
                )
            } else {
                Log.w("RiftAutoTest", "No Activity Window frame metrics collected; SurfaceView rendering may not be represented")
            }
            Log.i("RiftAutoTest", "SCREENSHOT_PATH=${screenshot.absolutePath}")
            scenario.onActivity { activity ->
                assertTrue("Activity should remain alive during the smoke test", !activity.isFinishing && !activity.isDestroyed)
            }
        }
    }
}
