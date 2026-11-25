package com.rift.spike.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.rift.spike.RiftGdxApplication

fun main() {
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("Rift Engine — libGDX Integration Spike")
        setWindowedMode(960, 540)
        useVsync(true)
    }
    Lwjgl3Application(RiftGdxApplication(), config)
}
