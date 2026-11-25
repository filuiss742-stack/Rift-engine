package com.rift.engine.demo

import com.rift.engine.graphics.RenderContext
import com.rift.engine.graphics3d.Camera3D
import com.rift.engine.graphics3d.DirectionalSpriteAnimation
import com.rift.engine.graphics3d.DirectionalSpriteRenderer
import com.rift.engine.graphics3d.SpriteDirection8
import com.rift.engine.graphics3d.Software3DRenderer
import com.rift.engine.graphics3d.Vector3
import com.rift.engine.graphics3d.Mesh3D
import com.rift.engine.graphics3d.cuboidMesh
import com.rift.engine.graphics3d.CuboidMaterialSet
import com.rift.engine.graphics3d.texturedCuboidMesh
import com.rift.engine.graphics3d.texturedGroundPlaneMesh
import com.rift.engine.input.EngineKey
import com.rift.engine.input.PointerEvent
import com.rift.engine.input.PointerEventType
import com.rift.engine.scene.Scene
import com.rift.engine.ui.UiLabel
import com.rift.engine.ui.VirtualButton
import com.rift.engine.ui.VirtualJoystick
import kotlin.math.sqrt

/**
 * Playable narrative pass for "The Crack" and Act I, "A Strange Dream".
 * Environments are world-space 3D meshes; the heroine remains a 2D billboard.
 * Dialogue is authored prototype copy and deliberately avoids explaining the mystery.
 */
class HybridPrologueScene : Scene() {
    private enum class Phase { INTRO, APARTMENT, STREET, FANTASY_WAKE, FANTASY_CAMP, COMPLETE }

    private data class Prop(
        val id: String,
        val position: Vector3,
        val radius: Float,
        val mesh: Mesh3D,
        val task: Boolean = true
    )

    private val renderer = Software3DRenderer()
    private var phase = Phase.INTRO
    private var introTime = 0f
    private var fade = 0f
    private var toastTimer = 0f
    private var showUse = false
    private var returnedHome = false
    private var dialogueOpen = false
    private var dialogueLines = emptyList<Pair<String, String>>()
    private var dialogueIndex = 0
    private var dialogueAfter: (() -> Unit)? = null
    private var touchAttack = false
    private var touchDodge = false
    private var enemyHealth = 3
    private var fantasyStart = Vector3.ZERO
    private var combatStarted = false
    private var enemyAttackTimer = 2.2f
    private var enemyTelegraph = 0f
    private var enemyWindupTarget = Vector3.ZERO
    private var heroHealth = 3
    private var heroInvulnerability = 0f
    private var damageFlash = 0f
    private var dodgeTimer = 0f
    private var attackCooldown = 0f
    private var glitchTimer = 0f
    private var glitchTriggered = false
    private val apartmentTasks = linkedSetOf<String>()
    private val streetTasks = linkedSetOf<String>()
    private var props = emptyList<Prop>()
    private var meshes = emptyList<Mesh3D>()
    private var fantasyBaseMeshes: List<Mesh3D> = emptyList()
    private var heroPosition = Vector3(-2.2f, 0f, 1.4f)
    private val heroSpeed = 3.1f
    private val worldCamera = Camera3D(
        position = Vector3(0f, 5.2f, 8.5f),
        target = Vector3.ZERO,
        verticalFovRadians = Math.toRadians(54.0).toFloat(),
        aspectRatio = 16f / 9f,
        near = 0.08f,
        far = 120f
    )
    private var heroTexture: com.rift.engine.graphics.TextureResource? = null
    private var heroSpriteAnimation: DirectionalSpriteAnimation? = null
    private var heroFacing = Vector3(0f,0f,1f)
    private var heroAnimationTime = 0f
    private var woodFloorTexture: com.rift.engine.graphics.TextureResource? = null
    private var asphaltTexture: com.rift.engine.graphics.TextureResource? = null
    private var brickTexture: com.rift.engine.graphics.TextureResource? = null
    private var concreteTexture: com.rift.engine.graphics.TextureResource? = null
    private var stoneTexture: com.rift.engine.graphics.TextureResource? = null
    private var mossyRockTexture: com.rift.engine.graphics.TextureResource? = null
    private val joystick = VirtualJoystick(24f, 454f, 112f)
    private val useButton = VirtualButton(
        832f, 468f, 104f, 78f, "USE",
        onPressed = { showUse = true },
        idleColor = 0xBB55466D.toInt(),
        pressedColor = 0xDD80638F.toInt()
    )
    private val objective = UiLabel(18f, 16f, 760f, 28f, "", 0xFFF1EAF6.toInt(), 17f, true)
    private val hint = UiLabel(18f, 46f, 820f, 24f, "", 0xFFD7C9E1.toInt(), 13f)
    private val combatStatus = UiLabel(18f, 76f, 340f, 26f, "", 0xFFFFD2D9.toInt(), 14f, true)
    private val toast = UiLabel(210f, 510f, 540f, 34f, "", 0xFFFFFFFF.toInt(), 17f, true)
    private val dialogueName = UiLabel(48f, 420f, 240f, 28f, "", 0xFFFFC5D0.toInt(), 16f, true)
    private val dialogueText = UiLabel(48f, 452f, 860f, 86f, "", 0xFFF5F0F8.toInt(), 18f)
    private val attackButton = VirtualButton(700f, 465f, 92f, 76f, "HIT", onPressed = { touchAttack = true })
    private val dodgeButton = VirtualButton(802f, 382f, 92f, 76f, "DODGE", onPressed = { touchDodge = true })
    private val endLabel = UiLabel(120f, 250f, 720f, 84f, "", 0xFFFFFFFF.toInt(), 24f, true)

