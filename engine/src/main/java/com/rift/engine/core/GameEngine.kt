package com.rift.engine.core

import com.rift.engine.assets.AssetManager
import com.rift.engine.audio.AudioManager
import com.rift.engine.diagnostics.DiagnosticReporter
import com.rift.engine.diagnostics.DiagnosticSeverity
import com.rift.engine.diagnostics.DiagnosticsOverlay
import com.rift.engine.graphics.RenderContext
import com.rift.engine.input.PointerEvent
import com.rift.engine.input.PointerEventType
import com.rift.engine.module.GameplayModuleManager
import com.rift.engine.plugin.PluginManager
import com.rift.engine.storage.InMemorySaveStore
import com.rift.engine.storage.SaveStore
import com.rift.engine.state.GameStateCodec
import java.util.concurrent.ConcurrentLinkedQueue

private sealed interface InputEvent {
    data class Pointer(val event: PointerEvent) : InputEvent
    data class Key(val code: Int, val pressed: Boolean) : InputEvent
    data class Axis(val axisId: String, val value: Float) : InputEvent
    data object ResetInput : InputEvent
}

/**
 * Shared runtime. A platform host provides the game loop, renderer and storage.
 *
 * ERROR diagnostics remain visible but do not automatically stop the game.
 * Only an explicitly reported FATAL diagnostic pauses engine updates.
 */
class GameEngine(
    val saves: SaveStore = InMemorySaveStore(),
    val diagnostics: DiagnosticReporter = DiagnosticReporter(),
    val assets: AssetManager = AssetManager(diagnostics = diagnostics),
    val audio: AudioManager = AudioManager(diagnostics = diagnostics)
) : AutoCloseable {
    val plugins = PluginManager(diagnostics)
    val gameplayModules = GameplayModuleManager(diagnostics)
    val scenes = SceneManager(diagnostics, assets, audio)

    private val inputs = ConcurrentLinkedQueue<InputEvent>()
    @Volatile private var closed = false

    private companion object {
        const val SAVE_KEY_PREFIX = "rift.game-state."
    }

    /** Legacy pointer-move callback retained for existing integrations. */
    fun onTouch(x: Float, y: Float, width: Float, height: Float) {
        onPointer(
            PointerEvent(
                pointerId = 0,
                type = PointerEventType.MOVE,
                x = x,
                y = y,
                viewportWidth = width,
                viewportHeight = height
            )
        )
    }

    fun onPointer(event: PointerEvent) {
        if (!closed && !diagnostics.hasFatalErrors()) {
            inputs.offer(InputEvent.Pointer(event))
        }
    }

    fun onKey(code: Int, pressed: Boolean) {
        if (!closed && !diagnostics.hasFatalErrors()) {
            inputs.offer(InputEvent.Key(code, pressed))
        }
    }

    fun onAxis(axisId: String, value: Float) {
        require(axisId.isNotBlank()) { "axisId must not be blank." }
        require(value.isFinite()) { "Axis values must be finite." }
        if (!closed && !diagnostics.hasFatalErrors()) {
            inputs.offer(InputEvent.Axis(axisId, value.coerceIn(-1f, 1f)))
        }
    }

    /** Releases all held keys and pointer buttons after the host loses focus. */
    fun onFocusLost() {
        if (!closed && !diagnostics.hasFatalErrors()) {
            inputs.offer(InputEvent.ResetInput)
        }
    }

    fun update(deltaSeconds: Float) {
        require(deltaSeconds.isFinite() && deltaSeconds >= 0f) {
            "deltaSeconds must be finite and non-negative."
        }
        if (closed || diagnostics.hasFatalErrors()) return

        safe("input.dispatch") {
            while (true) {
                when (val event = inputs.poll() ?: break) {
                    is InputEvent.Pointer -> scenes.onPointer(event.event)
                    is InputEvent.Key -> scenes.onKey(event.code, event.pressed)
                    is InputEvent.Axis -> scenes.onAxis(event.axisId, event.value)
                    InputEvent.ResetInput -> scenes.resetInput()
                }
                if (diagnostics.hasFatalErrors()) break
            }
        }

        if (!diagnostics.hasFatalErrors()) {
            safe("plugins.update") { plugins.onUpdate(deltaSeconds) }
        }
        if (!diagnostics.hasFatalErrors()) {
            safe("gameplayModules.update") { gameplayModules.update(deltaSeconds) }
        }
        if (!diagnostics.hasFatalErrors()) {
            safe("scene.update") { scenes.update(deltaSeconds) }
        }
    }

    /**
     * Saves the shared primitive GameState under a stable slot identifier.
     * Call at a safe game-loop boundary, not from a background worker.
     */
    fun saveGameState(slot: String): Boolean {
        check(!closed) { "GameEngine is closed." }
        require(validSaveSlot(slot)) {
            "Save slot must contain 1..64 letters, digits, dots, underscores, or hyphens."
        }
        return try {
            saves.putString(SAVE_KEY_PREFIX + slot, GameStateCodec.encode(scenes.gameState))
            true
        } catch (exception: Exception) {
            diagnostics.reportException(
                "save.game-state.write",
                exception,
                "Check the save slot and ensure the current game state contains supported primitive values."
            )
            false
        }
    }

    /**
     * Loads a previously saved primitive GameState. Returns false when the slot
     * is empty or the data is invalid; malformed data is reported, not applied.
     */
    fun loadGameState(slot: String): Boolean {
        check(!closed) { "GameEngine is closed." }
        require(validSaveSlot(slot)) {
            "Save slot must contain 1..64 letters, digits, dots, underscores, or hyphens."
        }
        return try {
            val encoded = saves.getString(SAVE_KEY_PREFIX + slot, "")
            if (encoded.isEmpty()) return false
            val decoded = GameStateCodec.decode(encoded)
            scenes.gameState.replaceWith(decoded)
            true
        } catch (exception: Exception) {
            diagnostics.report(
                DiagnosticSeverity.WARNING,
                "RIFT_SAVE_STATE_INVALID",
                "GameEngine",
                "Saved game state could not be loaded.",
                exception.toString(),
                slot,
                "Keep a backup of the save and restore a valid slot or start a new game."
            )
            false
        }
    }

    /** Releases all owned runtime resources when the host shuts down. */
    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        inputs.clear()

        // One failing cleanup must not prevent the remaining resources from closing.
        safe("engine.close.plugins") { plugins.close() }
        safe("engine.close.gameplayModules") { gameplayModules.close() }
        safe("engine.close.scenes") { scenes.close() }
        safe("engine.close.audio") { audio.close() }
        safe("engine.close.assets") { assets.close() }
    }

    fun render(context: RenderContext) {
        if (closed) return
        if (diagnostics.hasFatalErrors()) {
            DiagnosticsOverlay.render(context, diagnostics.snapshot(), blocking = true)
            return
        }

        safe("scene.render") { scenes.render(context) }

        if (diagnostics.hasErrors()) {
            DiagnosticsOverlay.render(context, diagnostics.snapshot())
        }
    }

    private fun validSaveSlot(slot: String): Boolean =
        slot.matches(Regex("^[A-Za-z0-9._-]{1,64}$"))

    private inline fun safe(
        subsystem: String,
        block: () -> Unit
    ) {
        try {
            block()
        } catch (exception: Exception) {
            diagnostics.reportException(subsystem, exception)
        }
    }
}
