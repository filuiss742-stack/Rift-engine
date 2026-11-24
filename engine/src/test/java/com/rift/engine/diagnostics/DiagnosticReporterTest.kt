package com.rift.engine.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticReporterTest {
    @Test
    fun keepsOnlyTheMostRecentEntries() {
        val reporter = DiagnosticReporter(maxEntries = 2)

        reporter.report(DiagnosticSeverity.INFO, "FIRST", "test", "first")
        reporter.report(DiagnosticSeverity.WARNING, "SECOND", "test", "second")
        reporter.report(DiagnosticSeverity.ERROR, "THIRD", "test", "third")

        assertEquals(listOf("SECOND", "THIRD"), reporter.snapshot().map { it.code })
    }

    @Test
    fun deduplicatesIdenticalReports() {
        val reporter = DiagnosticReporter()
        val first = reporter.report(DiagnosticSeverity.WARNING, "DUPLICATE", "test", "same")
        val second = reporter.report(DiagnosticSeverity.WARNING, "DUPLICATE", "test", "same")

        assertSame(first, second)
        assertEquals(1, reporter.snapshot().size)
    }

    @Test
    fun clearRemovesAllEntries() {
        val reporter = DiagnosticReporter()
        reporter.report(DiagnosticSeverity.ERROR, "TEST_ERROR", "test", "failure")

        reporter.clear()

        assertEquals(emptyList<DiagnosticEntry>(), reporter.snapshot())
        assertFalse(reporter.hasErrors())
        assertFalse(reporter.hasFatalErrors())
    }


    @Test
    fun storesActionableSuggestionsWithDiagnostics() {
        val reporter = DiagnosticReporter()
        val entry = reporter.report(
            severity = DiagnosticSeverity.ERROR,
            code = "ASSET_MISSING",
            subsystem = "assets",
            message = "Texture was not found.",
            location = "sprites/hero.png",
            suggestion = "Check the path and ensure the file is packaged."
        )

        assertEquals("Check the path and ensure the file is packaged.", entry.suggestion)
        assertEquals(entry, reporter.latestError())
    }

    @Test
    fun runtimeExceptionsReceiveDefaultRepairGuidance() {
        val reporter = DiagnosticReporter()
        val entry = reporter.reportException("scene.update", IllegalStateException("test"))

        assertTrue(entry.suggestion?.contains("source location") == true)
    }

    @Test
    fun fatalStateSurvivesHistoryEvictionUntilExplicitClear() {
        val reporter = DiagnosticReporter(maxEntries = 2)

        reporter.report(DiagnosticSeverity.FATAL, "FATAL_FIRST", "test", "stop")
        repeat(5) { index ->
            reporter.report(DiagnosticSeverity.INFO, "INFO_$index", "test", "info")
        }

        assertEquals(3, reporter.snapshot().size)
        assertTrue(reporter.hasFatalErrors())
        assertTrue(reporter.snapshot().any { it.code == "FATAL_FIRST" })

        reporter.clear()
        assertFalse(reporter.hasFatalErrors())
    }

    @Test
    fun fatalSeverityIsDistinctFromRecoverableErrors() {
        val reporter = DiagnosticReporter()

        reporter.report(DiagnosticSeverity.ERROR, "RECOVERABLE", "test", "operation failed")

        assertTrue(reporter.hasErrors())
        assertFalse(reporter.hasFatalErrors())

        reporter.report(DiagnosticSeverity.FATAL, "ENGINE_STOP", "test", "engine cannot continue")

        assertTrue(reporter.hasFatalErrors())
    }
}
