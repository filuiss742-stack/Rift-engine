package com.rift.engine.demo

import com.rift.engine.entity.GameComponent
import com.rift.engine.entity.GameObject
import com.rift.engine.graphics.RenderContext
import com.rift.engine.graphics.Sprite
import com.rift.engine.graphics.TextureRegion
import com.rift.engine.input.EngineKey
import com.rift.engine.input.PointerEvent
import com.rift.engine.input.PointerEventType
import com.rift.engine.movement.MovementInput
import com.rift.engine.movement.TopDownMovementProfile
import com.rift.engine.scene.Scene
import com.rift.engine.ui.UiLabel
import com.rift.engine.ui.VirtualButton
import com.rift.engine.ui.VirtualJoystick
import kotlin.math.sin

/**
 * First playable prologue slice: rift intro -> modern apartment -> street ->
 * return home -> sleep -> wake in the second heroine's body.
 * Dialogue prose is intentionally not authored here; player-facing text is neutral scaffolding.
 */
class ModernPrologueScene : Scene() {
    private enum class Phase { INTRO, APARTMENT, STREET, FANTASY_WAKE, COMPLETE }

    private var phase = Phase.INTRO
    private var introTime = 0f
    private var sleepFade = 0f
    private var messageTime = 0f
    private var setupComplete = false
    private var hasReturnedHome = false
    private var touchInteract = false
    private var interactionMessage = ""
    private val completedApartmentTasks = linkedSetOf<String>()
    private val completedStreetTasks = linkedSetOf<String>()
    private val interactableObjects = mutableMapOf<String, PrologueObject>()
    private val objectEntities = mutableListOf<GameObject>()
    private val mapTextures = mutableMapOf<String, com.rift.engine.graphics.TextureResource>()
    private val objectTextures = mutableMapOf<String, com.rift.engine.graphics.TextureResource>()
    private var heroineTexture: com.rift.engine.graphics.TextureResource? = null
    private var activeMap: PrologueMap = ModernPrologueWorld.apartment

    private val heroine = GameObject(462f, 420f, 54f, 82f)
    private val walkProfile = TopDownMovementProfile(168f)
    private val joystick = VirtualJoystick(24f, 458f, 112f)
    private val interactButton = VirtualButton(
        834f, 468f, 100f, 78f, "USE",
        onPressed = { touchInteract = true },
        idleColor = 0xBB55466D.toInt(),
        pressedColor = 0xDD80638F.toInt()
    )
    private val skipButton = VirtualButton(
        822f, 22f, 112f, 48f, "SKIP",
        onPressed = { finishIntro() },
        idleColor = 0xAA6D304A.toInt(),
        pressedColor = 0xDD9D3C60.toInt()
    )
    private val objective = UiLabel(18f, 16f, 760f, 28f, "", 0xFFF1EAF6.toInt(), 17f, true)
    private val hint = UiLabel(18f, 46f, 760f, 24f, "", 0xFFD7C9E1.toInt(), 13f)
    private val toast = UiLabel(210f, 510f, 540f, 34f, "", 0xFFFFFFFF.toInt(), 17f, true)
    private val endLabel = UiLabel(120f, 254f, 720f, 84f, "", 0xFFFFFFFF.toInt(), 24f, true)

    init {
        input.bindKey("left", EngineKey.A); input.bindKey("left", EngineKey.LEFT)
        input.bindKey("right", EngineKey.D); input.bindKey("right", EngineKey.RIGHT)
        input.bindKey("up", EngineKey.W); input.bindKey("up", EngineKey.UP)
        input.bindKey("down", EngineKey.S); input.bindKey("down", EngineKey.DOWN)
        input.bindKey("interact", EngineKey.E); input.bindKey("interact", EngineKey.ENTER)
        input.bindKey("skip", EngineKey.ESCAPE)
        heroine.movementProfile = walkProfile
        heroine.renderDefaultVisual = false
        add(heroine)
        ui.add(objective); ui.add(hint); ui.add(toast); ui.add(joystick); ui.add(interactButton); ui.add(skipButton); ui.add(endLabel)
        objective.visible = false; hint.visible = false; toast.visible = false
        joystick.visible = false; interactButton.visible = false; endLabel.visible = false
    }

