package com.rift.engine.desktop

import com.rift.engine.core.FixedStepAccumulator
import com.rift.engine.core.GameEngine
import com.rift.engine.diagnostics.DiagnosticReporter
import com.rift.engine.diagnostics.DiagnosticSeverity
import com.rift.engine.graphics.RenderContext
import com.rift.engine.input.EngineKey
import com.rift.engine.scene.Scene
import com.rift.engine.storage.SaveStore
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Line2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.Timer
import javax.swing.WindowConstants

class DesktopRenderContext(
    private val graphics: Graphics2D,
    override val width: Int,
    override val height: Int
) : RenderContext {
    init {
        graphics.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        )
    }

    override fun clear(color: Int) {
        graphics.color = Color(color, true)
        graphics.fillRect(0, 0, width, height)
    }

    override fun drawRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        color: Int
    ) {
        graphics.color = Color(color, true)
        graphics.fill(Rectangle2D.Float(left, top, right - left, bottom - top))
    }

    override fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radius: Float,
        color: Int
    ) {
        graphics.color = Color(color, true)
        graphics.fill(
            RoundRectangle2D.Float(
                left,
                top,
                right - left,
                bottom - top,
                radius * 2f,
                radius * 2f
            )
        )
    }

    override fun drawLine(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        color: Int,
        strokeWidth: Float
    ) {
        graphics.color = Color(color, true)
        graphics.stroke = BasicStroke(strokeWidth)
        graphics.draw(Line2D.Float(x1, y1, x2, y2))
    }

    override fun drawText(
        text: String,
        x: Float,
        y: Float,
        color: Int,
        size: Float,
        bold: Boolean
    ) {
        graphics.color = Color(color, true)
        graphics.font = Font(
            Font.SANS_SERIF,
            if (bold) Font.BOLD else Font.PLAIN,
            size.toInt().coerceAtLeast(1)
        )
        graphics.drawString(text, x, y)
    }

    override fun measureText(
        text: String,
        size: Float,
        bold: Boolean
    ): Float {
        val font = Font(
            Font.SANS_SERIF,
            if (bold) Font.BOLD else Font.PLAIN,
            size.toInt().coerceAtLeast(1)
        )
        return graphics.getFontMetrics(font).stringWidth(text).toFloat()
    }
}

