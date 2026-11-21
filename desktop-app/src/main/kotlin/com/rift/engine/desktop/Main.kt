package com.rift.engine.desktop
import com.rift.engine.core.GameEngine
import com.rift.engine.demo.SandboxScene
import java.io.File
import javax.swing.SwingUtilities
fun main(){
 val engine=GameEngine(DesktopSaveStore(File(System.getProperty("user.home"),".rift-engine/saves.properties")))
 SwingUtilities.invokeLater{DesktopGameWindow(engine,SandboxScene()).isVisible=true}
}
