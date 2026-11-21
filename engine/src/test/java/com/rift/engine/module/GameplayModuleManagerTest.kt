package com.rift.engine.module

import com.rift.engine.diagnostics.DiagnosticReporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameplayModuleManagerTest {
    @Test
    fun validatesDependenciesBeforeInstallation() {
        val diagnostics = DiagnosticReporter()
        val manager = GameplayModuleManager(diagnostics)
        val dependent = TestModule(
            GameplayModuleDescriptor("top-down", "1.0.0", dependencies = setOf("input"))
        )

        assertFalse(manager.install(dependent))
        assertFalse(manager.isInstalled("top-down"))
        assertTrue(diagnostics.snapshot().any { it.code == "MODULE_DEPENDENCY_MISSING" })

        assertTrue(manager.install(TestModule(descriptor("input"))))
        assertTrue(manager.install(dependent))
    }

    @Test
    fun rejectsDeclaredConflictsInEitherDirection() {
        val diagnostics = DiagnosticReporter()
        val manager = GameplayModuleManager(diagnostics)
        assertTrue(manager.install(TestModule(
            GameplayModuleDescriptor("physics-2d", "1.0.0", conflicts = setOf("physics-3d"))
        )))
        assertFalse(manager.install(TestModule(descriptor("physics-3d"))))
        assertTrue(diagnostics.snapshot().any { it.code == "MODULE_CONFLICT" })
    }

    @Test
    fun preventsRemovalOfRequiredModule() {
        val manager = GameplayModuleManager()
        assertTrue(manager.install(TestModule(descriptor("input"))))
        assertTrue(manager.install(TestModule(
            GameplayModuleDescriptor("top-down", "1.0.0", dependencies = setOf("input"))
        )))

        assertFalse(manager.uninstall("input"))
        assertTrue(manager.isInstalled("input"))
        assertTrue(manager.uninstall("top-down"))
        assertTrue(manager.uninstall("input"))
    }

    @Test
    fun replacesImplementationUsingSameStableId() {
        val manager = GameplayModuleManager()
        val original = TestModule(descriptor("combat"))
        val replacement = TestModule(descriptor("combat", "2.0.0"))

        assertTrue(manager.install(original))
        assertTrue(manager.replace("combat", replacement))
        assertEquals("2.0.0", manager.installed().single().version)
        assertEquals(1, original.uninstallCount)
        assertEquals(1, replacement.installCount)
    }

    @Test
    fun failedReplacementAttemptsToRestorePreviousModule() {
        val manager = GameplayModuleManager()
        val original = TestModule(descriptor("combat"))
        val replacement = TestModule(descriptor("combat", "2.0.0"), failInstall = true)

        assertTrue(manager.install(original))
        assertFalse(manager.replace("combat", replacement))
        assertEquals(2, original.installCount)
        assertEquals("1.0.0", manager.installed().single().version)
    }

    @Test
    fun exposesCapabilitiesAndUpdatesInstalledModules() {
        val manager = GameplayModuleManager()
        val module = TestModule(
            GameplayModuleDescriptor(
                "action-combat",
                "1.0.0",
                capabilities = setOf("combat.realtime", "combat.hitboxes")
            )
        )

        assertTrue(manager.install(module))
        assertTrue(manager.hasCapability("combat.realtime"))
        manager.update(1f / 60f)
        assertEquals(1, module.updateCount)
    }

    @Test
    fun failingModuleDoesNotBlockOtherModuleUpdates() {
        val diagnostics = DiagnosticReporter()
        val manager = GameplayModuleManager(diagnostics)
        val broken = TestModule(descriptor("broken"), failUpdate = true)
        val healthy = TestModule(descriptor("healthy"))

        assertTrue(manager.install(broken))
        assertTrue(manager.install(healthy))
        manager.update(1f / 60f)

        assertEquals(1, healthy.updateCount)
        assertTrue(diagnostics.snapshot().any {
            it.subsystem == "module[broken].onUpdate"
        })
    }

    private fun descriptor(id: String, version: String = "1.0.0") =
        GameplayModuleDescriptor(id, version)

    private class TestModule(
        override val descriptor: GameplayModuleDescriptor,
        private val failInstall: Boolean = false,
        private val failUpdate: Boolean = false
    ) : GameplayModule {
        var installCount = 0
            private set
        var uninstallCount = 0
            private set
        var updateCount = 0
            private set

        override fun onInstall(context: GameplayModuleContext) {
            installCount += 1
            if (failInstall) error("Expected install failure")
        }

        override fun onUpdate(deltaSeconds: Float) {
            updateCount += 1
            if (failUpdate) error("Expected update failure")
        }

        override fun onUninstall() {
            uninstallCount += 1
        }
    }
}
