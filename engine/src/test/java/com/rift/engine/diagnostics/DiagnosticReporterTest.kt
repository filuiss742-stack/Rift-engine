package com.rift.engine.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
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
        assertEquals(false, reporter.hasErrors())
    }
}
