package com.rift.engine.module

import com.rift.engine.diagnostics.DiagnosticReporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class GameplayModuleManagerTest {
    @Test
    fun closeUninstallsAllModulesOnceInReverseOrder() {
        val manager = GameplayModuleManager()
        val cleanupOrder = mutableListOf<String>()
        val first = object : GameplayModule {
            override val descriptor = descriptor("first")
            override fun onUninstall() { cleanupOrder += "first" }
        }
        val second = object : GameplayModule {
            override val descriptor = descriptor("second")
            override fun onUninstall() { cleanupOrder += "second" }
        }

        assertTrue(manager.install(first))
        assertTrue(manager.install(second))
        manager.close()
        manager.close()

        assertEquals(listOf("second", "first"), cleanupOrder)
        assertTrue(manager.installed().isEmpty())
        assertFalse(manager.install(TestModule(descriptor("late"))))
    }

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
    fun failedInstallationAttemptsCleanup() {
        val manager = GameplayModuleManager()
        val module = TestModule(descriptor("partial"), failInstall = true)

        assertFalse(manager.install(module))
        assertEquals(1, module.uninstallCount)
        assertFalse(manager.isInstalled("partial"))
    }


    @Test
    fun failedReplacementAndFailedRestoreRemovesInactiveModule() {
        val diagnostics = DiagnosticReporter()
        val manager = GameplayModuleManager(diagnostics)
        var originalInstallCount = 0
        var originalUpdateCount = 0
        var originalCleanupCount = 0
        val original = object : GameplayModule {
            override val descriptor = descriptor("combat")
            override fun onInstall(context: GameplayModuleContext) {
                originalInstallCount += 1
                if (originalInstallCount > 1) error("restore failed")
            }
            override fun onUpdate(deltaSeconds: Float) {
                originalUpdateCount += 1
            }
            override fun onUninstall() {
                originalCleanupCount += 1
            }
        }
        val replacement = TestModule(descriptor("combat", "2.0.0"), failInstall = true)

        assertTrue(manager.install(original))
        assertFalse(manager.replace("combat", replacement))
        assertFalse(manager.isInstalled("combat"))
        assertTrue(manager.installed().isEmpty())
        assertEquals(0, originalUpdateCount)
        assertEquals(2, originalCleanupCount)
        assertTrue(diagnostics.snapshot().any { it.code == "MODULE_RESTORE_FAILED" })

        manager.update(1f / 60f)
        assertEquals(0, originalUpdateCount)
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
        assertEquals(1, replacement.uninstallCount)
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
    fun uninstallWaitsUntilAnInFlightUpdateCallbackFinishes() {
        val manager = GameplayModuleManager()
        val callbackStarted = CountDownLatch(1)
        val allowCallbackToFinish = CountDownLatch(1)
        val uninstallFinished = AtomicBoolean(false)
        val module = object : GameplayModule {
            override val descriptor = descriptor("blocking")
            override fun onUpdate(deltaSeconds: Float) {
                callbackStarted.countDown()
                check(allowCallbackToFinish.await(2, TimeUnit.SECONDS))
            }
        }
        assertTrue(manager.install(module))

        val updateThread = Thread { manager.update(1f / 60f) }
        updateThread.start()
        assertTrue(callbackStarted.await(2, TimeUnit.SECONDS))

        val uninstallThread = Thread {
            manager.uninstall("blocking")
            uninstallFinished.set(true)
        }
        uninstallThread.start()

        Thread.sleep(50)
        assertFalse(uninstallFinished.get())
        allowCallbackToFinish.countDown()
        updateThread.join(2000)
        uninstallThread.join(2000)

        assertFalse(updateThread.isAlive)
        assertFalse(uninstallThread.isAlive)
        assertTrue(uninstallFinished.get())
        assertFalse(manager.isInstalled("blocking"))
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
