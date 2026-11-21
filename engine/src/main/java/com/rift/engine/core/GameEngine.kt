package com.rift.engine.core
import com.rift.engine.diagnostics.*
import com.rift.engine.graphics.RenderContext
import com.rift.engine.plugin.PluginManager
import com.rift.engine.storage.*
import java.util.concurrent.ConcurrentLinkedQueue
private sealed interface InputEvent {
 data class Pointer(val x:Float,val y:Float,val w:Float,val h:Float):InputEvent
 data class Key(val code:Int,val pressed:Boolean):InputEvent
}
/** Shared runtime. A platform host provides the loop, renderer and persistence. */
class GameEngine(val saves:SaveStore=InMemorySaveStore()) {
 val diagnostics=DiagnosticReporter()
 val plugins=PluginManager(diagnostics)
 val scenes=SceneManager(diagnostics)
 private val inputs=ConcurrentLinkedQueue<InputEvent>()
 fun onTouch(x:Float,y:Float,w:Float,h:Float){if(!diagnostics.hasErrors())inputs.offer(InputEvent.Pointer(x,y,w,h))}
 fun onKey(code:Int,pressed:Boolean){if(!diagnostics.hasErrors())inputs.offer(InputEvent.Key(code,pressed))}
 fun update(deltaSeconds:Float){
  if(diagnostics.hasErrors())return
  safe("input.dispatch"){
   while(true){
    when(val e=inputs.poll()?:break){
     is InputEvent.Pointer->scenes.onTouch(e.x,e.y,e.w,e.h)
     is InputEvent.Key->scenes.onKey(e.code,e.pressed)
    }
    if(diagnostics.hasErrors())break
   }
  }
  if(!diagnostics.hasErrors())safe("plugins.update"){plugins.onUpdate(deltaSeconds)}
  if(!diagnostics.hasErrors())safe("scene.update"){scenes.update(deltaSeconds)}
 }
 fun render(c:RenderContext){
  if(diagnostics.hasErrors()){DiagnosticsOverlay.render(c,diagnostics.snapshot());return}
  safe("scene.render"){scenes.render(c)}
  if(diagnostics.hasErrors())DiagnosticsOverlay.render(c,diagnostics.snapshot())
 }
 private inline fun safe(name:String,block:()->Unit){try{block()}catch(e:Exception){diagnostics.reportException(name,e)}}
}
