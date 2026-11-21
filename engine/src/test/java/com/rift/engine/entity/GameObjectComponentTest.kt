package com.rift.engine.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GameObjectComponentTest {
    @Test
    fun attachesUpdatesAndDetachesComponents() {
        val owner = GameObject(0f, 0f, 10f, 10f)
        val events = mutableListOf<String>()
        val component = object : GameComponent {
            override fun onAttach(owner: GameObject) {
                events += "attach"
            }

            override fun update(owner: GameObject, deltaSeconds: Float) {
                events += "update"
            }

            override fun onDetach(owner: GameObject) {
                events += "detach"
            }
        }

        assertTrue(owner.addComponent(component))
        owner.update(1f / 60f)
        assertTrue(owner.removeComponent(component))

        assertEquals(listOf("attach", "update", "detach"), events)
        assertTrue(owner.components().isEmpty())
    }

    @Test
    fun duplicateInstanceIsNotAttachedTwice() {
        val owner = GameObject(0f, 0f, 10f, 10f)
        val component = object : GameComponent {}

        assertTrue(owner.addComponent(component))
        assertFalse(owner.addComponent(component))
        assertEquals(1, owner.components().size)
    }

    @Test
    fun componentChangesDuringUpdateAreDeferredUntilTraversalEnds() {
        val owner = GameObject(0f, 0f, 10f, 10f)
        var replacementUpdates = 0
        var replacementAttached = false
        val replacement = object : GameComponent {
            override fun onAttach(owner: GameObject) {
                replacementAttached = true
            }

            override fun update(owner: GameObject, deltaSeconds: Float) {
                replacementUpdates += 1
            }
        }
        val original = object : GameComponent {
            override fun update(owner: GameObject, deltaSeconds: Float) {
                owner.removeComponent(this)
                owner.addComponent(replacement)
            }
        }

        owner.addComponent(original)
        owner.update(1f / 60f)

        assertTrue(replacementAttached)
        assertEquals(0, replacementUpdates)
        assertSame(replacement, owner.components().single())

        owner.update(1f / 60f)
        assertEquals(1, replacementUpdates)
    }


    @Test
    fun failedAttachmentRollsBackComponentOwnershipAndAttemptsCleanup() {
        val owner = GameObject(0f, 0f, 10f, 10f)
        var cleanupCalled = false
        val broken = object : GameComponent {
            override fun onAttach(owner: GameObject) {
                error("attach failed")
            }

            override fun onDetach(owner: GameObject) {
                cleanupCalled = true
            }
        }

        try {
            owner.addComponent(broken)
        } catch (_: IllegalStateException) {
            // Expected: the component activation failed.
        }

        assertTrue(cleanupCalled)
        assertTrue(owner.components().isEmpty())
    }


    @Test
    fun failingComponentIsQuarantinedAndHealthyComponentsContinue() {
        val diagnostics = com.rift.engine.diagnostics.DiagnosticReporter()
        val owner = GameObject(0f, 0f, 10f, 10f)
        owner.diagnostics = diagnostics
        var failingUpdates = 0
        var healthyUpdates = 0
        owner.addComponent(object : GameComponent {
            override fun update(owner: GameObject, deltaSeconds: Float) {
                failingUpdates += 1
                error("broken component")
            }
        })
        owner.addComponent(object : GameComponent {
            override fun update(owner: GameObject, deltaSeconds: Float) {
                healthyUpdates += 1
            }
        })

        owner.update(1f / 60f)
        owner.update(1f / 60f)

        assertEquals(1, failingUpdates)
        assertEquals(2, healthyUpdates)
        assertEquals("RIFT_RUNTIME_EXCEPTION", diagnostics.latestError()?.code)
    }

    @Test
    fun readdingComponentCancelsPendingRemoval() {
        val owner = GameObject(0f, 0f, 10f, 10f)
        var updates = 0
        val component = object : GameComponent {
            override fun update(owner: GameObject, deltaSeconds: Float) {
                owner.removeComponent(this)
                owner.addComponent(this)
                updates += 1
            }
        }

        owner.addComponent(component)
        owner.update(1f / 60f)
        owner.update(1f / 60f)

        assertEquals(2, updates)
        assertSame(component, owner.components().single())
    }
}
