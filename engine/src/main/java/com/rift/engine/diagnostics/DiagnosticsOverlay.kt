package com.rift.engine.diagnostics

import com.rift.engine.graphics.RenderContext

/**
 * Fallback screen uses the shared render contract and no game assets.
 */
object DiagnosticsOverlay {
    fun render(
        context: RenderContext,
        entries: List<DiagnosticEntry>
    ) {
        val background = 0xFF11141D.toInt()
        val white = 0xFFFFFFFF.toInt()
        val orange = 0xFFFFBE5F.toInt()
        val body = 0xFFDCE1EB.toInt()

        context.clear(background)

        var y = 44f
        context.drawText(
            "RIFT ENGINE / DIAGNOSTICS",
            24f,
            y,
            white,
            28f,
            true
        )
        y += 34f

        val latestError = entries.lastOrNull {
            it.severity == DiagnosticSeverity.ERROR ||
                it.severity == DiagnosticSeverity.FATAL
        }

        if (latestError != null) {
            context.drawText(
                "${latestError.severity} • ${latestError.code} • ${latestError.subsystem}",
                24f,
                y,
                orange,
                19f,
                true
            )
            y += 28f
            context.drawText(
                latestError.message,
                24f,
                y,
                body,
                16f
            )
            y += 24f
            latestError.suggestion?.let { suggestion ->
                context.drawText(
                    "Suggested next step: $suggestion",
                    24f,
                    y,
                    orange,
                    14f
                )
                y += 22f
            }
        } else {
            context.drawText(
                "No runtime errors recorded.",
                24f,
                y,
                body,
                16f
            )
            y += 24f
        }

        context.drawText(
            "Recent events: ${entries.size}",
            24f,
            y,
            orange,
            18f,
            true
        )
    }
}
