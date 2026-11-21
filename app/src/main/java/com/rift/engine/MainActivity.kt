package com.rift.engine
import android.app.Activity
import android.os.Bundle
import com.rift.engine.android.RiftSurfaceView
import com.rift.engine.demo.SandboxGame
class MainActivity:Activity(){
 private lateinit var view:RiftSurfaceView
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);view=RiftSurfaceView(this,SandboxGame());setContentView(view)}
 override fun onPause(){view.pause();super.onPause()}
 override fun onResume(){super.onResume();view.resume()}
 override fun onDestroy(){view.stop();super.onDestroy()}
}
