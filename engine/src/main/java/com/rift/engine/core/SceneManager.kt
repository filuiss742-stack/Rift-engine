package com.rift.engine.core
import com.rift.engine.diagnostics.*
import com.rift.engine.graphics.RenderContext
import com.rift.engine.scene.Scene
class SceneManager(private val diagnostics:DiagnosticReporter=DiagnosticReporter()) {
 var current:Scene?=null; private set
 fun setInitialScene(scene:Scene){
  if(current==null){current=scene;safe("scene.onEnter"){scene.onEnter()}}
  else diagnostics.report(DiagnosticSeverity.WARNING,"SCENE_INITIAL_ALREADY_SET","SceneManager","Use changeScene() to replace the active scene.")
 }
 fun changeScene(scene:Scene){current?.let{safe("scene.onExit"){it.onExit()}};current=scene;safe("scene.onEnter"){scene.onEnter()}}
 fun update(deltaSeconds:Float){current?.update(deltaSeconds)}
 fun render(c:RenderContext){val s=current;if(s==null)diagnostics.report(DiagnosticSeverity.ERROR,"SCENE_NOT_SET","SceneManager","Call setInitialScene() before rendering.") else s.render(c)}
 fun onTouch(x:Float,y:Float,w:Float,h:Float){current?.onTouch(x,y,w,h)}
 fun onKey(code:Int,pressed:Boolean){current?.onKey(code,pressed)}
 private inline fun safe(name:String,block:()->Unit){try{block()}catch(e:Exception){diagnostics.reportException(name,e)}}
}
