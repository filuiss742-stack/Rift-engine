package com.rift.engine.narrative

sealed interface StoryCondition {
    fun isMet(state: StoryState): Boolean
}

data class HasStoryFlag(val flagId: String, val expected: Boolean = true) : StoryCondition {
    override fun isMet(state: StoryState): Boolean = state.hasFlag(flagId) == expected
}

data class StoryValueEquals(val valueId: String, val expected: Int) : StoryCondition {
    override fun isMet(state: StoryState): Boolean = state.value(valueId) == expected
}

data class StoryValueAtLeast(val valueId: String, val minimum: Int) : StoryCondition {
    override fun isMet(state: StoryState): Boolean = state.value(valueId) >= minimum
}

sealed interface StoryEffect {
    fun apply(state: StoryState)
}

data class SetStoryFlag(val flagId: String, val enabled: Boolean = true) : StoryEffect {
    override fun apply(state: StoryState) = state.setFlag(flagId, enabled)
}

data class SetStoryValue(val valueId: String, val value: Int) : StoryEffect {
    override fun apply(state: StoryState) = state.setValue(valueId, value)
}

data class AddStoryValue(val valueId: String, val amount: Int) : StoryEffect {
    override fun apply(state: StoryState) = state.addValue(valueId, amount)
}