    init {
        input.bindKey("left", EngineKey.A); input.bindKey("left", EngineKey.LEFT)
        input.bindKey("right", EngineKey.D); input.bindKey("right", EngineKey.RIGHT)
        input.bindKey("forward", EngineKey.W); input.bindKey("forward", EngineKey.UP)
        input.bindKey("back", EngineKey.S); input.bindKey("back", EngineKey.DOWN)
        input.bindKey("use", EngineKey.E); input.bindKey("use", EngineKey.ENTER)
        input.bindKey("skip", EngineKey.ESCAPE)
        input.bindKey("attack", EngineKey.SPACE)
        input.bindKey("dodge", EngineKey.SHIFT)
        ui.add(objective); ui.add(hint); ui.add(combatStatus); ui.add(toast); ui.add(joystick); ui.add(useButton)
        ui.add(dialogueName); ui.add(dialogueText); ui.add(attackButton); ui.add(dodgeButton); ui.add(endLabel)
        objective.visible = false; hint.visible = false; combatStatus.visible = false; toast.visible = false
        dialogueName.visible = false; dialogueText.visible = false
        joystick.visible = false; useButton.visible = false; attackButton.visible = false; dodgeButton.visible = false
        endLabel.visible = false
    }

    override fun onEnter() {
        woodFloorTexture = assets?.loadTexture("prologue.material.wood-floor", "prologue/materials/wood_floor.png")
        asphaltTexture = assets?.loadTexture("prologue.material.asphalt", "prologue/materials/asphalt_02.png")
        brickTexture = assets?.loadTexture("prologue.material.brick", "prologue/materials/brick_wall_001.png")
        concreteTexture = assets?.loadTexture("prologue.material.concrete", "prologue/materials/concrete_wall_008.png")
        stoneTexture = assets?.loadTexture("prologue.material.stone-wall", "prologue/materials/stone_wall.png")
        mossyRockTexture = assets?.loadTexture("prologue.material.mossy-rock", "prologue/materials/mossy_rock.png")
        heroTexture = assets?.loadTexture(
            "prologue.heroine.legacy-idle",
            "prologue/characters/heroine/heroine_idle.png"
        )
        heroSpriteAnimation = heroTexture?.let { texture ->
            DirectionalSpriteAnimation(
                framesByDirection = SpriteDirection8.values().associateWith { listOf(texture) }
            )
        }
        fantasyBaseMeshes = buildFantasyBaseMeshes()
        buildApartment()
        phase = Phase.INTRO
        // Music is intentionally disabled until real, supported audio assets are integrated.
    }

    override fun onExit() {
        audio?.stopMusic()
    }

    override fun update(deltaSeconds: Float) {
        heroAnimationTime += deltaSeconds
        if (phase == Phase.INTRO) {
            introTime += deltaSeconds
            if (input.wasPressed("skip") || input.wasPressed("use") || introTime >= 5.5f) beginApartment()
            return
        }
        if (dialogueOpen) {
            if (input.wasPressed("use") || input.wasPressed("attack") || showUse) advanceDialogue()
            showUse = false
            return
        }
        if (phase == Phase.FANTASY_WAKE) {
            fade = (fade - deltaSeconds * 1.2f).coerceAtLeast(0f)
            if (fade <= 0f) {
                phase = Phase.FANTASY_CAMP
                showUse = false
                joystick.visible = true; useButton.visible = true
                attackButton.visible = true; dodgeButton.visible = true
                objective.visible = true; hint.visible = true; combatStatus.visible = true
                heroHealth = 3; heroInvulnerability = 0f; damageFlash = 0f
                combatStatus.text = "СОСТОЯНИЕ: 3 / 3"
                updateObjective()
                startDialogue(listOf(
                    "Лиза" to "Я помню, как легла спать. Это точно не моя комната.",
                    "Лиза" to "Костёр... руины... И почему я чувствую чужую усталость?",
                    "Лиза" to "Мира. Это имя всплыло само. Я что, действительно в чужом теле?",
                    "Лиза" to "Ладно. Сначала надо понять, где я и как отсюда выбраться."
                ))
            }
            return
        }
        if (phase == Phase.FANTASY_CAMP) {
            updateFantasyCombat(deltaSeconds)
            return
        }
        if (phase == Phase.COMPLETE) {
            fade = (fade - deltaSeconds * .72f).coerceAtLeast(0f)
            return
        }
        if (fade > 0f) fade = (fade - deltaSeconds * 1.4f).coerceAtLeast(0f)
        if (toastTimer > 0f) { toastTimer = (toastTimer - deltaSeconds).coerceAtLeast(0f); toast.visible = toastTimer > 0f }

        val ix = ((if (input.isDown("right")) 1f else 0f) -
            (if (input.isDown("left")) 1f else 0f) + joystick.axisX).coerceIn(-1f, 1f)
        val iz = ((if (input.isDown("back")) 1f else 0f) -
            (if (input.isDown("forward")) 1f else 0f) + joystick.axisY).coerceIn(-1f, 1f)
        val length = sqrt(ix * ix + iz * iz)
        val nx = if (length > 1f) ix / length else ix
        val nz = if (length > 1f) iz / length else iz
        if (length > .001f) heroFacing = Vector3(nx,0f,nz)
        val candidate = Vector3(
            heroPosition.x + nx * heroSpeed * deltaSeconds,
            0f,
            heroPosition.z + nz * heroSpeed * deltaSeconds
        )
        if (isWalkable(candidate)) heroPosition = candidate

        worldCamera.position = Vector3(heroPosition.x + 0.2f, 5.0f, heroPosition.z + 8.0f)
        worldCamera.target = Vector3(heroPosition.x, 1.0f, heroPosition.z - 0.8f)
        if (input.wasPressed("use") || showUse) {
            showUse = false
            interact()
        }
        updateObjective()
    }

