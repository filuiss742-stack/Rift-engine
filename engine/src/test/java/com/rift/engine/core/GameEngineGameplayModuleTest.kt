package com.rift.engine.core

import com.rift.engine.module.GameplayModule
import com.rift.engine.module.GameplayModuleContext
import com.rift.engine.module.GameplayModuleDescriptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineGameplayModuleTest {
    @Test
    fun engineUpdatesInstalledGameplayModules() {
        val engine = GameEngine()
        val module = CountingModule()

        assertTrue(engine.gameplayModules.install(module))
        engine.update(1f / 60f)

        assertEquals(1, module.updateCount)
    }

    private class CountingModule : GameplayModule {
        override val descriptor = GameplayModuleDescriptor(
            id = "test.counting",
            version = "1.0.0"
        )

        var updateCount = 0
            private set

        override fun onUpdate(deltaSeconds: Float) {
            updateCount += 1
        }
    }
}
