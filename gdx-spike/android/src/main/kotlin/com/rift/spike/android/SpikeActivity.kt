package com.rift.spike.android

import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.rift.spike.RiftGdxApplication

class SpikeActivity : AndroidApplication() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        initialize(RiftGdxApplication(), AndroidApplicationConfiguration())
    }
}
