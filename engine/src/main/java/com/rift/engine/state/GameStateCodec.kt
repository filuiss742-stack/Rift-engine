package com.rift.engine.state

import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64

/**
 * Versioned, bounded codec for primitive GameState values.
 * Unknown types and malformed data are rejected rather than silently dropped.
 */
object GameStateCodec {
    private const val HEADER = "RIFT_STATE_V1"
    private const val MAX_BYTES = 1_048_576
    private const val MAX_ENTRIES = 10_000
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun encode(state: GameState): String {
        val snapshot = state.snapshot().toSortedMap()
        require(snapshot.size <= MAX_ENTRIES) {
            "Game state contains more than $MAX_ENTRIES entries."
        }
        val result = StringBuilder(HEADER).append('\n')
        var encodedBytes = HEADER.toByteArray(UTF_8).size + 1
        snapshot.forEach { (key, value) ->
            val (type, rawValue) = encodeValue(value)
            val line = type + "\t" + encodeText(key) + "\t" +
                encodeText(rawValue) + "\n"
            encodedBytes += line.toByteArray(UTF_8).size
            require(encodedBytes <= MAX_BYTES) {
                "Encoded game state exceeds the 1 MiB limit."
            }
            result.append(line)
        }
        return result.toString()
    }

    fun decode(encoded: String): Map<String, Any> {
        require(encoded.toByteArray(UTF_8).size <= MAX_BYTES) {
            "Encoded game state exceeds the 1 MiB limit."
        }
        val lines = encoded.removeSuffix("\n").split('\n')
        require(lines.firstOrNull() == HEADER) {
            "Unsupported or missing Rift game-state header."
        }
        require(lines.size - 1 <= MAX_ENTRIES) {
            "Game state contains more than $MAX_ENTRIES entries."
        }

        val result = linkedMapOf<String, Any>()
        lines.drop(1).forEach { line ->
            require(line.isNotEmpty()) { "Game state contains an empty entry." }
            val parts = line.split('\t')
            require(parts.size == 3) { "Game-state entry must have three tab-separated fields." }
            val key = decodeText(parts[1])
            require(key.isNotBlank()) { "Game-state keys must not be blank." }
            require(!result.containsKey(key)) { "Duplicate game-state key '$key'." }
            val rawValue = decodeText(parts[2])
            result[key] = decodeValue(parts[0], rawValue)
        }
        return result
    }

    private fun encodeValue(value: Any): Pair<String, String> = when (value) {
        is Boolean -> "b" to value.toString()
        is Int -> "i" to value.toString()
        is Long -> "l" to value.toString()
        is Float -> {
            require(value.isFinite()) { "Game-state floats must be finite." }
            "f" to value.toString()
        }
        is Double -> {
            require(value.isFinite()) { "Game-state doubles must be finite." }
            "d" to value.toString()
        }
        is String -> "s" to value
        else -> throw IllegalArgumentException(
            "Unsupported game-state value type: " + value::class.qualifiedName
        )
    }

    private fun decodeValue(type: String, value: String): Any = when (type) {
        "b" -> value.toBooleanStrictOrNull()
            ?: throw IllegalArgumentException("Invalid boolean game-state value.")
        "i" -> value.toIntOrNull()
            ?: throw IllegalArgumentException("Invalid integer game-state value.")
        "l" -> value.toLongOrNull()
            ?: throw IllegalArgumentException("Invalid long game-state value.")
        "f" -> value.toFloatOrNull()?.takeIf { it.isFinite() }
            ?: throw IllegalArgumentException("Invalid float game-state value.")
        "d" -> value.toDoubleOrNull()?.takeIf { it.isFinite() }
            ?: throw IllegalArgumentException("Invalid double game-state value.")
        "s" -> value
        else -> throw IllegalArgumentException("Unknown game-state value type '$type'.")
    }

    private fun encodeText(value: String): String =
        encoder.encodeToString(value.toByteArray(UTF_8))

    private fun decodeText(value: String): String =
        String(decoder.decode(value), UTF_8)
}
