package com.rift.engine.desktop

import com.rift.engine.core.FixedStepAccumulator
import com.rift.engine.core.GameEngine
import com.rift.engine.diagnostics.DiagnosticReporter
import com.rift.engine.diagnostics.DiagnosticSeverity
import com.rift.engine.graphics.RenderContext
import com.rift.engine.graphics.ViewportRenderContext
import com.rift.engine.graphics.ViewportTransform
import com.rift.engine.input.EngineKey
import com.rift.engine.input.PointerButtons
import com.rift.engine.input.PointerEvent
import com.rift.engine.input.PointerEventType
import com.rift.engine.game.RiftGame
import com.rift.engine.storage.SaveStore
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.FocusAdapter
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.geom.AffineTransform
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
    graphics: Graphics2D,
    override val width: Int,
    override val height: Int
) : RenderContext {
    private var currentGraphics = graphics
    private val graphicsStack = mutableListOf<Graphics2D>()

    init {
        currentGraphics.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        )
    }

    override fun clear(color: Int) {
        currentGraphics.color = Color(color, true)
        currentGraphics.fillRect(0, 0, width, height)
    }

    override fun drawRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        color: Int
    ) {
        currentGraphics.color = Color(color, true)
        currentGraphics.fill(Rectangle2D.Float(left, top, right - left, bottom - top))
    }

    override fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radius: Float,
        color: Int
    ) {
        currentGraphics.color = Color(color, true)
        currentGraphics.fill(
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
        currentGraphics.color = Color(color, true)
        currentGraphics.stroke = BasicStroke(strokeWidth)
        currentGraphics.draw(Line2D.Float(x1, y1, x2, y2))
    }

    override fun drawText(
        text: String,
        x: Float,
        y: Float,
        color: Int,
        size: Float,
        bold: Boolean
    ) {
        currentGraphics.color = Color(color, true)
        currentGraphics.font = Font(
            Font.SANS_SERIF,
            if (bold) Font.BOLD else Font.PLAIN,
            size.toInt().coerceAtLeast(1)
        )
        currentGraphics.drawString(text, x, y)
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
        return currentGraphics.getFontMetrics(font).stringWidth(text).toFloat()
    }

    override fun pushState() {
        graphicsStack.add(currentGraphics)
        currentGraphics = currentGraphics.create() as Graphics2D
    }

    override fun popState() {
        check(graphicsStack.isNotEmpty()) { "Render state stack is empty." }
        currentGraphics.dispose()
        currentGraphics = graphicsStack.removeAt(graphicsStack.lastIndex)
    }

    override fun translate(x: Float, y: Float) {
        currentGraphics.translate(x.toDouble(), y.toDouble())
    }

    override fun rotate(degrees: Float) {
        currentGraphics.rotate(Math.toRadians(degrees.toDouble()))
    }

    override fun scale(x: Float, y: Float) {
        currentGraphics.scale(x.toDouble(), y.toDouble())
    }

    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float) {
        currentGraphics.clip(Rectangle2D.Float(left, top, right - left, bottom - top))
    }

    override fun drawTextureRegion(
        texture: com.rift.engine.graphics.TextureResource,
        sourceLeft: Int,
        sourceTop: Int,
        sourceRight: Int,
        sourceBottom: Int,
        destinationLeft: Float,
        destinationTop: Float,
        destinationRight: Float,
        destinationBottom: Float,
        alpha: Float
    ) {
        val desktopTexture = texture as? DesktopTexture
            ?: throw IllegalArgumentException("Texture was not created by the desktop texture loader.")
        check(!desktopTexture.isDisposed) { "Cannot draw a disposed texture." }
        require(alpha.isFinite() && alpha in 0f..1f) { "alpha must be between zero and one." }
        require(
            sourceLeft >= 0 && sourceTop >= 0 &&
                sourceRight <= texture.width && sourceBottom <= texture.height &&
                sourceLeft < sourceRight && sourceTop < sourceBottom
        ) { "Texture source rectangle is outside the texture bounds." }
        require(destinationRight > destinationLeft && destinationBottom > destinationTop) {
            "Texture destination rectangle must have positive dimensions."
        }

        val sourceWidth = (sourceRight - sourceLeft).toDouble()
        val sourceHeight = (sourceBottom - sourceTop).toDouble()
        val scaleX = (destinationRight - destinationLeft) / sourceWidth
        val scaleY = (destinationBottom - destinationTop) / sourceHeight
        val transform = AffineTransform(
            scaleX.toDouble(),
            0.0,
            0.0,
            scaleY.toDouble(),
            destinationLeft - sourceLeft * scaleX,
            destinationTop - sourceTop * scaleY
        )

        val child = currentGraphics.create() as Graphics2D
        try {
            child.clip(Rectangle2D.Float(
                destinationLeft,
                destinationTop,
                destinationRight - destinationLeft,
                destinationBottom - destinationTop
            ))
            child.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)
            child.drawImage(desktopTexture.image, transform, null)
        } finally {
            child.dispose()
        }
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
    private val game: RiftGame
) : JFrame("Rift Engine") {
    private val clock = FixedStepAccumulator()
    private var previousFrameNanos = System.nanoTime()

    private val panel = object : JPanel() {
        override fun getPreferredSize(): Dimension {
            return Dimension(game.viewportWidth, game.viewportHeight)
        }

        override fun paintComponent(graphics: Graphics) {
            super.paintComponent(graphics)

            // Work on a private graphics copy so engine rendering cannot leak
            // modified color, font, stroke, or rendering hints into Swing.
            val gameGraphics = graphics.create() as Graphics2D
            try {
                val baseContext = DesktopRenderContext(gameGraphics, width, height)
                val viewport = ViewportTransform(
                    game.viewportWidth,
                    game.viewportHeight,
                    width.coerceAtLeast(1),
                    height.coerceAtLeast(1)
                )
                val renderContext = ViewportRenderContext(baseContext, viewport)
                try {
                    engine.render(renderContext)
                } finally {
                    renderContext.close()
                }
            } finally {
                gameGraphics.dispose()
            }
        }
    }

    private val gameTimer = Timer(16) {
        val now = System.nanoTime()
        val frameDelta = (now - previousFrameNanos) / 1_000_000_000f
        previousFrameNanos = now
        clock.advance(frameDelta) { fixedDelta -> engine.update(fixedDelta) }
        panel.repaint()
    }

    init {
        engine.scenes.setInitialScene(game.createInitialScene())

        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosed(event: WindowEvent) {
                gameTimer.stop()
                engine.close()
            }
        })
        contentPane.add(panel)
        panel.isFocusable = true
        panel.background = Color.BLACK

        panel.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) {
                panel.requestFocusInWindow()
                engine.onPointer(pointerEvent(event, PointerEventType.DOWN, mapPointerButton(event.button)))
            }

            override fun mouseReleased(event: MouseEvent) {
                engine.onPointer(pointerEvent(event, PointerEventType.UP, mapPointerButton(event.button)))
            }
        })

        panel.addMouseMotionListener(object : MouseAdapter() {
            override fun mouseDragged(event: MouseEvent) {
                engine.onPointer(pointerEvent(event, PointerEventType.MOVE))
            }

            override fun mouseMoved(event: MouseEvent) {
                engine.onPointer(pointerEvent(event, PointerEventType.MOVE))
            }
        })

        panel.addFocusListener(object : FocusAdapter() {
            override fun focusLost(event: java.awt.event.FocusEvent) {
                engine.onFocusLost()
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

        gameTimer.start()
    }

    private fun pointerEvent(
        event: MouseEvent,
        type: PointerEventType,
        button: Int = PointerButtons.NONE
    ): PointerEvent {
        val viewport = ViewportTransform(
            game.viewportWidth,
            game.viewportHeight,
            panel.width.coerceAtLeast(1),
            panel.height.coerceAtLeast(1)
        )
        return PointerEvent(
            pointerId = 0,
            type = type,
            x = viewport.screenToLogicalX(event.x.toFloat()),
            y = viewport.screenToLogicalY(event.y.toFloat()),
            viewportWidth = game.viewportWidth.toFloat(),
            viewportHeight = game.viewportHeight.toFloat(),
            button = button
        )
    }

    private fun mapPointerButton(button: Int): Int = when (button) {
        MouseEvent.BUTTON1 -> PointerButtons.PRIMARY
        MouseEvent.BUTTON2 -> PointerButtons.MIDDLE
        MouseEvent.BUTTON3 -> PointerButtons.SECONDARY
        else -> PointerButtons.NONE
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
            KeyEvent.VK_SPACE -> EngineKey.SPACE
            KeyEvent.VK_ENTER -> EngineKey.ENTER
            KeyEvent.VK_ESCAPE -> EngineKey.ESCAPE
            KeyEvent.VK_TAB -> EngineKey.TAB
            KeyEvent.VK_E -> EngineKey.E
            KeyEvent.VK_Q -> EngineKey.Q
            KeyEvent.VK_F -> EngineKey.F
            KeyEvent.VK_R -> EngineKey.R
            KeyEvent.VK_SHIFT -> EngineKey.SHIFT
            KeyEvent.VK_CONTROL -> EngineKey.CTRL
            KeyEvent.VK_ALT -> EngineKey.ALT
            else -> null
        }
    }
}
