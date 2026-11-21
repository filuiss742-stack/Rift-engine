package com.rift.engine.diagnostics

enum class DiagnosticSeverity {
    INFO,
    WARNING,
    ERROR
}

data class DiagnosticEntry(
    val severity: DiagnosticSeverity,
    val code: String,
    val subsystem: String,
    val message: String,
    val cause: String? = null,
    val location: String? = null,
    val timestampMillis: Long = System.currentTimeMillis()
)

/**
 * Small thread-safe diagnostic store shared by engine systems.
 *
 * It keeps recent events, removes exact duplicate reports, and never attempts
 * to modify user code automatically.
 */
class DiagnosticReporter(
    private val maxEntries: Int = 100
) {
    private val lock = Any()
    private val entries = mutableListOf<DiagnosticEntry>()

    init {
        require(maxEntries > 0) { "maxEntries must be greater than zero." }
    }

    fun report(
        severity: DiagnosticSeverity,
        code: String,
        subsystem: String,
        message: String,
        cause: String? = null,
        location: String? = null
    ): DiagnosticEntry = synchronized(lock) {
        val duplicate = entries.lastOrNull {
            it.severity == severity &&
                it.code == code &&
                it.subsystem == subsystem &&
                it.message == message
        }
        if (duplicate != null) return@synchronized duplicate

        val entry = DiagnosticEntry(
            severity = severity,
            code = code,
            subsystem = subsystem,
            message = message,
            cause = cause,
            location = location
        )
        entries += entry
        while (entries.size > maxEntries) {
            entries.removeAt(0)
        }
        entry
    }

    fun reportException(subsystem: String, exception: Exception): DiagnosticEntry {
        val frame = exception.stackTrace.firstOrNull()
        val location = frame?.let {
            "${it.className}.${it.methodName} (${it.fileName ?: "unknown"}:${it.lineNumber})"
        }
        return report(
            severity = DiagnosticSeverity.ERROR,
            code = "RIFT_RUNTIME_EXCEPTION",
            subsystem = subsystem,
            message = exception.message ?: "The operation failed without an error message.",
            cause = exception.cause?.toString(),
            location = location
        )
    }

    fun snapshot(): List<DiagnosticEntry> = synchronized(lock) {
        entries.toList()
    }

    fun latestError(): DiagnosticEntry? = synchronized(lock) {
        entries.lastOrNull { it.severity == DiagnosticSeverity.ERROR }
    }

    fun hasErrors(): Boolean = latestError() != null

    fun clear() {
        synchronized(lock) {
            entries.clear()
        }
    }
}
