package com.rift.engine.desktop

import com.rift.engine.assets.AssetManager
import com.rift.engine.core.GameEngine
import com.rift.engine.demo.SandboxGame
import com.rift.engine.diagnostics.DiagnosticReporter
import java.io.File
import javax.swing.SwingUtilities

fun main() {
    val diagnostics = DiagnosticReporter()
    val saveFile = File(
        System.getProperty("user.home"),
        ".rift-engine/saves.properties"
    )
    val saves = DesktopSaveStore(saveFile, diagnostics)
    val assets = AssetManager(DesktopTextureLoader(File("assets")), diagnostics)
    val engine = GameEngine(saves, diagnostics, assets)

    SwingUtilities.invokeLater {
        DesktopGameWindow(engine, SandboxGame()).isVisible = true
    }
}
