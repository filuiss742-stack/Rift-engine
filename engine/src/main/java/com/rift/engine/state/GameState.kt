package com.rift.engine.state

import java.util.LinkedHashMap

/**
 * Small typed state bag shared by scenes and gameplay modes.
 *
 * Values are primitive immutable types so snapshots are safe to copy.
 * GameStateCodec provides bounded, versioned persistence for these values.
 */
class GameState {
    private val values = LinkedHashMap<String, Any>()

    private companion object {
        const val MAX_ENTRIES = 10_000
        const val MAX_KEY_LENGTH = 4_096
    }

    fun putBoolean(key: String, value: Boolean) = put(key, value)
    fun putInt(key: String, value: Int) = put(key, value)
    fun putLong(key: String, value: Long) = put(key, value)
    fun putFloat(key: String, value: Float) {
        require(value.isFinite()) { "Float state values must be finite." }
        put(key, value)
    }
    fun putDouble(key: String, value: Double) {
        require(value.isFinite()) { "Double state values must be finite." }
        put(key, value)
    }
    fun putString(key: String, value: String) = put(key, value)

    @Synchronized
    fun boolean(key: String, defaultValue: Boolean = false): Boolean =
        values[key] as? Boolean ?: defaultValue

    @Synchronized
    fun int(key: String, defaultValue: Int = 0): Int =
        values[key] as? Int ?: defaultValue

    @Synchronized
    fun long(key: String, defaultValue: Long = 0L): Long =
        values[key] as? Long ?: defaultValue

    @Synchronized
    fun float(key: String, defaultValue: Float = 0f): Float =
        values[key] as? Float ?: defaultValue

    @Synchronized
    fun double(key: String, defaultValue: Double = 0.0): Double =
        values[key] as? Double ?: defaultValue

    @Synchronized
    fun string(key: String, defaultValue: String = ""): String =
        values[key] as? String ?: defaultValue

    @Synchronized
    fun contains(key: String): Boolean = values.containsKey(key)

    @Synchronized
    fun remove(key: String): Boolean = values.remove(key) != null

    @Synchronized
    fun clear() = values.clear()

    @Synchronized
    fun snapshot(): Map<String, Any> = values.toMap()

    /**
     * Replaces the complete primitive state snapshot. Call at a safe game-loop
     * boundary; this is intended for loading a saved game, not per-frame use.
     */
    @Synchronized
    fun replaceWith(snapshot: Map<String, Any>) {
        require(snapshot.size <= 10_000) { "Game state may contain at most 10,000 entries." }
        snapshot.forEach { (key, value) ->
            require(key.isNotBlank() && key.length <= 4096) {
                "Game-state keys must be non-blank and at most 4096 characters."
            }
            when (value) {
                is Boolean, is Int, is Long, is String -> Unit
                is Float -> require(value.isFinite()) { "Float state values must be finite." }
                is Double -> require(value.isFinite()) { "Double state values must be finite." }
                else -> throw IllegalArgumentException(
                    "Unsupported game-state value type: " + value.javaClass.name
                )
            }
        }
        values.clear()
        values.putAll(snapshot)
    }

    @Synchronized
    private fun put(key: String, value: Any) {
        require(key.isNotBlank() && key.length <= MAX_KEY_LENGTH) {
            "Game-state keys must be non-blank and at most 4096 characters."
        }
        require(values.containsKey(key) || values.size < MAX_ENTRIES) {
            "Game state may contain at most 10000 entries."
        }
        values[key] = value
    }
}
