package com.rift.engine.scene
import com.rift.engine.entity.GameObject
import com.rift.engine.graphics.RenderContext
abstract class Scene {
 protected val objects=mutableListOf<GameObject>()
 protected fun add(gameObject:GameObject){objects+=gameObject}
 open fun onEnter()=Unit
 open fun onExit()=Unit
 open fun update(deltaSeconds:Float){objects.toList().forEach{it.update(deltaSeconds)}}
 open fun render(context:RenderContext){objects.forEach{it.render(context)}}
 open fun onTouch(x:Float,y:Float,w:Float,h:Float)=Unit
 open fun onKey(keyCode:Int,pressed:Boolean)=Unit
}