    override fun render(context: RenderContext) {
        context.clear(0xFF10121B.toInt())
        when (phase) {
            Phase.INTRO -> renderIntro(context)
            Phase.FANTASY_WAKE, Phase.FANTASY_CAMP, Phase.COMPLETE -> {
                renderFantasyWake(context)
                if (dialogueOpen) {
                    context.drawRoundRect(28f, 400f, context.width - 28f, context.height - 22f, 14f, 0xEE171421.toInt())
                }
                if (phase == Phase.COMPLETE) {
                    endLabel.text = "ПРОЛОГ ЗАВЕРШЁН"
                    endLabel.visible = true
                }
                super.render(context)
            }
            else -> {
                worldCamera.aspectRatio = context.width.toFloat() / context.height.coerceAtLeast(1)
                val frameMeshes = meshes.toMutableList()
                val heroMesh = buildHeroSpriteMesh(worldCamera)
                if (heroMesh != null) frameMeshes += heroMesh
                renderer.render(context, worldCamera, frameMeshes)
                if (heroMesh == null) drawHeroBillboard(context)
                if (dialogueOpen) context.drawRoundRect(28f, 400f, context.width - 28f, context.height - 22f, 14f, 0xEE171421.toInt())
                super.render(context)
            }
        }
        if (fade > 0f) {
            context.drawRect(0f, 0f, context.width.toFloat(), context.height.toFloat(),
                ((fade.coerceIn(0f, 1f) * 230f).toInt() shl 24) or 0x00101020)
        }
    }

    override fun onPointer(event: PointerEvent) {
        if (event.type == PointerEventType.DOWN) {
            if (phase == Phase.INTRO) beginApartment()
            else if (dialogueOpen) showUse = true
            else if (phase == Phase.FANTASY_WAKE) showUse = true
        }
        super.onPointer(event)
    }

    private fun beginApartment() {
        if (phase != Phase.INTRO) return
        phase = Phase.APARTMENT
        fade = 0.7f
        joystick.visible = true; useButton.visible = true
        objective.visible = true; hint.visible = true
        updateObjective()
        startDialogue(listOf(
            "Лиза" to "Пять минут. Я заслужила хотя бы пять минут без будильника.",
            "Лиза" to "Ладно. Встала — уже достижение."
        ))
    }

    private fun buildApartment() {
        phase = Phase.APARTMENT
        heroPosition = Vector3(-2.2f, 0f, 1.4f)
        val built = mutableListOf<Mesh3D>()
        val interactables = mutableListOf<Prop>()
        val wood = woodFloorTexture
        built += if (wood != null) texturedGroundPlaneMesh(Vector3(0f, 0f, 0f), 12f, 10f, wood, 6f, 5f)
            else cuboidMesh(Vector3(0f, -.08f, 0f), Vector3(12f, .16f, 10f), 0xFF6B5548.toInt())
        // Photographed CC0 plaster is used on the room walls instead of flat color blocks.
        val plaster = concreteTexture
        fun wall(center: Vector3, size: Vector3) {
            built += if (plaster != null) texturedCuboidMesh(center, size, CuboidMaterialSet(plaster, plaster, plaster, plaster, plaster, plaster), 2.5f, 1.5f)
                else cuboidMesh(center, size, 0xFF514B5A.toInt(), 0xFF3C3944.toInt())
        }
        wall(Vector3(0f, 1.6f, -5f), Vector3(12f, 3.2f, .22f))
        wall(Vector3(-6f, 1.6f, 0f), Vector3(.22f, 3.2f, 10f))
        wall(Vector3(6f, 1.6f, 0f), Vector3(.22f, 3.2f, 10f))
        // Window, bed, desk, fridge, sink and phone are independent 3D props.
        fun prop(id: String, p: Vector3, size: Vector3, color: Int, radius: Float = 1.6f, texture: com.rift.engine.graphics.TextureResource? = null) {
            val mesh = if (texture != null) texturedCuboidMesh(p, size, CuboidMaterialSet(texture, texture, texture, texture, texture, texture), 2f, 1.5f)
                else cuboidMesh(p, size, color, shade(color, .78f), shade(color, .62f))
            built += mesh
            interactables += Prop(id, Vector3(p.x, 0f, p.z), radius, mesh)
        }
        prop("bed", Vector3(-3.7f,.34f,-2.9f), Vector3(2.8f,.68f,2.1f), 0xFF555D79.toInt(), 2.2f)
        prop("desk", Vector3(3.5f,.58f,-3.7f), Vector3(2.3f,1.16f,1.1f), 0xFF795B45.toInt(), texture = woodFloorTexture)
        prop("fridge", Vector3(4.8f,1.0f,2.4f), Vector3(.95f,2f,.9f), 0xFFB9BEC8.toInt(), texture = concreteTexture)
        prop("sink", Vector3(2.4f,.48f,2.8f), Vector3(1.5f,.96f,.8f), 0xFF9CA9B2.toInt(), texture = concreteTexture)
        prop("phone", Vector3(-.4f,.08f,-.2f), Vector3(.28f,.16f,.5f), 0xFF272A35.toInt(), 1.4f)
        prop("tv", Vector3(-2.4f,1.75f,-4.72f), Vector3(2.0f,1.2f,.12f), 0xFF171923.toInt(), 2.1f)
        prop("exit", Vector3(5.8f,1.1f,-.2f), Vector3(.18f,2.2f,1.3f), 0xFF49382F.toInt(), 1.5f)
        // Window glass and sill.
        built += cuboidMesh(Vector3(0f,2.15f,-4.86f), Vector3(3.1f,1.55f,.04f), 0xFF263E59.toInt(), 0xFF263E59.toInt())
        built += cuboidMesh(Vector3(0f,1.34f,-4.78f), Vector3(3.4f,.12f,.32f), 0xFFB7A99B.toInt())
        props = interactables
        meshes = built
    }

