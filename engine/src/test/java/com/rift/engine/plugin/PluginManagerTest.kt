package com.rift.engine.plugin

import com.rift.engine.diagnostics.DiagnosticReporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginManagerTest {
    @Test
    fun failedInstallDoesNotLeavePluginRegistered() {
        val diagnostics = DiagnosticReporter()
        val manager = PluginManager(diagnostics)

        manager.install(object : EnginePlugin {
            override val id = "test.broken"

            override fun onInstall() {
                error("activation failed")
            }
        })

        assertTrue(manager.all().isEmpty())
        assertTrue(diagnostics.hasErrors())
    }

    @Test
    fun duplicateIdDoesNotReplaceExistingPlugin() {
        val manager = PluginManager()
        val original = CountingPlugin("test.shared")
        val ignored = CountingPlugin("test.shared")

        manager.install(original)
        manager.install(ignored)

        assertEquals(1, manager.all().size)
        assertTrue(manager.all().single() === original)
        assertEquals(0, ignored.updateCount)
    }

    @Test
    fun pluginCanUninstallItselfDuringUpdate() {
        val manager = PluginManager()
        val plugin = SelfRemovingPlugin(manager)

        manager.install(plugin)
        manager.onUpdate(1f / 60f)

        assertTrue(plugin.updated)
        assertTrue(plugin.uninstalled)
        assertFalse(manager.all().any { it.id == plugin.id })
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidDeltaTime() {
        PluginManager().onUpdate(Float.NaN)
    }

    private class CountingPlugin(
        override val id: String
    ) : EnginePlugin {
        var updateCount = 0
            private set

        override fun onUpdate(deltaSeconds: Float) {
            updateCount += 1
        }
    }

    private class SelfRemovingPlugin(
        private val manager: PluginManager
    ) : EnginePlugin {
        override val id = "test.self-removing"
        var updated = false
            private set
        var uninstalled = false
            private set

        override fun onUpdate(deltaSeconds: Float) {
            updated = true
            manager.uninstall(id)
        }

        override fun onUninstall() {
            uninstalled = true
        }
    }
}
