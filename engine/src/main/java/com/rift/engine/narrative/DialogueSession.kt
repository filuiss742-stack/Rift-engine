package com.rift.engine.narrative

/** Runs a dialogue graph against shared story state; presentation belongs to the game. */
class DialogueSession(
    private val graph: DialogueGraph,
    val state: StoryState = StoryState()
) {
    var currentNode: DialogueNode? = null
        private set

    val isFinished: Boolean
        get() = currentNode == null

    fun start(nodeId: String): Boolean {
        val node = graph.node(nodeId) ?: return false
        currentNode = node
        return true
    }

    /** Advances to the next line, or closes a terminal line. Choice nodes wait for a choice. */
    fun advance(): Boolean {
        val node = currentNode ?: return false
        val nextId = node.nextNodeId
        if (nextId != null) {
            currentNode = graph.node(nextId)
            return currentNode != null
        }
        if (node.choices.isEmpty()) {
            currentNode = null
            return true
        }
        return false
    }

    fun availableChoices(): List<DialogueChoice> {
        val node = currentNode ?: return emptyList()
        return node.choices.filter { choice ->
            choice.conditions.all { condition -> condition.isMet(state) }
        }
    }

    fun choose(choiceId: String): Boolean {
        val choice = availableChoices().firstOrNull { it.id == choiceId } ?: return false
        val target = graph.node(choice.targetNodeId) ?: return false
        choice.effects.forEach { it.apply(state) }
        currentNode = target
        return true
    }
}