    private fun buildStreet() {
        phase = Phase.STREET
        heroPosition = Vector3(-8f,0f,2.5f)
        val built = mutableListOf<Mesh3D>()
        val interactables = mutableListOf<Prop>()
        val road = asphaltTexture
        val sidewalk = concreteTexture
        if (road != null) built += texturedGroundPlaneMesh(Vector3(0f,0f,0f), 30f, 24f, road, 18f, 14f)
        else built += cuboidMesh(Vector3(0f,-.08f,0f), Vector3(30f,.16f,24f), 0xFF565A5C.toInt())
        if (sidewalk != null) built += texturedGroundPlaneMesh(Vector3(0f,.018f,2.5f), 30f, 4.2f, sidewalk, 10f, 1.4f)
        else built += cuboidMesh(Vector3(0f,-.04f,2.5f), Vector3(30f,.08f,4.2f), 0xFFB0AAA0.toInt())
        fun prop(id:String,p:Vector3,size:Vector3,color:Int,radius:Float=1.8f,texture:com.rift.engine.graphics.TextureResource?=null) {
            val mesh = if (texture != null) texturedCuboidMesh(p,size,CuboidMaterialSet(texture,texture,texture,texture,texture,texture), 2.5f, 1.8f)
                else cuboidMesh(p,size,color,shade(color,.78f),shade(color,.62f))
            built+=mesh; interactables+=Prop(id,Vector3(p.x,0f,p.z),radius,mesh)
        }
        fun assembly(id:String,position:Vector3,radius:Float,vararg parts:Mesh3D) {
            val mesh=Mesh3D(parts.flatMap { it.triangles })
            built+=mesh
            interactables+=Prop(id,position,radius,mesh)
        }
        prop("store",Vector3(-10f,1.7f,-7f),Vector3(5f,3.4f,3f),0xFF8B6D5C.toInt(),3f,brickTexture)

        // A bench is assembled from a timber seat/back and four separate metal supports.
        val benchParts=mutableListOf<Mesh3D>()
        val timber=woodFloorTexture
        fun woodPart(p:Vector3,size:Vector3) = if(timber!=null)
            texturedCuboidMesh(p,size,CuboidMaterialSet(timber,timber,timber,timber,timber,timber), 2.5f, 1.5f)
        else cuboidMesh(p,size,0xFF795B45.toInt())
        benchParts+=woodPart(Vector3(2f,.68f,-1f),Vector3(2.5f,.14f,.72f))
        benchParts+=woodPart(Vector3(2f,1.02f,-1.32f),Vector3(2.5f,.68f,.12f))
        for(x in listOf(.95f,3.05f)) for(z in listOf(-1.25f,-.78f))
            benchParts+=cuboidMesh(Vector3(x,.36f,z),Vector3(.11f,.62f,.11f),0xFF303943.toInt())
        assembly("bench",Vector3(2f,0f,-1f),1.8f,*benchParts.toTypedArray())

        // The notice board has a post and a framed sign instead of one floating cube.
        assembly("message",Vector3(5f,0f,1f),1.4f,
            cuboidMesh(Vector3(5f,.75f,1f),Vector3(.10f,1.5f,.10f),0xFF343A42.toInt()),
            cuboidMesh(Vector3(5f,1.38f,1f),Vector3(1.0f,.72f,.12f),0xFF252A35.toInt()),
            cuboidMesh(Vector3(5f,1.38f,.925f),Vector3(.86f,.56f,.02f),0xFFD6C9AE.toInt())
        )

        // Bus stop: two supports, a roof, a glass-like dark rear panel and a small bench.
        assembly("busstop",Vector3(-2f,0f,-4f),2.1f,
            cuboidMesh(Vector3(-2.82f,.95f,-4f),Vector3(.10f,1.9f,.10f),0xFF65727D.toInt()),
            cuboidMesh(Vector3(-1.18f,.95f,-4f),Vector3(.10f,1.9f,.10f),0xFF65727D.toInt()),
            cuboidMesh(Vector3(-2f,1.95f,-4f),Vector3(2.0f,.14f,1.05f),0xFF424C59.toInt()),
            cuboidMesh(Vector3(-2f,1.0f,-4.42f),Vector3(1.72f,1.55f,.055f),0xFF526B78.toInt()),
            cuboidMesh(Vector3(-2f,.42f,-3.82f),Vector3(1.35f,.10f,.36f),0xFF795B45.toInt())
        )

        prop("shop",Vector3(5.5f,1.3f,-6f),Vector3(3.2f,2.6f,2f),0xFF8B6D5C.toInt(),2.4f,brickTexture)
        prop("home",Vector3(11f,1.7f,-7f),Vector3(5f,3.4f,3f),0xFF666C79.toInt(),3f,concreteTexture)
        for (x in listOf(-13f,-4f,7f,14f)) {
            // Street lamp with a post, short arm and lantern housing.
            built+=cuboidMesh(Vector3(x,1.35f,-10f),Vector3(.12f,2.7f,.12f),0xFF55483D.toInt())
            built+=cuboidMesh(Vector3(x+.30f,2.65f,-10f),Vector3(.68f,.09f,.10f),0xFF55483D.toInt())
            built+=cuboidMesh(Vector3(x+.60f,2.48f,-10f),Vector3(.34f,.34f,.34f),0xFFFFD99A.toInt(),0xFFB98D5C.toInt())
            // Keep low-poly trees behind the street frontage, away from the follow-camera sightline.
            built+=cuboidMesh(Vector3(x,1.45f,-13f),Vector3(.38f,2.9f,.38f),0xFF624D3D.toInt())
            built+=cuboidMesh(Vector3(x,3.0f,-13f),Vector3(1.35f,1.15f,1.35f),0xFF315541.toInt(),0xFF284736.toInt())
            built+=cuboidMesh(Vector3(x-.42f,2.85f,-13.05f),Vector3(.85f,.95f,.92f),0xFF3C684B.toInt())
            built+=cuboidMesh(Vector3(x+.40f,2.92f,-12.96f),Vector3(.82f,1.0f,.90f),0xFF284736.toInt())
        }
        props=interactables; meshes=built
    }

