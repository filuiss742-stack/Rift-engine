package com.rift.engine.narrative

import org.junit.Assert.*
import org.junit.Test

class DialogueSessionTest {
    @Test
    fun choiceEffectsChangeStateUsedByLaterBranches() {
        val graph = DialogueGraph(
            listOf(
                DialogueNode("start", "Guard", "What will you do?", choices = listOf(
                    DialogueChoice("help", "Help the guard", "helped", effects = listOf(
                        SetStoryFlag("guard_helped"), AddStoryValue("trust", 2)
                    )),
                    DialogueChoice("leave", "Walk away", "left")
                )),
                DialogueNode("helped", "Guard", "I will remember this."),
                DialogueNode("later", "Guard", "You helped me before.", choices = listOf(
                    DialogueChoice("remembered", "Ask for help", "ally",
                        conditions = listOf(HasStoryFlag("guard_helped")))
                )),
                DialogueNode("ally", "Guard", "I will stand with you."),
                DialogueNode("left", "Guard", "Then go.")
            )
        )
        val state = StoryState()
        val session = DialogueSession(graph, state)
        assertTrue(session.start("start"))
        assertTrue(session.choose("help"))
        assertTrue(state.hasFlag("guard_helped"))
        assertEquals(2, state.value("trust"))
        assertTrue(session.start("later"))
        assertEquals(listOf("remembered"), session.availableChoices().map { it.id })
        assertTrue(session.choose("remembered"))
        assertEquals("ally", session.currentNode?.id)
    }

    @Test
    fun lockedChoiceCannotBeSelectedOrApplyEffects() {
        val graph = DialogueGraph(listOf(
            DialogueNode("start", "Guide", "Do you have proof?", choices = listOf(
                DialogueChoice("show", "Show proof", "success",
                    conditions = listOf(HasStoryFlag("has_proof")),
                    effects = listOf(SetStoryFlag("proof_shown")))
            )),
            DialogueNode("success", "Guide", "Thank you.")
        ))
        val state = StoryState()
        val session = DialogueSession(graph, state)
        assertTrue(session.start("start"))
        assertTrue(session.availableChoices().isEmpty())
        assertFalse(session.choose("show"))
        assertFalse(state.hasFlag("proof_shown"))
        assertEquals("start", session.currentNode?.id)
    }

    @Test
    fun automaticDialogueAdvancesAndEnds() {
        val graph = DialogueGraph(listOf(
            DialogueNode("one", "A", "First", nextNodeId = "two"),
            DialogueNode("two", "B", "Last")
        ))
        val session = DialogueSession(graph)
        assertTrue(session.start("one"))
        assertTrue(session.advance())
        assertEquals("two", session.currentNode?.id)
        assertFalse(session.advance())
        assertFalse(session.isFinished)
    }

    @Test(expected = IllegalArgumentException::class)
    fun missingTargetIsRejectedWhenGraphIsCreated() {
        DialogueGraph(listOf(DialogueNode("start", "A", "Hello", nextNodeId = "missing")))
    }
}
