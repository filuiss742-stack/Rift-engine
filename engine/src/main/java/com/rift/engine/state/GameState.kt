package com.rift.engine.state

import java.util.concurrent.ConcurrentHashMap

/**
 * Small typed state bag shared by scenes and gameplay modes.
 *
 * Values are primitive immutable types so snapshots are safe to copy. This is
 * runtime state only; serialization is handled by a later save/codec layer.
 */
class GameState {
    private val values = ConcurrentHashMap<String, Any>()

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

    fun boolean(key: String, defaultValue: Boolean = false): Boolean =
        values[key] as? Boolean ?: defaultValue

    fun int(key: String, defaultValue: Int = 0): Int =
        values[key] as? Int ?: defaultValue

    fun long(key: String, defaultValue: Long = 0L): Long =
        values[key] as? Long ?: defaultValue

    fun float(key: String, defaultValue: Float = 0f): Float =
        values[key] as? Float ?: defaultValue

    fun double(key: String, defaultValue: Double = 0.0): Double =
        values[key] as? Double ?: defaultValue

    fun string(key: String, defaultValue: String = ""): String =
        values[key] as? String ?: defaultValue

    fun contains(key: String): Boolean = values.containsKey(key)

    fun remove(key: String): Boolean = values.remove(key) != null

    fun clear() = values.clear()

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

    private fun put(key: String, value: Any) {
        require(key.isNotBlank()) { "Game state keys must not be blank." }
        values[key] = value
    }
}