    private fun interact() {
        val nearest = props.map { it to distance(heroPosition,it.position) }
            .filter { it.second <= it.first.radius }.minByOrNull { it.second }?.first
        if (nearest == null) { showToast("Подойди ближе к объекту."); return }
        when (phase) {
            Phase.APARTMENT -> when (nearest.id) {
                "bed" -> if (returnedHome && streetTasks.size >= 2) {
                    startDialogue(listOf(
                        "Лиза" to "Наконец-то. Завтра разберусь с этим странным институтом.",
                        "Лиза" to "Хотя какое мне дело до их экспериментов?.."
                    )) {
                        phase=Phase.FANTASY_WAKE; fade=1f
                        heroPosition=Vector3(0f,0f,1.8f)
                        fantasyStart=heroPosition
                        combatStarted=false; enemyHealth=3; enemyAttackTimer=2.2f; enemyTelegraph=0f
                        enemyWindupTarget=Vector3.ZERO; heroHealth=3; heroInvulnerability=0f; damageFlash=0f
                        combatStatus.visible=false
                        joystick.visible=false; useButton.visible=false
                        attackButton.visible=false; dodgeButton.visible=false
                        objective.visible=false; hint.visible=false; toast.visible=false
                        props=emptyList(); meshes=emptyList()
                    }
                } else showToast("Сначала закончи день.")
                "exit" -> if (apartmentTasks.containsAll(setOf("phone","fridge","tv"))) buildStreet()
                    else showToast("Телефон, завтрак и новости. Обычное утро.")
                "phone" -> {
                    apartmentTasks.add("phone")
                    startDialogue(listOf(
                        "Сообщение • Аня" to "Ты сегодня вообще собираешься выйти из дома? Или опять объявишься, когда все уже разойдутся?",
                        "Лиза" to "Я выйду. Просто у меня талант собираться ровно столько, сколько нужно, чтобы опоздать.",
                        "Аня" to "Это не талант. Это стабильность."
                    ))
                }
                "fridge" -> {
                    apartmentTasks.add("fridge")
                    startDialogue(listOf(
                        "Лиза" to "Кофе, тост и обещание начать нормально питаться с понедельника.",
                        "Лиза" to "Какого именно понедельника — история умалчивает."
                    ))
                }
                "tv" -> {
                    apartmentTasks.add("tv")
                    startDialogue(listOf(
                        "Ведущая" to "Главная новость дня: исследовательский институт подтвердил успешное открытие так называемого нейтрального пространства.",
                        "Ведущая" to "По словам учёных, область не принадлежит ни одному из известных физических регионов. Сейчас специалисты изучают её свойства и просят граждан не приближаться к лабораторному комплексу.",
                        "Лиза" to "Нейтральное пространство... Звучит так, будто кто-то очень хотел назвать опасную штуку безопасной.",
                        "Ведущая" to "Пресс-конференция института состоится сегодня вечером. Мы продолжим следить за развитием событий."
                    ))
                }
                else -> showToast("Пока здесь нечего делать.")
            }
            Phase.STREET -> when (nearest.id) {
                "home" -> if (streetTasks.size>=2) {
                    returnedHome=true; buildApartment()
                    startDialogue(listOf(
                        "Аня • сообщение" to "Ты пропала. Всё нормально?",
                        "Лиза" to "Да. Просто день какой-то длинный.",
                        "Аня" to "Тогда не засиживайся. Отдохни хоть раз."
                    ))
                } else showToast("Пройдись до остановки и загляни в магазин.")
                "busstop" -> {
                    streetTasks.add("busstop")
                    startDialogue(listOf("Лиза" to "Автобус опять задерживается. Удивительно. Мир меняется, а это — никогда."))
                }
                "shop" -> {
                    streetTasks.add("shop")
                    startDialogue(listOf(
                        "Продавец" to "Пакет нужен?",
                        "Лиза" to "Нет, спасибо. Я и так стараюсь не приносить домой лишнего.",
                        "Продавец" to "Тогда хорошего вечера."
                    ))
                }
                else -> showToast("Ничего интересного.")
            }
            else -> Unit
        }
        updateObjective()
    }

    private fun isWalkable(p:Vector3):Boolean {
        val limit = if (phase==Phase.STREET) 14f else 5.35f
        if (kotlin.math.abs(p.x)>limit || kotlin.math.abs(p.z)>limit*.82f) return false
        return props.none { distance(p,it.position) < .82f && it.id !in setOf("phone","message","exit","home") }
    }

    private fun buildHeroSpriteMesh(camera: Camera3D): Mesh3D? {
        val animation = heroSpriteAnimation ?: return null
        return DirectionalSpriteRenderer.buildMesh(
            position = heroPosition,
            facingDirection = heroFacing,
            camera = camera,
            animation = animation,
            elapsedSeconds = heroAnimationTime,
            worldWidth = .8f,
            worldHeight = 1.75f
        ).mesh
    }

    private fun drawHeroBillboard(context:RenderContext, camera: Camera3D = worldCamera) {
        val bottom = renderer.project(Vector3(heroPosition.x,0f,heroPosition.z),camera,context.width,context.height) ?: return
        val top = renderer.project(Vector3(heroPosition.x,1.75f,heroPosition.z),camera,context.width,context.height) ?: return
        // Screen Y grows downward, so the projected top is normally smaller than the bottom.
        // Use the absolute vertical span to keep the sprite destination rectangle positive.
        val width = kotlin.math.abs(bottom.second - top.second) * .46f
        val left=bottom.first-width*.5f
        val right=bottom.first+width*.5f
        val tex=heroTexture
        if(tex!=null) context.drawTexture(tex,left,top.second,right,bottom.second)
        else {
            context.drawRoundRect(left,top.second,right,bottom.second,8f,0xFF252331.toInt())
            context.drawRect(left+width*.3f,top.second+8f,right-width*.3f,top.second+25f,0xFFE0C9E9.toInt())
        }
    }

