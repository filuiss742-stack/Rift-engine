package com.rift.engine.desktop
import com.rift.engine.core.*
import com.rift.engine.graphics.RenderContext
import com.rift.engine.input.EngineKey
import com.rift.engine.scene.Scene
import com.rift.engine.storage.SaveStore
import java.awt.*
import java.awt.event.*
import java.io.File
import java.util.Properties
import javax.swing.*

class DesktopRenderContext(private val g:Graphics2D,override val width:Int,override val height:Int):RenderContext {
 init{g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON)}
 override fun clear(color:Int){g.color=Color(color,true);g.fillRect(0,0,width,height)}
 override fun drawRect(l:Float,t:Float,r:Float,b:Float,color:Int){g.color=Color(color,true);g.fillRect(l.toInt(),t.toInt(),(r-l).toInt(),(b-t).toInt())}
 override fun drawRoundRect(l:Float,t:Float,r:Float,b:Float,rad:Float,color:Int){g.color=Color(color,true);g.fillRoundRect(l.toInt(),t.toInt(),(r-l).toInt(),(b-t).toInt(),(rad*2).toInt(),(rad*2).toInt())}
 override fun drawLine(x1:Float,y1:Float,x2:Float,y2:Float,color:Int,strokeWidth:Float){g.color=Color(color,true);g.stroke=BasicStroke(strokeWidth);g.drawLine(x1.toInt(),y1.toInt(),x2.toInt(),y2.toInt())}
 override fun drawText(text:String,x:Float,y:Float,color:Int,size:Float,bold:Boolean){g.color=Color(color,true);g.font=Font(Font.SANS_SERIF,if(bold)Font.BOLD else Font.PLAIN,size.toInt().coerceAtLeast(1));g.drawString(text,x,y)}
 override fun measureText(text:String,size:Float,bold:Boolean):Float{val f=Font(Font.SANS_SERIF,if(bold)Font.BOLD else Font.PLAIN,size.toInt().coerceAtLeast(1));return g.getFontMetrics(f).stringWidth(text).toFloat()}
}
class DesktopSaveStore(private val file:File):SaveStore {
 private val props=Properties()
 init{if(file.isFile)file.inputStream().use(props::load)}
 @Synchronized override fun putString(key:String,value:String){props.setProperty("s.$key",value);flush()}
 @Synchronized override fun getString(key:String,defaultValue:String)=props.getProperty("s.$key",defaultValue)
 @Synchronized override fun putFloat(key:String,value:Float){props.setProperty("f.$key",value.toString());flush()}
 @Synchronized override fun getFloat(key:String,defaultValue:Float)=props.getProperty("f.$key")?.toFloatOrNull()?:defaultValue
 private fun flush(){file.parentFile?.mkdirs();file.outputStream().use{props.store(it,"Rift saves")}}
}
class DesktopGameWindow(private val engine:GameEngine,scene:Scene):JFrame("Rift Engine") {
 private val clock=FixedStepAccumulator();private var previous=System.nanoTime()
 private val panel=object:JPanel(){
  override fun getPreferredSize()=Dimension(960,600)
  override fun paintComponent(g:Graphics){super.paintComponent(g);engine.render(DesktopRenderContext(g as Graphics2D,width,height))}
 }
 init{
  engine.scenes.setInitialScene(scene);defaultCloseOperation=WindowConstants.EXIT_ON_CLOSE;contentPane.add(panel);panel.isFocusable=true
  panel.addMouseListener(object:MouseAdapter(){override fun mousePressed(e:MouseEvent){panel.requestFocusInWindow();engine.onTouch(e.x.toFloat(),e.y.toFloat(),panel.width.toFloat(),panel.height.toFloat())}})
  panel.addMouseMotionListener(object:MouseAdapter(){override fun mouseDragged(e:MouseEvent){engine.onTouch(e.x.toFloat(),e.y.toFloat(),panel.width.toFloat(),panel.height.toFloat())}})
  panel.addKeyListener(object:KeyAdapter(){
   override fun keyPressed(e:KeyEvent){mapKey(e.keyCode)?.let{engine.onKey(it,true)}}
   override fun keyReleased(e:KeyEvent){mapKey(e.keyCode)?.let{engine.onKey(it,false)}}
  })
  pack();setLocationRelativeTo(null)
  Timer(16){val now=System.nanoTime();clock.advance(((now-previous)/1e9).toFloat()){engine.update(it)};previous=now;panel.repaint()}.start()
 }
 private fun mapKey(code:Int):Int?=when(code){
  KeyEvent.VK_A->EngineKey.A;KeyEvent.VK_D->EngineKey.D;KeyEvent.VK_W->EngineKey.W;KeyEvent.VK_S->EngineKey.S
  KeyEvent.VK_LEFT->EngineKey.LEFT;KeyEvent.VK_RIGHT->EngineKey.RIGHT;KeyEvent.VK_UP->EngineKey.UP;KeyEvent.VK_DOWN->EngineKey.DOWN
  else->null
 }
}