    override fun onEnter() {
        if (setupComplete) return
        setupComplete = true
        val manager = assets ?: return
        heroineTexture = manager.loadTexture("prologue.heroine.idle", "prologue/characters/heroine/heroine_idle.png")
        mapTextures[ModernPrologueWorld.apartment.background] = manager.loadTexture("prologue.map.apartment", ModernPrologueWorld.apartment.background) ?: return
        mapTextures[ModernPrologueWorld.street.background] = manager.loadTexture("prologue.map.street", ModernPrologueWorld.street.background) ?: return
        mapTextures[ModernPrologueWorld.fantasyWakeBackground] = manager.loadTexture("prologue.map.fantasy-wake", ModernPrologueWorld.fantasyWakeBackground) ?: return
        val spriteNames = setOf("bed", "desk", "fridge", "phone", "sink", "bench", "tree", "storefront", "door")
        spriteNames.forEach { name ->
            manager.loadTexture("prologue.object.$name", "prologue/objects/$name.png")?.let { objectTextures[name] = it }
        }
        heroineTexture?.let { texture -> heroine.addComponent(TextureSpriteComponent(Sprite(TextureRegion(texture, 0, 0, texture.width, texture.height)))) }
        loadMap(ModernPrologueWorld.apartment)
    }

    override fun update(deltaSeconds: Float) {
        when (phase) {
            Phase.INTRO -> {
                introTime += deltaSeconds
                if (input.wasPressed("skip") || input.wasPressed("interact") || introTime >= 6.2f) finishIntro()
                return
            }
            Phase.FANTASY_WAKE, Phase.COMPLETE -> {
                if (phase == Phase.FANTASY_WAKE) sleepFade = (sleepFade - deltaSeconds * 1.5f).coerceAtLeast(0f)
                if (phase == Phase.FANTASY_WAKE && (input.wasPressed("interact") || touchInteract)) {
                    touchInteract = false
                    phase = Phase.COMPLETE
                    endLabel.text = "ПРОЛОГ ЗАВЕРШЁН — ДАЛЬШЕ ВТОРОЙ МИР"
                    endLabel.visible = true
                }
                return
            }
            else -> Unit
        }

        val moveX = ((if (input.isDown("right")) 1f else 0f) - (if (input.isDown("left")) 1f else 0f) + joystick.axisX).coerceIn(-1f, 1f)
        val moveY = ((if (input.isDown("down")) 1f else 0f) - (if (input.isDown("up")) 1f else 0f) + joystick.axisY).coerceIn(-1f, 1f)
        val length = kotlin.math.sqrt(moveX * moveX + moveY * moveY)
        val dx = if (length > 1f) moveX / length else moveX
        val dy = if (length > 1f) moveY / length else moveY
        val oldX = heroine.x
        val oldY = heroine.y
        heroine.movementInput = MovementInput(dx, dy)
        super.update(deltaSeconds)
        resolveMovement(oldX, oldY)
        heroine.renderLayer = heroine.y.toInt() + 100
        updateCamera()

        if (messageTime > 0f) {
            messageTime = (messageTime - deltaSeconds).coerceAtLeast(0f)
            toast.visible = messageTime > 0f
        }
        if (input.wasPressed("interact") || touchInteract) {
            touchInteract = false
            interact()
        }
        updateObjective()
    }

    override fun render(context: RenderContext) {
        context.clear(0xFF11101B.toInt())
        when (phase) {
            Phase.INTRO -> renderIntro(context)
            Phase.FANTASY_WAKE, Phase.COMPLETE -> {
                val bg = mapTextures[ModernPrologueWorld.fantasyWakeBackground]
                if (bg != null) context.drawTexture(bg, 0f, 0f, 960f, 600f)
                context.drawRect(0f, 0f, 960f, 600f, 0x55201A35)
                context.drawText("...", 452f, 500f, 0xFFFFFFFF.toInt(), 28f, true)
                super.render(context)
                if (phase == Phase.FANTASY_WAKE) {
                    context.drawRoundRect(210f, 500f, 750f, 566f, 12f, 0xDD171421.toInt())
                    context.drawText("Нажми USE, чтобы продолжить", 270f, 540f, 0xFFF1EAF6.toInt(), 19f, true)
                }
            }
            else -> {
                val background = mapTextures[activeMap.background]
                if (background != null) camera.withTransform(context) {
                    context.drawTexture(background, 0f, 0f, activeMap.width, activeMap.height)
                }
                super.render(context)
            }
        }
        if (sleepFade > 0f) {
            context.drawRect(0f, 0f, 960f, 600f, (((sleepFade.coerceIn(0f, 1f) * 230f).toInt()) shl 24) or 0x00101020)
        }
    }

