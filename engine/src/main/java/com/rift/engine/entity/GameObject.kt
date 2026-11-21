package com.rift.engine.entity
import com.rift.engine.graphics.RenderContext
import com.rift.engine.movement.*
import com.rift.engine.physics.Aabb
open class GameObject(x:Float,y:Float,var width:Float,var height:Float,var color:Int=0xFFFFFFFF.toInt()) {
 val movementBody=MovementBody(x,y)
 var x:Float get()=movementBody.x; set(v){movementBody.x=v}
 var y:Float get()=movementBody.y; set(v){movementBody.y=v}
 var velocityX:Float get()=movementBody.velocityX; set(v){movementBody.velocityX=v}
 var velocityY:Float get()=movementBody.velocityY; set(v){movementBody.velocityY=v}
 var isGrounded:Boolean get()=movementBody.isGrounded; set(v){movementBody.isGrounded=v}
 var movementProfile:MovementProfile?=null
 var movementInput:MovementInput=MovementInput()
 var active=true
 open fun update(deltaSeconds:Float){
  if(!active)return
  movementProfile?.update(movementBody,movementInput,deltaSeconds)
  x+=velocityX*deltaSeconds; y+=velocityY*deltaSeconds
 }
 open fun render(context:RenderContext){if(active)context.drawRoundRect(x,y,x+width,y+height,12f,color)}
 fun bounds()=Aabb(x,y,x+width,y+height)
}
