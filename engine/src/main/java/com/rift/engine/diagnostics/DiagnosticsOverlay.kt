package com.rift.engine.diagnostics

import com.rift.engine.graphics.RenderContext

/**
 * Asset-free diagnostic presentation. Recoverable errors use a compact overlay
 * so the running game remains visible; fatal errors use a blocking screen.
 */
object DiagnosticsOverlay {
    fun render(
        context: RenderContext,
        entries: List<DiagnosticEntry>,
        blocking: Boolean = false
    ) {
        val white = 0xFFFFFFFF.toInt()
        val orange = 0xFFFFBE5F.toInt()
        val body = 0xFFDCE1EB.toInt()
        val latestError = entries.lastOrNull {
            it.severity == DiagnosticSeverity.ERROR ||
                it.severity == DiagnosticSeverity.FATAL
        }

        if (blocking) {
            context.clear(0xFF11141D.toInt())
        }

        val maxTextWidth = (context.width - 48f).coerceAtLeast(80f)
        val messageLines = latestError?.let {
            wrap(it.message, context, maxTextWidth, 16f)
        }.orEmpty()
        val suggestionLines = latestError?.suggestion?.let {
            wrap(it, context, maxTextWidth, 14f)
        }.orEmpty()
        val locationLines = latestError?.location?.let {
            wrap("Location: $it", context, maxTextWidth, 12f)
        }.orEmpty()
        val causeLines = latestError?.cause?.let {
            wrap("Cause: $it", context, maxTextWidth, 12f)
        }.orEmpty()
        val panelHeight = if (blocking) context.height.toFloat() else
            (48f + messageLines.size * 20f + suggestionLines.size * 18f +
                locationLines.size * 16f + causeLines.size * 16f + 28f)
                .coerceAtMost(context.height.toFloat() - 16f)

        if (!blocking) {
            context.drawRoundRect(
                8f,
                8f,
                (context.width - 8).coerceAtLeast(8).toFloat(),
                (8f + panelHeight).coerceAtMost(context.height.toFloat() - 8f),
                8f,
                0xE611141D.toInt()
            )
        }

        var y = if (blocking) 44f else 34f
        context.drawText(
            if (blocking) "RIFT ENGINE / FATAL DIAGNOSTIC" else "RIFT / DIAGNOSTICS",
            24f,
            y,
            white,
            if (blocking) 28f else 18f,
            true
        )
        y += if (blocking) 34f else 26f

        if (latestError != null) {
            context.drawText(
                "${latestError.severity} • ${latestError.code} • ${latestError.subsystem}",
                24f,
                y,
                orange,
                15f,
                true
            )
            y += 22f
            messageLines.forEach { line ->
                context.drawText(line, 24f, y, body, 16f)
                y += 20f
            }
            if (suggestionLines.isNotEmpty()) {
                suggestionLines.forEachIndexed { index, line ->
                    context.drawText(
                        if (index == 0) "Next step: $line" else line,
                        24f,
                        y,
                        orange,
                        14f
                    )
                    y += 18f
                }
            }
            (locationLines + causeLines).forEach { line ->
                context.drawText(line, 24f, y, body, 12f)
                y += 16f
            }
        } else {
            context.drawText("No runtime errors recorded.", 24f, y, body, 16f)
            y += 24f
        }

        context.drawText(
            "Recent events: ${entries.size}",
            24f,
            y + 4f,
            orange,
            14f,
            true
        )
    }

    private fun wrap(
        text: String,
        context: RenderContext,
        maxWidth: Float,
        size: Float
    ): List<String> {
        val result = mutableListOf<String>()
        var line = ""
        text.split(Regex("\\s+")).forEach { word ->
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (line.isNotEmpty() && context.measureText(candidate, size) > maxWidth) {
                result += line
                line = word
            } else {
                line = candidate
            }
        }
        if (line.isNotEmpty()) result += line
        return result
    }
}