    private fun renderIntro(context:RenderContext) {
        context.drawRect(0f,0f,context.width.toFloat(),context.height.toFloat(),0xFF05050B.toInt())
        val centerX=context.width*.5f; val centerY=context.height*.48f
        val strength=(introTime/2.6f).coerceIn(0f,1f)
        val half=2f+strength*42f
        context.drawLine(centerX-2f,centerY-half,centerX+4f,centerY+half,0xFF7B102B.toInt(),9f)
        context.drawLine(centerX,centerY-half,centerX+3f,centerY+half,0xFFFF294F.toInt(),3f)
        for(i in 0 until 48) {
            val side=if(i%2==0)-1f else 1f
            val distance=(1f-((introTime*.65f+i*.063f)%1f))*250f
            val x=centerX+side*distance
            val y=centerY+kotlin.math.sin(i*2.7f+introTime*4f)*distance*.12f
            context.drawRect(x,y,x+4f,y+2f,if(i%3==0)0xFFFF355C.toInt() else 0xFFB71E4D.toInt())
        }
        // Teia reaches through the tear; the image stays unexplained.
        if (introTime > 1.1f && introTime < 4.35f) {
            val reach = ((introTime - 1.1f) / 1.2f).coerceIn(0f,1f)
            val hx = centerX + 8f + reach * 62f
            val hy = centerY + 12f - reach * 20f
            val handColor = 0xFFE5DCE8.toInt()
            context.drawLine(centerX+2f,centerY+3f,hx,hy,handColor,13f)
            context.drawLine(hx,hy,hx+42f,hy-18f,handColor,11f)
            context.drawLine(hx+30f,hy-12f,hx+52f,hy-35f,handColor,7f)
            context.drawLine(hx+34f,hy-8f,hx+62f,hy-24f,handColor,7f)
            context.drawLine(hx+37f,hy-2f,hx+64f,hy-10f,handColor,7f)
            context.drawLine(hx+35f,hy+5f,hx+58f,hy+6f,handColor,7f)
            context.drawLine(hx,hy+12f,hx+32f,hy+18f,handColor,10f)
        }
        if (introTime > 4.35f) {
            context.drawRect(0f,0f,context.width.toFloat(),context.height.toFloat(),0xFF000000.toInt())
            return
        }
        context.drawText("ТРЕЩИНА",centerX-60f,centerY+130f,0xFFEDE8F5.toInt(),25f,true)
        context.drawText("Нажми или E, чтобы начать",centerX-135f,context.height-36f,0xFFBBAEC9.toInt(),15f)
    }

    private fun buildFantasyBaseMeshes(): List<Mesh3D> {
        val ruin = mutableListOf<Mesh3D>()
        val groundTexture = mossyRockTexture
        val wallTexture = stoneTexture
        ruin += if (groundTexture != null) {
            texturedGroundPlaneMesh(Vector3(0f,0f,0f),18f,16f,groundTexture,12f,10f)
        } else {
            cuboidMesh(Vector3(0f,-.08f,0f),Vector3(18f,.16f,16f),0xFF45434A.toInt())
        }
        fun ruinWall(center: Vector3, size: Vector3) {
            ruin += if (wallTexture != null) {
                texturedCuboidMesh(center,size,CuboidMaterialSet(
                    wallTexture,wallTexture,wallTexture,wallTexture,wallTexture,wallTexture
                ), 3f, 2f)
            } else {
                cuboidMesh(center,size,0xFF514B50.toInt(),0xFF403B42.toInt(),0xFF625B61.toInt())
            }
        }
        ruinWall(Vector3(-4f,1.5f,-4f),Vector3(.35f,3f,8f))
        ruinWall(Vector3(4f,1.5f,-4f),Vector3(.35f,3f,8f))
        ruinWall(Vector3(-2.2f,1.8f,-3.2f),Vector3(.28f,3.6f,.28f))
        ruinWall(Vector3(2.8f,1.1f,-4.8f),Vector3(.3f,2.2f,.3f))
        ruinWall(Vector3(-4f,2.9f,-4f),Vector3(2.2f,.24f,1.4f))
        ruinWall(Vector3(3.8f,2.7f,-4.2f),Vector3(1.8f,.22f,1.2f))
        ruin += cuboidMesh(Vector3(0f,.18f,0f),Vector3(2.8f,.36f,2.2f),0xFF66606B.toInt())
        // Broken paving and fallen masonry frame the camp without blocking its playable space.
        ruin += cuboidMesh(Vector3(-3.1f,.12f,1.8f),Vector3(1.5f,.24f,.62f),0xFF5A555D.toInt())
        ruin += cuboidMesh(Vector3(3.3f,.16f,2.1f),Vector3(1.1f,.32f,.72f),0xFF625B62.toInt())
        ruin += cuboidMesh(Vector3(-1.6f,.12f,.3f),Vector3(1.1f,.18f,.18f),0xFF65462F.toInt())
        ruin += cuboidMesh(Vector3(-1.6f,.18f,.3f),Vector3(.18f,.3f,1.1f),0xFF65462F.toInt())
        ruin += cuboidMesh(Vector3(-1.6f,.55f,.3f),Vector3(.36f,.75f,.36f),0xFFE66C35.toInt())
        ruin += cuboidMesh(Vector3(-1.6f,.95f,.3f),Vector3(.22f,.5f,.22f),0xFFFFC05A.toInt())
        return ruin
    }

