package com.rift.engine.graphics
/** Platform-neutral renderer API. Coordinates are logical pixels; colors are ARGB ints. */
interface RenderContext {
 val width:Int
 val height:Int
 fun clear(color:Int)
 fun drawRect(left:Float,top:Float,right:Float,bottom:Float,color:Int)
 fun drawRoundRect(left:Float,top:Float,right:Float,bottom:Float,radius:Float,color:Int)
 fun drawLine(x1:Float,y1:Float,x2:Float,y2:Float,color:Int,strokeWidth:Float=1f)
 fun drawText(text:String,x:Float,y:Float,color:Int,size:Float,bold:Boolean=false)
 fun measureText(text:String,size:Float,bold:Boolean=false):Float
}
