package com.rift.engine.entity

import com.rift.engine.graphics.RenderContext

/**
 * Optional behavior attached to a GameObject.
 *
 * Components receive their owner explicitly. This keeps ownership visible
 * and avoids hidden global entity lookups.
 */
interface GameComponent {
    fun onAttach(owner: GameObject) = Unit
    fun onDetach(owner: GameObject) = Unit
    fun update(owner: GameObject, deltaSeconds: Float) = Unit
    fun render(owner: GameObject, context: RenderContext) = Unit
}