    private fun renderFantasyWake(context:RenderContext) {
        context.clear(0xFF171824.toInt())
        worldCamera.aspectRatio=context.width.toFloat()/context.height.coerceAtLeast(1)
        worldCamera.position=Vector3(heroPosition.x+.2f,5f,heroPosition.z+8f)
        worldCamera.target=Vector3(heroPosition.x,1f,heroPosition.z-.8f)
        val wakeCamera=worldCamera
        val ruin = fantasyBaseMeshes.toMutableList()
        if (phase == Phase.FANTASY_CAMP && combatStarted && enemyHealth > 0) {
            ruin += cuboidMesh(Vector3(2.4f,.72f,-1.5f),Vector3(.9f,1.44f,.8f),0xFF292633.toInt(),0xFF7D263B.toInt(),0xFF17151D.toInt())
            ruin += cuboidMesh(Vector3(2.4f,1.55f,-1.5f),Vector3(.52f,.32f,.48f),0xFF9E3447.toInt())
        }
        if (phase == Phase.FANTASY_CAMP && glitchTriggered && enemyHealth <= 0) {
            ruin += cuboidMesh(Vector3(-2.8f,1.35f,-2.8f),Vector3(.07f,2.7f,.12f),0xFFFF244F.toInt(),0xFF8C1536.toInt(),0xFFFF6B86.toInt())
            ruin += cuboidMesh(Vector3(-2.86f,1.35f,-2.8f),Vector3(.035f,2.45f,.14f),0xFFFFA0B0.toInt())
        }
        val showHero = phase == Phase.FANTASY_CAMP || phase == Phase.COMPLETE
        val heroMesh = if (showHero) buildHeroSpriteMesh(wakeCamera) else null
        if (heroMesh != null) ruin += heroMesh
        renderer.render(context,wakeCamera,ruin)
        if (showHero && heroMesh == null) drawHeroBillboard(context,wakeCamera)
        context.drawText(when (phase) {
            Phase.FANTASY_CAMP -> "РУИНЫ • КОСТЁР"
            Phase.COMPLETE -> "РАЗЛОМ • ПЕРВЫЙ КОНТАКТ"
            else -> "..."
        },context.width*.5f-115f,context.height-28f,0xFFFFFFFF.toInt(),15f,true)
        if (enemyTelegraph > 0f && phase == Phase.FANTASY_CAMP && enemyHealth > 0) {
            val marker = renderer.project(Vector3(enemyWindupTarget.x,.05f,enemyWindupTarget.z),wakeCamera,context.width,context.height)
            if (marker != null) {
                val radius = (1f - (enemyTelegraph / .78f).coerceIn(0f,1f)) * 18f + 22f
                var px = marker.first + radius
                var py = marker.second
                for (i in 1..32) {
                    val angle = i * (Math.PI * 2.0 / 32.0)
                    val nx = marker.first + kotlin.math.cos(angle).toFloat() * radius
                    val ny = marker.second + kotlin.math.sin(angle).toFloat() * radius
                    context.drawLine(px,py,nx,ny,0xFFFF425A.toInt(),3f)
                    px=nx; py=ny
                }
                context.drawText("УДАР!",marker.first-24f,marker.second-radius-10f,0xFFFF9AA8.toInt(),13f,true)
            }
        }
        if (damageFlash > 0f) {
            context.drawRect(0f,0f,context.width.toFloat(),context.height.toFloat(),0x44FF243F.toInt())
        }
        if (glitchTimer > 0f) {
            context.drawRect(0f,0f,context.width.toFloat(),context.height.toFloat(),0x55FF174F.toInt())
            context.drawText("СИГНАЛ НЕ СОВПАДАЕТ",context.width*.5f-145f,context.height*.35f,0xFFFF526B.toInt(),20f,true)
        }
    }

    private fun updateObjective() {
        when(phase) {
            Phase.APARTMENT -> { objective.text="ЗЕМЛЯ • ДОМ ЛИЗЫ"; hint.text="Утро: телефон, завтрак и новости (" + apartmentTasks.size + "/3)." }
            Phase.STREET -> { objective.text="ЗЕМЛЯ • ГОРОД"; hint.text="Остановка и магазин (" + streetTasks.size + "/2), затем возвращайся домой." }
            Phase.FANTASY_CAMP -> {
                objective.text="НЕИЗВЕСТНЫЙ МИР • РУИНЫ"
                combatStatus.text = "СОСТОЯНИЕ: $heroHealth / 3"
                hint.text=when {
                    !combatStarted -> "Осмотрись и отойди от костра. WASD или виртуальный стик."
                    enemyHealth>0 -> "SPACE / HIT — атака • SHIFT / DODGE — уклонение"
                    else -> "Первый глитч. Нажми E, чтобы осмотреть разлом."
                }
            }
            else -> Unit
        }
    }


    private fun startDialogue(lines: List<Pair<String, String>>, after: (() -> Unit)? = null) {
        dialogueLines = lines
        dialogueIndex = 0
        dialogueAfter = after
        dialogueOpen = lines.isNotEmpty()
        if (dialogueOpen) {
            dialogueName.text = lines[0].first
            dialogueText.text = lines[0].second
            dialogueName.visible = true
            dialogueText.visible = true
            joystick.visible = false
            useButton.visible = true
            attackButton.visible = false
            dodgeButton.visible = false
        }
    }

    private fun advanceDialogue() {
        if (!dialogueOpen) return
        dialogueIndex++
        if (dialogueIndex >= dialogueLines.size) {
            dialogueOpen = false
            dialogueName.visible = false
            dialogueText.visible = false
            val callback = dialogueAfter
            dialogueAfter = null
            callback?.invoke()
            if (phase == Phase.FANTASY_CAMP) {
                joystick.visible = true; useButton.visible = true
                attackButton.visible = true; dodgeButton.visible = true
            } else if (phase == Phase.APARTMENT || phase == Phase.STREET) {
                joystick.visible = true; useButton.visible = true
            }
            return
        }
        dialogueName.text = dialogueLines[dialogueIndex].first
        dialogueText.text = dialogueLines[dialogueIndex].second
    }

