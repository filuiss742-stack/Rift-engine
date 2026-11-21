package com.rift.engine.physics
data class Aabb(val left:Float,val top:Float,val right:Float,val bottom:Float) {
 init { require(left<=right && top<=bottom) }
}
object Collision {
 fun intersects(a:Aabb,b:Aabb)=a.left<b.right && b.left<a.right && a.top<b.bottom && b.top<a.bottom
 fun clamp(value:Float,min:Float,max:Float)=value.coerceIn(min,max)
}