    override fun onPointer(event: PointerEvent) {
        if (phase == Phase.INTRO && event.type == PointerEventType.DOWN) {
            finishIntro()
        } else if (phase == Phase.FANTASY_WAKE && event.type == PointerEventType.DOWN) {
            touchInteract = true
        }
        super.onPointer(event)
    }

    private fun renderIntro(context: RenderContext) {
        val t = introTime
        context.drawRect(0f, 0f, 960f, 600f, 0xFF05050B.toInt())
        val opening = (t / 2.4f).coerceIn(0f, 1f)
        val cx = 480f
        val cy = 290f
        val half = 2f + opening * 34f
        context.drawLine(cx, cy - 12f - half, cx + 3f, cy - half, 0xFFFFFFFF.toInt(), 3f)
        context.drawLine(cx + 3f, cy - half, cx - 2f, cy + half, 0xFFF3F2FF.toInt(), 4f)
        context.drawLine(cx - 2f, cy + half, cx + 1f, cy + half + 18f, 0xFFFFFFFF.toInt(), 2f)
        val pulse = (sin(t * 9f) * 0.5f + 0.5f)
        for (i in 0 until 42) {
            val progress = ((t * 0.72f + i * 0.071f) % 1f)
            val side = if (i % 2 == 0) -1f else 1f
            val spread = (1f - progress) * (220f + (i % 7) * 18f)
            val x = cx + side * spread
            val y = cy + sin(i * 2.7f + t * 4f) * (24f + spread * 0.18f)
            val size = 2f + (i % 4) * 1.6f
            context.drawRect(x, y, x + size * 2f, y + size, if (i % 3 == 0) 0xFFFF355C.toInt() else 0xFFB71E4D.toInt())
            if (i % 4 == 0) context.drawLine(x, y, cx, cy + (if (i % 2 == 0) -4f else 4f), 0x66FF315A, 1.2f)
        }
        context.drawRect(0f, 0f, 960f, 600f, ((pulse * 20f).toInt() shl 24) or 0x00FF2048)
        val flash = if (t > 5.25f) ((t - 5.25f) / 0.95f).coerceIn(0f, 1f) else 0f
        if (flash > 0f) context.drawRect(0f, 0f, 960f, 600f, ((flash * 255f).toInt() shl 24) or 0x00FFFFFF)
        skipButton.visible = true
        skipButton.render(context)
    }

    private fun finishIntro() {
        if (phase != Phase.INTRO) return
        phase = Phase.APARTMENT
        skipButton.visible = false
        joystick.visible = true
        interactButton.visible = true
        objective.visible = true
        hint.visible = true
        updateObjective()
    }

    private fun loadMap(map: PrologueMap) {
        objectEntities.forEach(::remove)
        objectEntities.clear()
        interactableObjects.clear()
        activeMap = map
        heroine.x = map.spawnX
        heroine.y = map.spawnY
        map.objects.forEach { item ->
            val texture = objectTextures[item.sprite] ?: return@forEach
            val entity = GameObject(item.x, item.y, item.width, item.height)
            entity.renderDefaultVisual = false
            entity.renderLayer = (item.y + item.height).toInt()
            entity.addComponent(TextureSpriteComponent(Sprite(TextureRegion(texture, 0, 0, texture.width, texture.height))))
            add(entity)
            objectEntities.add(entity)
            if (item.interactable) interactableObjects[item.id] = item
        }
        updateCamera()
    }

    private fun updateCamera() {
        camera.x = (heroine.x + heroine.width * 0.5f - 480f).coerceIn(0f, maxOf(0f, activeMap.width - 960f))
        camera.y = (heroine.y + heroine.height * 0.5f - 300f).coerceIn(0f, maxOf(0f, activeMap.height - 600f))
    }