    private fun updateFantasyCombat(delta: Float) {
        if (toastTimer > 0f) {
            toastTimer = (toastTimer - delta).coerceAtLeast(0f)
            toast.visible = toastTimer > 0f
        }
        if (fade > 0f) fade = (fade - delta * 1.4f).coerceAtLeast(0f)
        if (glitchTimer > 0f) glitchTimer = (glitchTimer - delta).coerceAtLeast(0f)
        attackCooldown = (attackCooldown - delta).coerceAtLeast(0f)
        dodgeTimer = (dodgeTimer - delta).coerceAtLeast(0f)
        heroInvulnerability = (heroInvulnerability - delta).coerceAtLeast(0f)
        damageFlash = (damageFlash - delta).coerceAtLeast(0f)
        val ix = ((if (input.isDown("right")) 1f else 0f) -
            (if (input.isDown("left")) 1f else 0f) + joystick.axisX).coerceIn(-1f,1f)
        val iz = ((if (input.isDown("back")) 1f else 0f) -
            (if (input.isDown("forward")) 1f else 0f) + joystick.axisY).coerceIn(-1f,1f)
        val length = sqrt(ix*ix+iz*iz)
        val nx = if (length>1f) ix/length else ix
        val nz = if (length>1f) iz/length else iz
        if (length > .001f) heroFacing = Vector3(nx,0f,nz)
        val speed = if (dodgeTimer>0f) 7.5f else 3.1f
        val candidate=Vector3(heroPosition.x+nx*speed*delta,0f,heroPosition.z+nz*speed*delta)
        if (kotlin.math.abs(candidate.x)<6.2f && kotlin.math.abs(candidate.z)<5.2f) heroPosition=candidate
        val doDodge=input.wasPressed("dodge") || touchDodge
        val doAttack=input.wasPressed("attack") || touchAttack || (input.wasPressed("use") && enemyHealth>0)
        val doUse=input.wasPressed("use") || showUse
        touchDodge=false; touchAttack=false; showUse=false
        if (doDodge && dodgeTimer<=0f) dodgeTimer=.38f
        val movedX=heroPosition.x-fantasyStart.x
        val movedZ=heroPosition.z-fantasyStart.z
        if (!combatStarted && sqrt(movedX*movedX+movedZ*movedZ)>1.25f) {
            combatStarted=true
            startDialogue(listOf(
                "Лиза" to "Под ногами хрустит камень. Значит, я действительно здесь...",
                "Лиза" to "Стой. Кто-то шевельнулся за колонной."
            ))
            updateObjective()
            return
        }
        if (!combatStarted) { updateObjective(); return }
        val enemyPos=Vector3(2.4f,0f,-1.5f)
        val dx=enemyPos.x-heroPosition.x; val dz=enemyPos.z-heroPosition.z
        val distanceToEnemy=sqrt(dx*dx+dz*dz)
        if (enemyHealth>0) {
            if (doAttack && attackCooldown<=0f) {
                attackCooldown=.52f
                if (distanceToEnemy >= 2.5f) {
                    showToast("Слишком далеко. Подойди к существу.")
                } else {
                    enemyHealth--
                if (enemyHealth<=0) enemyTelegraph=0f
                if (enemyHealth<=0 && !glitchTriggered) {
                    glitchTriggered=true; glitchTimer=1.2f
                    startDialogue(listOf(
                        "Лиза" to "Что это было?.. Оно словно рассыпалось ещё до того, как я коснулась его.",
                        "Лиза" to "И эта вспышка... На секунду всё вокруг стало неправильным."
                    ))
                }
                }
            }
            enemyAttackTimer-=delta
            if (enemyHealth>0 && enemyAttackTimer<=0f && distanceToEnemy<3.6f && enemyTelegraph<=0f) {
                enemyWindupTarget=heroPosition
                enemyTelegraph=.78f
            }
            if (enemyHealth>0 && enemyTelegraph>0f) {
                enemyTelegraph-=delta
                if (enemyTelegraph<=0f) {
                    val caught=distance(heroPosition,enemyWindupTarget)<=1.25f
                    if (caught && dodgeTimer<=0f && heroInvulnerability<=0f) {
                        heroHealth=(heroHealth-1).coerceAtLeast(0)
                        heroInvulnerability=.9f
                        damageFlash=.18f
                        if (heroHealth<=0) {
                            startDialogue(listOf(
                                "Лиза" to "Ноги подкосились. Я не могу здесь умереть... Что бы это ни было, нужно подняться.",
                                "Лиза" to "В следующий раз не стану ждать, пока оно замахнётся."
                            )) {
                                heroHealth=3; heroPosition=Vector3(0f,0f,1.8f)
                                fantasyStart=heroPosition; combatStarted=false
                                enemyHealth=3; enemyAttackTimer=2.2f; enemyTelegraph=0f
                                glitchTriggered=false; glitchTimer=0f
                            }
                        } else {
                            showToast("Получен удар. Уклоняйся во время красного предупреждения.")
                        }
                    }
                    enemyAttackTimer=2.6f
                }
            }
        } else if (doUse && glitchTriggered && !dialogueOpen) {
            val riftDistance = distance(heroPosition, Vector3(-2.8f,0f,-2.8f))
            if (riftDistance > 3.2f) {
                showToast("Красная трещина мерцает у разрушенной колонны. Подойди ближе.")
                return
            }
            startDialogue(listOf(
                "Лиза" to "Трещина в воздухе. Нет... не в воздухе. Будто сам мир на секунду забыл, каким должен быть.",
                "Лиза" to "Я просто хотела проснуться. Почему это происходит со мной?"
            )) {
                phase=Phase.COMPLETE; fade=1f
                joystick.visible=false; useButton.visible=false; attackButton.visible=false; dodgeButton.visible=false
                objective.visible=false; combatStatus.visible=false
                hint.text="ГЛАВА I • НУЛЕВАЯ ГРАНЬ"; hint.visible=true
                endLabel.text="ПРОЛОГ ЗАВЕРШЁН"
                endLabel.visible=true
            }
        }
        updateObjective()
    }

    private fun showToast(message:String) {
        toast.text=message; toast.visible=true; toastTimer=1.8f
    }

    private fun distance(a:Vector3,b:Vector3):Float {
        val dx=a.x-b.x; val dz=a.z-b.z
        return sqrt(dx*dx+dz*dz)
    }

    private fun shade(color:Int,factor:Float):Int {
        val a=(color ushr 24) and 255
        val r=(((color ushr 16) and 255)*factor).toInt().coerceIn(0,255)
        val g=(((color ushr 8) and 255)*factor).toInt().coerceIn(0,255)
        val b=((color and 255)*factor).toInt().coerceIn(0,255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
