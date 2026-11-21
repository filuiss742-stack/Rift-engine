package com.rift.engine.diagnostics
import com.rift.engine.graphics.RenderContext
/** Fallback screen uses only the shared render contract, not platform UI or game assets. */
object DiagnosticsOverlay {
 fun render(c:RenderContext,entries:List<DiagnosticEntry>){
  val bg=0xFF11141D.toInt();val white=0xFFFFFFFF.toInt();val orange=0xFFFFBE5F.toInt();val body=0xFFDCE1EB.toInt()
  c.clear(bg);var y=44f
  c.drawText("RIFT ENGINE / DIAGNOSTICS",24f,y,white,28f,true);y+=34f
  val error=entries.lastOrNull{it.severity==DiagnosticSeverity.ERROR}
  if(error!=null){c.drawText(error.code+" • "+error.subsystem,24f,y,orange,19f,true);y+=28f
   c.drawText(error.message,24f,y,body,16f);y+=24f
  }else{c.drawText("No runtime errors recorded.",24f,y,body,16f);y+=24f}
  c.drawText("Recent events: "+entries.size,24f,y,orange,18f,true)
 }
}