    private fun resolveMovement(oldX: Float, oldY: Float) {
        heroine.x = heroine.x.coerceIn(24f, activeMap.width - heroine.width - 24f)
        heroine.y = heroine.y.coerceIn(42f, activeMap.height - heroine.height - 18f)
        if (activeMap.blockers.any { it.intersects(heroine.x + 8f, heroine.y + 12f, heroine.width - 16f, heroine.height - 16f) }) {
            val candidateX = heroine.x
            val candidateY = heroine.y
            heroine.x = candidateX
            heroine.y = oldY
            if (activeMap.blockers.any { it.intersects(heroine.x + 8f, heroine.y + 12f, heroine.width - 16f, heroine.height - 16f) }) {
                heroine.x = oldX
                heroine.y = candidateY
            }
            if (activeMap.blockers.any { it.intersects(heroine.x + 8f, heroine.y + 12f, heroine.width - 16f, heroine.height - 16f) }) {
                heroine.x = oldX
                heroine.y = oldY
            }
        }
    }

    private fun interact() {
        val nearest = interactableObjects.values
            .map { it to distanceTo(it) }
            .filter { it.second <= it.first.radius }
            .minByOrNull { it.second }?.first
        if (nearest == null) {
            showMessage("Рядом нет объекта для взаимодействия.")
            return
        }
        when (phase) {
            Phase.APARTMENT -> when (nearest.id) {
                "bed" -> {
                    if (hasReturnedHome && completedStreetTasks.size >= 2) {
                        phase = Phase.FANTASY_WAKE
                        objectEntities.forEach(::remove)
                        objectEntities.clear()
                        interactableObjects.clear()
                        joystick.visible = false; interactButton.visible = true
                        objective.visible = false; hint.visible = false; toast.visible = false
                        heroine.x = 450f; heroine.y = 340f
                        sleepFade = 1f
                    } else showMessage("Сначала закончи свои дела.")
                }
                "exit" -> if (completedApartmentTasks.size >= 3) {
                    phase = Phase.STREET
                    loadMap(ModernPrologueWorld.street)
                } else showMessage("Пока рано выходить.")
                else -> {
                    completedApartmentTasks.add(nearest.id)
                    showMessage("Дело отмечено.")
                }
            }
            Phase.STREET -> when (nearest.id) {
                "home" -> if (completedStreetTasks.size >= 2) {
                    phase = Phase.APARTMENT
                    hasReturnedHome = true
                    loadMap(ModernPrologueWorld.apartment)
                } else showMessage("Ещё остались дела в городе.")
                "tree_a", "tree_b" -> Unit
                else -> {
                    completedStreetTasks.add(nearest.id)
                    showMessage("Дело отмечено.")
                }
            }
            else -> Unit
        }
        updateObjective()
    }

    private fun distanceTo(item: PrologueObject): Float {
        val dx = heroine.x + heroine.width * 0.5f - (item.x + item.width * 0.5f)
        val dy = heroine.y + heroine.height * 0.5f - (item.y + item.height * 0.5f)
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private fun updateObjective() {
        when (phase) {
            Phase.APARTMENT -> {
                objective.text = "СОВРЕМЕННЫЙ МИР • КВАРТИРА"
                hint.text = "Исследуй комнату и закончи повседневные дела (" + completedApartmentTasks.size + "/3)."
            }
            Phase.STREET -> {
                objective.text = "СОВРЕМЕННЫЙ МИР • УЛИЦА"
                hint.text = "Закончи дела снаружи (" + completedStreetTasks.size + "/2), затем возвращайся домой."
            }
            else -> Unit
        }
    }

    private fun showMessage(text: String) {
        interactionMessage = text
        toast.text = interactionMessage
        toast.visible = true
        messageTime = 1.8f
    }

    private fun sin(value: Float): Float = kotlin.math.sin(value.toDouble()).toFloat()

    private class TextureSpriteComponent(private val sprite: Sprite) : GameComponent {
        override fun render(owner: GameObject, context: RenderContext) {
            sprite.draw(context, 0f, 0f, owner.width, owner.height)
        }
    }
}