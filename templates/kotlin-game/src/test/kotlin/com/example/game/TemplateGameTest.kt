package com.example.game

import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateGameTest {
    @Test
    fun createsAnIndependentWorldScene() {
        val scene = TemplateGame().createInitialScene()

        assertTrue(scene is WorldScene)
    }
}
