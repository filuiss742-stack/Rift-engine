package com.rift.engine.diagnostics

import com.rift.engine.graphics.RenderContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsOverlayTest {
    @Test
    fun recoverableErrorsUseOverlayWithoutClearingGameplayFrame() {
        val reporter = DiagnosticReporter()
        reporter.report(
            DiagnosticSeverity.ERROR,
            "RIFT_TEST_ERROR",
            "test",
            "A recoverable issue occurred.",
            suggestion = "Check the test fixture."
        )
        val context = RecordingContext()

        DiagnosticsOverlay.render(context, reporter.snapshot())

        assertEquals(0, context.clearCount)
        assertEquals(1, context.roundRectCount)
        assertTrue(context.text.any { it.contains("RIFT_TEST_ERROR") })
        assertTrue(context.text.any { it.contains("Check the test fixture") })
    }

    @Test
    fun fatalErrorsUseBlockingDiagnosticScreen() {
        val reporter = DiagnosticReporter()
        reporter.report(
            DiagnosticSeverity.FATAL,
            "RIFT_FATAL_TEST",
            "test",
            "Runtime cannot continue."
        )
        val context = RecordingContext()

        DiagnosticsOverlay.render(context, reporter.snapshot(), blocking = true)

        assertEquals(1, context.clearCount)
        assertEquals(0, context.roundRectCount)
        assertTrue(context.text.any { it.contains("FATAL") })
    }

    private class RecordingContext : RenderContext {
        override val width = 480
        override val height = 320
        var clearCount = 0
        var roundRectCount = 0
        val text = mutableListOf<String>()

        override fun clear(color: Int) { clearCount += 1 }
        override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, color: Int) = Unit
        override fun drawRoundRect(left: Float, top: Float, right: Float, bottom: Float, radius: Float, color: Int) {
            roundRectCount += 1
        }
        override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, color: Int, strokeWidth: Float) = Unit
        override fun drawText(text: String, x: Float, y: Float, color: Int, size: Float, bold: Boolean) {
            this.text += text
        }
        override fun measureText(text: String, size: Float, bold: Boolean): Float = text.length * size * 0.5f
        override fun pushState() = Unit
        override fun popState() = Unit
        override fun translate(x: Float, y: Float) = Unit
        override fun rotate(degrees: Float) = Unit
        override fun scale(x: Float, y: Float) = Unit
        override fun clipRect(left: Float, top: Float, right: Float, bottom: Float) = Unit
    }
}