class DesktopSaveStore(
    private val file: File,
    private val diagnostics: DiagnosticReporter = DiagnosticReporter()
) : SaveStore {
    private val properties = Properties()

    init {
        if (file.isFile) {
            try {
                file.inputStream().use(properties::load)
            } catch (exception: Exception) {
                properties.clear()
                diagnostics.report(
                    severity = DiagnosticSeverity.WARNING,
                    code = "DESKTOP_SAVE_READ_FAILED",
                    subsystem = "DesktopSaveStore",
                    message = "The save file could not be read. Defaults will be used.",
                    cause = exception.toString(),
                    location = file.absolutePath
                )
                preserveCorruptedFile()
            }
        }
    }

    @Synchronized
    override fun putString(key: String, value: String) {
        properties.setProperty("s.$key", value)
        flush()
    }

    @Synchronized
    override fun getString(key: String, defaultValue: String): String {
        return properties.getProperty("s.$key", defaultValue)
    }

    @Synchronized
    override fun putFloat(key: String, value: Float) {
        properties.setProperty("f.$key", value.toString())
        flush()
    }

    @Synchronized
    override fun getFloat(key: String, defaultValue: Float): Float {
        val storedValue = properties.getProperty("f.$key") ?: return defaultValue
        val parsedValue = storedValue.toFloatOrNull()

        if (parsedValue == null) {
            diagnostics.report(
                severity = DiagnosticSeverity.WARNING,
                code = "DESKTOP_SAVE_VALUE_INVALID",
                subsystem = "DesktopSaveStore",
                message = "A saved float value was invalid; the default value will be used.",
                cause = "Key: $key, value: $storedValue",
                location = file.absolutePath
            )
        }

        return parsedValue ?: defaultValue
    }

    private fun preserveCorruptedFile() {
        val backupFile = file.resolveSibling(
            "${file.name}.corrupt-${System.currentTimeMillis()}"
        )

        try {
            Files.move(file.toPath(), backupFile.toPath())
        } catch (exception: Exception) {
            diagnostics.report(
                severity = DiagnosticSeverity.WARNING,
                code = "DESKTOP_SAVE_BACKUP_FAILED",
                subsystem = "DesktopSaveStore",
                message = "The unreadable save file could not be moved to a backup.",
                cause = exception.toString(),
                location = file.absolutePath
            )
        }
    }

    private fun flush() {
        val parent = file.absoluteFile.parentFile
        val temporaryFile: File

        try {
            Files.createDirectories(parent.toPath())
            temporaryFile = Files.createTempFile(
                parent.toPath(),
                file.name,
                ".tmp"
            ).toFile()

            try {
                temporaryFile.outputStream().use { output ->
                    properties.store(output, "Rift saves")
                }

                try {
                    Files.move(
                        temporaryFile.toPath(),
                        file.toPath(),
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                    )
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(
                        temporaryFile.toPath(),
                        file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
            } finally {
                Files.deleteIfExists(temporaryFile.toPath())
            }
        } catch (exception: Exception) {
            diagnostics.reportException("DesktopSaveStore.flush", exception)
        }
    }
}

class DesktopGameWindow(
    private val engine: GameEngine,
    scene: Scene
) : JFrame("Rift Engine") {
    private val clock = FixedStepAccumulator()
    private var previousFrameNanos = System.nanoTime()

    private val panel = object : JPanel() {
        override fun getPreferredSize(): Dimension {
            return Dimension(960, 600)
        }

        override fun paintComponent(graphics: Graphics) {
            super.paintComponent(graphics)

            // Work on a private graphics copy so engine rendering cannot leak
            // modified color, font, stroke, or rendering hints into Swing.
            val gameGraphics = graphics.create() as Graphics2D
            try {
                engine.render(
                    DesktopRenderContext(gameGraphics, width, height)
                )
            } finally {
                gameGraphics.dispose()
            }
        }
    }

    init {
        engine.scenes.setInitialScene(scene)

        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        contentPane.add(panel)
        panel.isFocusable = true

        panel.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) {
                panel.requestFocusInWindow()
                sendPointerInput(event)
            }
        })

        panel.addMouseMotionListener(object : MouseAdapter() {
            override fun mouseDragged(event: MouseEvent) {
                sendPointerInput(event)
            }
        })

        panel.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(event: KeyEvent) {
                mapKey(event.keyCode)?.let { engine.onKey(it, true) }
            }

            override fun keyReleased(event: KeyEvent) {
                mapKey(event.keyCode)?.let { engine.onKey(it, false) }
            }
        })

        pack()
        setLocationRelativeTo(null)

        // This simple Swing timer is adequate for the current demo. A
        // dedicated desktop game thread is a planned host improvement before
        // introducing expensive per-frame simulation systems.
        Timer(16) {
            val now = System.nanoTime()
            val frameDelta = (now - previousFrameNanos) / 1_000_000_000f
            previousFrameNanos = now

            clock.advance(frameDelta) { fixedDelta ->
                engine.update(fixedDelta)
            }
            panel.repaint()
        }.start()
    }

    private fun sendPointerInput(event: MouseEvent) {
        engine.onTouch(
            event.x.toFloat(),
            event.y.toFloat(),
            panel.width.toFloat(),
            panel.height.toFloat()
        )
    }

    private fun mapKey(keyCode: Int): Int? {
        return when (keyCode) {
            KeyEvent.VK_A -> EngineKey.A
            KeyEvent.VK_D -> EngineKey.D
            KeyEvent.VK_W -> EngineKey.W
            KeyEvent.VK_S -> EngineKey.S
            KeyEvent.VK_LEFT -> EngineKey.LEFT
            KeyEvent.VK_RIGHT -> EngineKey.RIGHT
            KeyEvent.VK_UP -> EngineKey.UP
            KeyEvent.VK_DOWN -> EngineKey.DOWN
            else -> null
        }
    }
}
