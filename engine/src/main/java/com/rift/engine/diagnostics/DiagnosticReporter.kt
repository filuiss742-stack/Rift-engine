package com.rift.engine.diagnostics

enum class DiagnosticSeverity {
    INFO,
    WARNING,
    ERROR,
    FATAL
}

data class DiagnosticEntry(
    val severity: DiagnosticSeverity,
    val code: String,
    val subsystem: String,
    val message: String,
    val cause: String? = null,
    val location: String? = null,
    val timestampMillis: Long = System.currentTimeMillis(),
    val suggestion: String? = null
)

/**
 * Thread-safe, bounded diagnostic history shared by engine systems.
 *
 * Reports are evidence, not automatic fixes. Exact duplicate reports are
 * coalesced so a recurring failure cannot flood the history. FATAL state is
 * sticky and independent of the bounded history until clear() is called.
 */
class DiagnosticReporter(
    private val maxEntries: Int = 100
) {
    private val lock = Any()
    private val entries = mutableListOf<DiagnosticEntry>()
    private var fatalEntry: DiagnosticEntry? = null

    init {
        require(maxEntries > 0) { "maxEntries must be greater than zero." }
    }

    fun report(
        severity: DiagnosticSeverity,
        code: String,
        subsystem: String,
        message: String,
        cause: String? = null,
        location: String? = null,
        suggestion: String? = null
    ): DiagnosticEntry = synchronized(lock) {
        val duplicate = entries.lastOrNull {
            it.severity == severity &&
                it.code == code &&
                it.subsystem == subsystem &&
                it.message == message &&
                it.suggestion == suggestion
        }
        if (duplicate != null) {
            return@synchronized duplicate
        }

        val entry = DiagnosticEntry(
            severity = severity,
            code = code,
            subsystem = subsystem,
            message = message,
            cause = cause,
            location = location,
            suggestion = suggestion
        )
        entries += entry
        if (severity == DiagnosticSeverity.FATAL && fatalEntry == null) {
            fatalEntry = entry
        }

        while (entries.size > maxEntries) {
            entries.removeAt(0)
        }

        entry
    }

    fun reportException(
        subsystem: String,
        exception: Exception,
        suggestion: String? = "Inspect the failing subsystem and the reported source location."
    ): DiagnosticEntry {
        val frame = exception.stackTrace.firstOrNull()
        val location = frame?.let {
            "${it.className}.${it.methodName} (${it.fileName ?: "unknown"}:${it.lineNumber})"
        }

        return report(
            severity = DiagnosticSeverity.ERROR,
            code = "RIFT_RUNTIME_EXCEPTION",
            subsystem = subsystem,
            message = "${exception.javaClass.simpleName}: ${exception.message ?: "The operation failed without an error message."}",
            cause = exception.cause?.toString(),
            location = location,
            suggestion = suggestion
        )
    }

    fun snapshot(): List<DiagnosticEntry> = synchronized(lock) {
        val retained = entries.toMutableList()
        fatalEntry?.let { fatal ->
            if (retained.none { it === fatal }) retained += fatal
        }
        retained.sortedBy { it.timestampMillis }
    }

    fun latestError(): DiagnosticEntry? = synchronized(lock) {
        entries.lastOrNull {
            it.severity == DiagnosticSeverity.ERROR ||
                it.severity == DiagnosticSeverity.FATAL
        } ?: fatalEntry
    }

    fun hasErrors(): Boolean = synchronized(lock) {
        fatalEntry != null || entries.any {
            it.severity == DiagnosticSeverity.ERROR ||
                it.severity == DiagnosticSeverity.FATAL
        }
    }

    fun hasFatalErrors(): Boolean = synchronized(lock) {
        fatalEntry != null
    }

    fun clear() {
        synchronized(lock) {
            entries.clear()
            fatalEntry = null
        }
    }
}
