package com.rift.engine.android
import android.content.Context
import android.graphics.*
import android.view.*
import com.rift.engine.core.*
import com.rift.engine.graphics.RenderContext
import com.rift.engine.game.RiftGame
import com.rift.engine.storage.SaveStore
import java.util.concurrent.locks.LockSupport

class AndroidRenderContext(private val canvas:Canvas):RenderContext {
 private val p=Paint(Paint.ANTI_ALIAS_FLAG)
 override val width get()=canvas.width
 override val height get()=canvas.height
 override fun clear(color:Int){canvas.drawColor(color)}
 override fun drawRect(left:Float,top:Float,right:Float,bottom:Float,color:Int){p.color=color;p.style=Paint.Style.FILL;canvas.drawRect(left,top,right,bottom,p)}
 override fun drawRoundRect(left:Float,top:Float,right:Float,bottom:Float,radius:Float,color:Int){p.color=color;p.style=Paint.Style.FILL;canvas.drawRoundRect(RectF(left,top,right,bottom),radius,radius,p)}
 override fun drawLine(x1:Float,y1:Float,x2:Float,y2:Float,color:Int,strokeWidth:Float){p.color=color;p.strokeWidth=strokeWidth;canvas.drawLine(x1,y1,x2,y2,p)}
 override fun drawText(text:String,x:Float,y:Float,color:Int,size:Float,bold:Boolean){p.color=color;p.textSize=size;p.typeface=if(bold)Typeface.DEFAULT_BOLD else Typeface.DEFAULT;canvas.drawText(text,x,y,p)}
 override fun measureText(text:String,size:Float,bold:Boolean):Float{p.textSize=size;p.typeface=if(bold)Typeface.DEFAULT_BOLD else Typeface.DEFAULT;return p.measureText(text)}
}
class AndroidSaveStore(context:Context):SaveStore {
 private val prefs=context.applicationContext.getSharedPreferences("rift_engine_save",Context.MODE_PRIVATE)
 override fun putString(key:String,value:String){prefs.edit().putString(key,value).apply()}
 override fun getString(key:String,defaultValue:String)=prefs.getString(key,defaultValue)?:defaultValue
 override fun putFloat(key:String,value:Float){prefs.edit().putFloat(key,value).apply()}
 override fun getFloat(key:String,defaultValue:Float)=prefs.getFloat(key,defaultValue)
}
class RiftSurfaceView(context:Context,game:RiftGame):SurfaceView(context),SurfaceHolder.Callback {
 private val engine=GameEngine(AndroidSaveStore(context));private val clock=FixedStepAccumulator()
 @Volatile private var running=false;private var thread:Thread?=null
 init{holder.addCallback(this);isFocusable=true;engine.scenes.setInitialScene(game.createInitialScene())}
 override fun surfaceCreated(h:SurfaceHolder){resume()}
 override fun surfaceDestroyed(h:SurfaceHolder){pause()}
 override fun surfaceChanged(h:SurfaceHolder,f:Int,w:Int,hh:Int)=Unit
 fun resume(){synchronized(this){if(running||thread?.isAlive==true)return;clock.reset();running=true;thread=Thread({loop()},"Rift-Android").also{it.start()}}}
 private fun loop(){var prev=System.nanoTime();val target=1_000_000_000L/60
  while(running&&!Thread.currentThread().isInterrupted){val start=System.nanoTime()
   try{val dt=((start-prev)/1e9).toFloat();prev=start;clock.advance(dt){engine.update(it)}
    var c:Canvas?=null;try{if(holder.surface.isValid){c=holder.lockCanvas();if(c!=null)engine.render(AndroidRenderContext(c))}}finally{if(c!=null)holder.unlockCanvasAndPost(c)}
   }catch(e:Exception){engine.diagnostics.reportException("android.loop",e)}
   val wait=target-(System.nanoTime()-start);if(wait>0)LockSupport.parkNanos(wait)
  }
 }
 fun pause(){val t=synchronized(this){running=false;thread?.also{it.interrupt()}}
  if(t!=null&&t!==Thread.currentThread())try{t.join(1000)}catch(_:InterruptedException){Thread.currentThread().interrupt()}
  synchronized(this){if(thread===t&&t?.isAlive!=true)thread=null}
 }
 fun stop()=pause()
 override fun onTouchEvent(e:MotionEvent):Boolean{if(e.action==MotionEvent.ACTION_DOWN||e.action==MotionEvent.ACTION_MOVE)engine.onTouch(e.x,e.y,width.toFloat(),height.toFloat());return true}
}
