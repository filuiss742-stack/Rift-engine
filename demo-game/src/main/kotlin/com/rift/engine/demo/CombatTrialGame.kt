package com.rift.engine.demo

import com.rift.engine.game.RiftGame
import com.rift.engine.scene.Scene

/** Separate entry point for the combat, sprite, dungeon and narrative acceptance demo. */
class CombatTrialGame : RiftGame {
    override fun createInitialScene(): Scene = CombatTrialScene()
}
