package com.rift.engine.desktop

import com.rift.engine.diagnostics.DiagnosticReporter
import com.rift.engine.diagnostics.DiagnosticSeverity
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopSaveStoreTest {

    @Test
    fun shortFileNamesCanBePersisted() {
        val directory = Files.createTempDirectory("rift-save-short-name").toFile()
        val file = directory.resolve("s")
        val store = DesktopSaveStore(file)

        store.putString("level", "forest")

        assertEquals("forest", DesktopSaveStore(file).getString("level"))
    }

    @Test
    fun persistenceFailureIsNotReportedAsSuccessfulWrite() {
        val directory = Files.createTempDirectory("rift-save-write-failure").toFile()
        val file = directory.resolve("blocked")
        Files.createDirectory(file)
        val diagnostics = DiagnosticReporter()
        val store = DesktopSaveStore(file, diagnostics)

        try {
            store.putString("level", "forest")
            throw AssertionError("Expected a persistence failure.")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message.orEmpty().contains("Could not persist save data"))
        }

        assertTrue(diagnostics.snapshot().any { it.code == "RIFT_RUNTIME_EXCEPTION" })
    }

    @Test
    fun rejectsNonFiniteFloatValues() {
        val directory = Files.createTempDirectory("rift-save-finite").toFile()
        val store = DesktopSaveStore(directory.resolve("saves.properties"))

        try {
            store.putFloat("volume", Float.NaN)
            throw AssertionError("Expected non-finite values to be rejected.")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    @Test
    fun valuesSurviveStoreRecreation() {
        val directory = Files.createTempDirectory("rift-save-test").toFile()
        val file = directory.resolve("saves.properties")

        val firstStore = DesktopSaveStore(file)
        firstStore.putString("level", "forest")
        firstStore.putFloat("volume", 0.75f)

        val reopenedStore = DesktopSaveStore(file)

        assertEquals("forest", reopenedStore.getString("level"))
        assertEquals(0.75f, reopenedStore.getFloat("volume"), 0.0001f)
    }

    @Test
    fun malformedSaveIsBackedUpAndReported() {
        val directory = Files.createTempDirectory("rift-save-corrupt-test").toFile()
        val file = directory.resolve("saves.properties")
        file.writeText("broken=\\uZZZZ")

        val diagnostics = DiagnosticReporter()
        val store = DesktopSaveStore(file, diagnostics)

        assertEquals("fallback", store.getString("missing", "fallback"))
        assertTrue(
            diagnostics.snapshot().any {
                it.code == "DESKTOP_SAVE_READ_FAILED" &&
                    it.severity == DiagnosticSeverity.WARNING
            }
        )
        assertTrue(
            directory.listFiles().orEmpty().any {
                it.name.startsWith("saves.properties.corrupt-")
            }
        )
    }

    @Test
    fun invalidFloatUsesDefaultAndReportsWarning() {
        val directory = Files.createTempDirectory("rift-save-value-test").toFile()
        val file = directory.resolve("saves.properties")
        file.writeText("f.volume=not-a-number")

        val diagnostics = DiagnosticReporter()
        val store = DesktopSaveStore(file, diagnostics)

        assertEquals(0.5f, store.getFloat("volume", 0.5f), 0.0001f)
        assertTrue(
            diagnostics.snapshot().any {
                it.code == "DESKTOP_SAVE_VALUE_INVALID"
            }
        )
    }
}
