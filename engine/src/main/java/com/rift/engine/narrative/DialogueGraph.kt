package com.rift.engine.narrative

/** Game-authored dialogue data, independent from rendering and platform code. */
class DialogueGraph(nodes: List<DialogueNode>) {
    private val nodesById: Map<String, DialogueNode>

    init {
        require(nodes.isNotEmpty()) { "A dialogue graph must contain at least one node." }
        require(nodes.all { it.id.isNotBlank() }) { "Dialogue node IDs must not be blank." }
        require(nodes.map { it.id }.distinct().size == nodes.size) { "Dialogue node IDs must be unique." }
        nodes.forEach { node ->
            require(node.speaker.isNotBlank()) { "Speaker must not be blank in node '${node.id}'." }
            require(node.text.isNotBlank()) { "Text must not be blank in node '${node.id}'." }
            require(node.choices.isEmpty() || node.nextNodeId == null) {
                "Node '${node.id}' cannot have both choices and an automatic next node."
            }
            require(node.choices.map { it.id }.distinct().size == node.choices.size) {
                "Choice IDs must be unique inside node '${node.id}'."
            }
        }
        nodesById = nodes.associateBy { it.id }
        nodes.forEach { node ->
            node.nextNodeId?.let { target ->
                require(target in nodesById) { "Node '${node.id}' points to missing node '$target'." }
            }
            node.choices.forEach { choice ->
                require(choice.id.isNotBlank()) { "Choice ID must not be blank in node '${node.id}'." }
                require(choice.text.isNotBlank()) { "Choice text must not be blank in node '${node.id}'." }
                require(choice.targetNodeId in nodesById) {
                    "Choice '${choice.id}' in node '${node.id}' points to missing node '${choice.targetNodeId}'."
                }
            }
        }
    }

    fun node(id: String): DialogueNode? = nodesById[id]
    fun contains(id: String): Boolean = id in nodesById
}

data class DialogueNode(
    val id: String,
    val speaker: String,
    val text: String,
    val nextNodeId: String? = null,
    val choices: List<DialogueChoice> = emptyList()
)

data class DialogueChoice(
    val id: String,
    val text: String,
    val targetNodeId: String,
    val conditions: List<StoryCondition> = emptyList(),
    val effects: List<StoryEffect> = emptyList()
)
