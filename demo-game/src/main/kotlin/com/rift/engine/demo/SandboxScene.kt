package com.rift.engine.demo
import com.rift.engine.entity.GameObject
import com.rift.engine.graphics.RenderContext
import com.rift.engine.input.EngineKey
import com.rift.engine.scene.Scene
/** Same demo scene is compiled into both platform applications. */
class SandboxScene:Scene(){
 private val player=GameObject(140f,220f,64f,64f,0xFF6996FF.toInt())
 init{add(player);add(GameObject(420f,170f,100f,100f,0xFFFFAA50.toInt()));add(GameObject(650f,350f,80f,80f,0xFF78DCAA.toInt()))}
 override fun render(c:RenderContext){
  c.clear(0xFF0F1320.toInt())
  for(x in 0..c.width step 64)c.drawLine(x.toFloat(),0f,x.toFloat(),c.height.toFloat(),0x23A0BEFF)
  for(y in 0..c.height step 64)c.drawLine(0f,y.toFloat(),c.width.toFloat(),y.toFloat(),0x23A0BEFF)
  super.render(c);c.drawText("RIFT ENGINE • SHARED GAME",32f,52f,0xFFFFFFFF.toInt(),30f,true)
  c.drawText("Touch / click to move",32f,86f,0xFFFFFFFF.toInt(),20f);c.drawText("WASD / arrows: move",32f,114f,0xFFFFFFFF.toInt(),16f)
 }
 override fun onTouch(x:Float,y:Float,screenWidth:Float,screenHeight:Float){player.x=x-player.width/2;player.y=y-player.height/2}
 override fun onKey(keyCode:Int,pressed:Boolean){
  if(!pressed)return
  val d=5f
  when(keyCode){EngineKey.A,EngineKey.LEFT->player.x-=d;EngineKey.D,EngineKey.RIGHT->player.x+=d;EngineKey.W,EngineKey.UP->player.y-=d;EngineKey.S,EngineKey.DOWN->player.y+=d}
 }
}
