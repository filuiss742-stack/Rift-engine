package com.rift.engine.narrative

/** Small shared story state: flags for facts and integers for counters/relationships. */
class StoryState {
    private val flags = linkedSetOf<String>()
    private val values = linkedMapOf<String, Int>()

    fun hasFlag(id: String): Boolean = id in flags

    fun setFlag(id: String, enabled: Boolean = true) {
        require(id.isNotBlank()) { "Story flag ID must not be blank." }
        if (enabled) flags.add(id) else flags.remove(id)
    }

    fun value(id: String, default: Int = 0): Int = values[id] ?: default

    fun setValue(id: String, value: Int) {
        require(id.isNotBlank()) { "Story value ID must not be blank." }
        values[id] = value
    }

    fun addValue(id: String, amount: Int) {
        setValue(id, value(id) + amount)
    }

    fun flagsSnapshot(): Set<String> = flags.toSet()
    fun valuesSnapshot(): Map<String, Int> = values.toMap()
}
