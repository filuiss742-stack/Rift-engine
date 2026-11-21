package com.rift.engine.android
import android.content.Context
import android.graphics.*
import android.view.*
import com.rift.engine.assets.AssetManager
import com.rift.engine.audio.AudioManager
import com.rift.engine.core.*
import com.rift.engine.diagnostics.DiagnosticReporter
import com.rift.engine.graphics.RenderContext
import com.rift.engine.graphics.ViewportRenderContext
import com.rift.engine.graphics.ViewportTransform
import com.rift.engine.game.RiftGame
import com.rift.engine.input.EngineKey
import com.rift.engine.input.PointerButtons
import com.rift.engine.input.PointerEvent
import com.rift.engine.input.PointerEventType
import com.rift.engine.storage.SaveStore
import java.util.concurrent.locks.LockSupport

class AndroidRenderContext(private val canvas: Canvas) : RenderContext {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val saveCounts = mutableListOf<Int>()

    override val width: Int
        get() = canvas.width

    override val height: Int
        get() = canvas.height

    override fun clear(color: Int) {
        canvas.drawColor(color)
    }

    override fun drawRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        color: Int
    ) {
        paint.color = color
        paint.style = Paint.Style.FILL
        canvas.drawRect(left, top, right, bottom, paint)
    }

    override fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radius: Float,
        color: Int
    ) {
        paint.color = color
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(left, top, right, bottom), radius, radius, paint)
    }

    override fun drawLine(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        color: Int,
        strokeWidth: Float
    ) {
        paint.color = color
        paint.strokeWidth = strokeWidth
        paint.style = Paint.Style.STROKE
        canvas.drawLine(x1, y1, x2, y2, paint)
        paint.style = Paint.Style.FILL
    }

    override fun drawText(
        text: String,
        x: Float,
        y: Float,
        color: Int,
        size: Float,
        bold: Boolean
    ) {
        paint.color = color
        paint.textSize = size
        paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        canvas.drawText(text, x, y, paint)
    }

    override fun measureText(text: String, size: Float, bold: Boolean): Float {
        paint.textSize = size
        paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        return paint.measureText(text)
    }

    override fun pushState() {
        saveCounts.add(canvas.save())
    }

    override fun popState() {
        check(saveCounts.isNotEmpty()) { "Render state stack is empty." }
        canvas.restoreToCount(saveCounts.removeAt(saveCounts.lastIndex))
    }

    override fun translate(x: Float, y: Float) {
        canvas.translate(x, y)
    }

    override fun rotate(degrees: Float) {
        canvas.rotate(degrees)
    }

    override fun scale(x: Float, y: Float) {
        canvas.scale(x, y)
    }

    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float) {
        canvas.clipRect(left, top, right, bottom)
    }

    override fun drawTextureRegion(
        texture: com.rift.engine.graphics.TextureResource,
        sourceLeft: Int,
        sourceTop: Int,
        sourceRight: Int,
        sourceBottom: Int,
        destinationLeft: Float,
        destinationTop: Float,
        destinationRight: Float,
        destinationBottom: Float,
        alpha: Float
    ) {
        val androidTexture = texture as? AndroidTexture
            ?: throw IllegalArgumentException("Texture was not created by the Android texture loader.")
        check(!androidTexture.isDisposed) { "Cannot draw a disposed texture." }
        require(alpha.isFinite() && alpha in 0f..1f) { "alpha must be between zero and one." }
        require(
            destinationLeft.isFinite() && destinationTop.isFinite() &&
                destinationRight.isFinite() && destinationBottom.isFinite() &&
                destinationRight > destinationLeft && destinationBottom > destinationTop
        ) { "Texture destination rectangle must have positive finite dimensions." }
        require(
            sourceLeft >= 0 && sourceTop >= 0 &&
                sourceRight <= texture.width && sourceBottom <= texture.height &&
                sourceLeft < sourceRight && sourceTop < sourceBottom
        ) { "Texture source rectangle is outside the texture bounds." }

        val previousAlpha = paint.alpha
        val previousFilter = paint.isFilterBitmap
        paint.alpha = (alpha * 255f).toInt()
        paint.isFilterBitmap = true
        try {
            canvas.drawBitmap(
                androidTexture.bitmap,
                Rect(sourceLeft, sourceTop, sourceRight, sourceBottom),
                RectF(destinationLeft, destinationTop, destinationRight, destinationBottom),
                paint
            )
        } finally {
            paint.alpha = previousAlpha
            paint.isFilterBitmap = previousFilter
        }
    }
}
class AndroidSaveStore(context:Context):SaveStore {
 private val prefs=context.applicationContext.getSharedPreferences("rift_engine_save",Context.MODE_PRIVATE)
 override fun putString(key:String,value:String){prefs.edit().putString(key,value).apply()}
 override fun getString(key:String,defaultValue:String)=prefs.getString(key,defaultValue)?:defaultValue
 override fun putFloat(key:String,value:Float){prefs.edit().putFloat(key,value).apply()}
 override fun getFloat(key:String,defaultValue:Float)=prefs.getFloat(key,defaultValue)
}
class RiftSurfaceView(context:Context,private val game:RiftGame):SurfaceView(context),SurfaceHolder.Callback {
 private val diagnostics=DiagnosticReporter()
 private val assets=AssetManager(AndroidTextureLoader(context),diagnostics)
 private val audio=AudioManager(AndroidAudioLoader(context),diagnostics)
 private val engine=GameEngine(AndroidSaveStore(context),diagnostics,assets,audio)
 private val clock=FixedStepAccumulator()
 @Volatile private var running=false;private var thread:Thread?=null
 init{holder.addCallback(this);isFocusable=true;isFocusableInTouchMode=true;engine.scenes.setInitialScene(game.createInitialScene());requestFocus()}
 override fun surfaceCreated(h:SurfaceHolder){resume()}
 override fun surfaceDestroyed(h:SurfaceHolder){pause()}
 override fun surfaceChanged(h:SurfaceHolder,f:Int,w:Int,hh:Int)=Unit
 fun resume(){synchronized(this){if(running||thread?.isAlive==true)return;clock.reset();running=true;thread=Thread({loop()},"Rift-Android").also{it.start()}}}
 private fun loop(){var prev=System.nanoTime();val target=1_000_000_000L/60
  while(running&&!Thread.currentThread().isInterrupted){val start=System.nanoTime()
   try{val dt=((start-prev)/1e9).toFloat();prev=start;clock.advance(dt){engine.update(it)}
    var c:Canvas?=null;try{if(holder.surface.isValid){c=holder.lockCanvas();if(c!=null){
     val baseContext=AndroidRenderContext(c)
     val viewport=ViewportTransform(game.viewportWidth,game.viewportHeight,baseContext.width,baseContext.height)
     val renderContext=ViewportRenderContext(baseContext,viewport)
     try{engine.render(renderContext)}finally{renderContext.close()}
    }}}finally{if(c!=null)holder.unlockCanvasAndPost(c)}
   }catch(e:Exception){engine.diagnostics.reportException("android.loop",e)}
   val wait=target-(System.nanoTime()-start);if(wait>0)LockSupport.parkNanos(wait)
  }
 }
 fun pause(){val t=synchronized(this){running=false;thread?.also{it.interrupt()}}
  if(t!=null&&t!==Thread.currentThread())try{t.join(1000)}catch(_:InterruptedException){Thread.currentThread().interrupt()}
  synchronized(this){if(thread===t&&t?.isAlive!=true)thread=null}
 }
 fun stop(){pause();engine.close()}
 override fun onKeyDown(keyCode:Int,event:KeyEvent):Boolean{val key=mapKey(keyCode)?:return super.onKeyDown(keyCode,event);engine.onKey(key,true);return true}
 override fun onKeyUp(keyCode:Int,event:KeyEvent):Boolean{val key=mapKey(keyCode)?:return super.onKeyUp(keyCode,event);engine.onKey(key,false);return true}
 private fun mapKey(keyCode:Int):Int?=when(keyCode){
  KeyEvent.KEYCODE_A->EngineKey.A;KeyEvent.KEYCODE_D->EngineKey.D;KeyEvent.KEYCODE_W->EngineKey.W;KeyEvent.KEYCODE_S->EngineKey.S
  KeyEvent.KEYCODE_DPAD_LEFT->EngineKey.LEFT;KeyEvent.KEYCODE_DPAD_RIGHT->EngineKey.RIGHT;KeyEvent.KEYCODE_DPAD_UP->EngineKey.UP;KeyEvent.KEYCODE_DPAD_DOWN->EngineKey.DOWN
  KeyEvent.KEYCODE_SPACE->EngineKey.SPACE;KeyEvent.KEYCODE_ENTER->EngineKey.ENTER;KeyEvent.KEYCODE_ESCAPE->EngineKey.ESCAPE;KeyEvent.KEYCODE_TAB->EngineKey.TAB
  KeyEvent.KEYCODE_E->EngineKey.E;KeyEvent.KEYCODE_Q->EngineKey.Q;KeyEvent.KEYCODE_F->EngineKey.F;KeyEvent.KEYCODE_R->EngineKey.R
  KeyEvent.KEYCODE_SHIFT_LEFT,KeyEvent.KEYCODE_SHIFT_RIGHT->EngineKey.SHIFT
  KeyEvent.KEYCODE_CTRL_LEFT,KeyEvent.KEYCODE_CTRL_RIGHT->EngineKey.CTRL
  KeyEvent.KEYCODE_ALT_LEFT,KeyEvent.KEYCODE_ALT_RIGHT->EngineKey.ALT
  KeyEvent.KEYCODE_BUTTON_A->EngineKey.SPACE
  KeyEvent.KEYCODE_BUTTON_B->EngineKey.ESCAPE
  KeyEvent.KEYCODE_BUTTON_X->EngineKey.E
  KeyEvent.KEYCODE_BUTTON_Y->EngineKey.Q
  KeyEvent.KEYCODE_BUTTON_L1->EngineKey.SHIFT
  KeyEvent.KEYCODE_BUTTON_R1->EngineKey.CTRL
  KeyEvent.KEYCODE_BUTTON_START->EngineKey.ENTER
  KeyEvent.KEYCODE_BUTTON_SELECT->EngineKey.TAB
  else->null
 }
 override fun onGenericMotionEvent(event:MotionEvent):Boolean {
  if(event.action==MotionEvent.ACTION_MOVE &&
     (event.source and InputDevice.SOURCE_JOYSTICK)==InputDevice.SOURCE_JOYSTICK){
   engine.onAxis("gamepad.left.x",event.getAxisValue(MotionEvent.AXIS_X))
   engine.onAxis("gamepad.left.y",event.getAxisValue(MotionEvent.AXIS_Y))
   engine.onAxis("gamepad.right.x",event.getAxisValue(MotionEvent.AXIS_RX))
   engine.onAxis("gamepad.right.y",event.getAxisValue(MotionEvent.AXIS_RY))
   engine.onAxis("gamepad.left.trigger",event.getAxisValue(MotionEvent.AXIS_LTRIGGER))
   engine.onAxis("gamepad.right.trigger",event.getAxisValue(MotionEvent.AXIS_RTRIGGER))
   engine.onAxis("gamepad.dpad.x",event.getAxisValue(MotionEvent.AXIS_HAT_X))
   engine.onAxis("gamepad.dpad.y",event.getAxisValue(MotionEvent.AXIS_HAT_Y))
   return true
  }
  return super.onGenericMotionEvent(event)
 }
 override fun onTouchEvent(e:MotionEvent):Boolean {
  val viewport=ViewportTransform(
   game.viewportWidth,
   game.viewportHeight,
   width.coerceAtLeast(1),
   height.coerceAtLeast(1)
  )
  val viewportWidth=game.viewportWidth.toFloat()
  val viewportHeight=game.viewportHeight.toFloat()
  fun logicalX(index:Int)=viewport.screenToLogicalX(e.getX(index))
  fun logicalY(index:Int)=viewport.screenToLogicalY(e.getY(index))
  when(e.actionMasked){
   MotionEvent.ACTION_DOWN,MotionEvent.ACTION_POINTER_DOWN -> {
    val i=e.actionIndex
    engine.onPointer(PointerEvent(e.getPointerId(i),PointerEventType.DOWN,logicalX(i),logicalY(i),viewportWidth,viewportHeight,PointerButtons.TOUCH))
   }
   MotionEvent.ACTION_MOVE -> {
    for(i in 0 until e.pointerCount){
     engine.onPointer(PointerEvent(e.getPointerId(i),PointerEventType.MOVE,logicalX(i),logicalY(i),viewportWidth,viewportHeight))
    }
   }
   MotionEvent.ACTION_UP,MotionEvent.ACTION_POINTER_UP -> {
    val i=e.actionIndex
    engine.onPointer(PointerEvent(e.getPointerId(i),PointerEventType.UP,logicalX(i),logicalY(i),viewportWidth,viewportHeight,PointerButtons.TOUCH))
   }
   MotionEvent.ACTION_CANCEL -> {
    for(i in 0 until e.pointerCount){
     engine.onPointer(PointerEvent(e.getPointerId(i),PointerEventType.CANCEL,logicalX(i),logicalY(i),viewportWidth,viewportHeight))
    }
   }
  }
  return true
 }
 override fun onWindowFocusChanged(hasWindowFocus:Boolean){
  super.onWindowFocusChanged(hasWindowFocus)
  if(!hasWindowFocus)engine.onFocusLost()
 }
}
