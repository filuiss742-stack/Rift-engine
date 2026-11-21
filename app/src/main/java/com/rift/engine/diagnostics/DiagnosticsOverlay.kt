package com.rift.engine.diagnostics

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/**
 * Fallback screen shown when an engine operation fails.
 * It deliberately uses only Android Canvas so it can render without game assets.
 */
object DiagnosticsOverlay {
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 30f
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }
    private val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 190, 95)
        textSize = 20f
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(220, 225, 235)
        textSize = 17f
    }
    private val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(150, 160, 180)
        textSize = 14f
    }

    fun render(canvas: Canvas, entries: List<DiagnosticEntry>) {
        canvas.drawColor(Color.rgb(17, 20, 29))

        val left = 28f
        var y = 46f
        canvas.drawText("RIFT ENGINE  /  DIAGNOSTICS", left, y, titlePaint)
        y += 34f

        val error = entries.lastOrNull { it.severity == DiagnosticSeverity.ERROR }
        if (error != null) {
            canvas.drawText("${error.code}  •  ${error.subsystem}", left, y, headingPaint)
            y += 27f
            y = drawWrapped(canvas, error.message, left, y, canvas.width - left * 2f)
            if (!error.cause.isNullOrBlank()) {
                y += 6f
                canvas.drawText("Cause: ${error.cause}", left, y, mutedPaint)
                y += 20f
            }
            if (!error.location.isNullOrBlank()) {
                y += 4f
                y = drawWrapped(canvas, "Location: ${error.location}", left, y, canvas.width - left * 2f, mutedPaint)
            }
        } else {
            canvas.drawText("No runtime errors recorded.", left, y, bodyPaint)
            y += 26f
        }

        y += 18f
        canvas.drawText("RECENT EVENTS (${entries.size})", left, y, headingPaint)
        y += 25f
        entries.takeLast(5).asReversed().forEach { entry ->
            val line = "[${entry.severity}] ${entry.code}: ${entry.message}"
            y = drawWrapped(canvas, line, left, y, canvas.width - left * 2f, bodyPaint)
            y += 5f
        }

        canvas.drawText(
            "Fix the reported issue, then call engine.diagnostics.clear() to resume rendering.",
            left,
            (canvas.height - 22f).coerceAtLeast(y + 20f),
            mutedPaint
        )
    }

    private fun drawWrapped(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint = bodyPaint
    ): Float {
        if (maxWidth <= 0f) return startY
        var y = startY
        var line = StringBuilder()
        text.split(Regex("\\s+")).forEach { word ->
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (line.isNotEmpty() && paint.measureText(candidate) > maxWidth) {
                canvas.drawText(line.toString(), x, y, paint)
                y += paint.textSize + 5f
                line = StringBuilder(word)
            } else {
                line = StringBuilder(candidate)
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line.toString(), x, y, paint)
            y += paint.textSize + 5f
        }
        return y
    }
}
