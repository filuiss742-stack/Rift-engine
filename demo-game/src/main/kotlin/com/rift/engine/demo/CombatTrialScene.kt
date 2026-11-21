package com.rift.engine.demo

import com.rift.engine.animation.*
import com.rift.engine.combat.HealthComponent
import com.rift.engine.entity.GameObject
import com.rift.engine.graphics.*
import com.rift.engine.input.EngineKey
import com.rift.engine.movement.*
import com.rift.engine.narrative.*
import com.rift.engine.physics.Aabb
import com.rift.engine.physics.PhysicsBodyType
import com.rift.engine.scene.Scene
import com.rift.engine.ui.*
import kotlin.math.sqrt

/** 2.5D trial built from public Rift APIs. It is deliberately not a true 3D renderer. */
class CombatTrialScene : Scene() {
    internal val player = GameObject(220f, 290f, 48f, 54f)
    internal val enemy = GameObject(520f, 290f, 62f, 62f)
    internal val npc = GameObject(155f, 205f, 48f, 54f)
    internal val playerHealth = HealthComponent(100f)
    internal val enemyHealth = HealthComponent(60f, onDeath = ::onEnemyDefeated)
    internal var enemyDefeated = false
        private set
    internal val isEnemyTelegraphing get() = enemyState == EnemyState.TELEGRAPH

    private enum class EnemyState { IDLE, TELEGRAPH, RECOVERY, DEFEATED }
    private var enemyState = EnemyState.IDLE
    private var enemyAttackCooldown = 1.8f
    private var telegraphRemaining = 0f
    private var recoveryRemaining = 0f
    private var deathRemaining = 0.55f
    private var dodgeRemaining = 0f
    private var dodgeCooldown = 0f
    private var attackCooldown = 0f
    private var attackAnimRemaining = 0f
    private var abilityCooldown = 0f
    private var dirX = 1f
    private var dirY = 0f
    private var touchAttack = false
    private var touchDodge = false
    private var touchSkill = false
    private var dialogueOpen = false
    private val walk = TopDownMovementProfile(178f)
    private val dodge = TopDownMovementProfile(470f)
    private var playerAnim: SpriteAnimationComponent? = null
    private var enemyAnim: SpriteAnimationComponent? = null

    private val stick = VirtualJoystick(22f, 458f, 112f)
    private val dodgeBtn = VirtualButton(682f, 468f, 82f, 82f, "DODGE", onPressed = { touchDodge = true })
    private val attackBtn = VirtualButton(774f, 468f, 82f, 82f, "ATTACK", onPressed = { touchAttack = true })
    private val skillBtn = VirtualButton(866f, 468f, 82f, 82f, "SKILL", onPressed = { touchSkill = true })
    private val hud = UiLabel(18f, 16f, 900f, 26f, "RIFT TRIAL • HP 100 • GOLEM 60", 0xFFFFFFFF.toInt(), 18f, true)
    private val hints = UiLabel(18f, 44f, 900f, 22f, "WASD / joystick • SPACE attack • SHIFT dodge • Q skill • E interact", 0xFFDCE6E4.toInt(), 13f)
    private val endingLabel = UiLabel(18f, 72f, 900f, 24f, "", 0xFFFFD6A5.toInt(), 16f, true)

    private val story = StoryState()
    private val dialogue = DialogueSession(
        DialogueGraph(listOf(
            DialogueNode("meet", "Mira", "Ты слышал шум у портала? Я боюсь идти туда одна. Поможешь?", choices = listOf(
                DialogueChoice("help", "Помочь Мире", "trusted", effects = listOf(AddStoryValue("relationship", 2), SetStoryFlag("mira_helped"))),
                DialogueChoice("dismiss", "Отказаться помогать", "distant", effects = listOf(AddStoryValue("relationship", -2), SetStoryFlag("mira_dismissed")))
            )),
            DialogueNode("trusted", "Mira", "Спасибо. Я этого не забуду. Если понадобится помощь — я буду рядом."),
            DialogueNode("distant", "Mira", "Понятно. Тогда я справлюсь сама.")
        )), story
    )
    private val panel = UiPanel(70f, 330f, 820f, 220f, 0xEE101923.toInt(), 12f)
    private val line = UiLabel(18f, 14f, 780f, 54f, "", 0xFFFFFFFF.toInt(), 17f, true)
    private val help = UiButton(18f, 92f, 360f, 52f, "Помочь Мире", onClick = { chooseDialogue("help") }, backgroundColor = 0xFF245D55.toInt())
    private val dismiss = UiButton(400f, 92f, 360f, 52f, "Отказаться", onClick = { chooseDialogue("dismiss") }, backgroundColor = 0xFF65414B.toInt())
    private val close = UiButton(620f, 158f, 160f, 42f, "Продолжить", onClick = { closeDialogue() })
    private val relation = UiLabel(18f, 158f, 520f, 30f, "Отношения: 0", 0xFFFFD6A5.toInt(), 15f, true)
    private val rig = CameraRig(camera, 960f, 600f, smoothing = 7f, deadZoneWidth = 72f, deadZoneHeight = 48f, worldBounds = Aabb(0f, 0f, 1400f, 900f))

    init {
        input.bindKey("left", EngineKey.A); input.bindKey("left", EngineKey.LEFT)
        input.bindKey("right", EngineKey.D); input.bindKey("right", EngineKey.RIGHT)
        input.bindKey("up", EngineKey.W); input.bindKey("up", EngineKey.UP)
        input.bindKey("down", EngineKey.S); input.bindKey("down", EngineKey.DOWN)
        input.bindKey("attack", EngineKey.SPACE); input.bindKey("dodge", EngineKey.SHIFT)
        input.bindKey("skill", EngineKey.Q); input.bindKey("interact", EngineKey.E)
        player.movementProfile = walk
        player.addComponent(playerHealth); enemy.addComponent(enemyHealth)
        add(player); add(enemy); add(npc)
        combat.addHurtbox(enemy, teamId = "enemy")
        ui.add(hud); ui.add(hints); ui.add(endingLabel); ui.add(stick); ui.add(dodgeBtn); ui.add(attackBtn); ui.add(skillBtn)
        story.setValue("relationship", 0); dialogue.start("meet")
        panel.add(line); panel.add(help); panel.add(dismiss); panel.add(relation); panel.add(close)
        panel.visible = false; close.visible = false; ui.add(panel)
        rig.snapTo(player)
    }

    override fun onEnter() {
        loadSprites()
        playerHealth.reset(gameState.int("player.hp", 100).coerceIn(1, 100).toFloat())
        story.setValue("relationship", gameState.int("story.relationship"))
        endingLabel.text = gameState.string("story.ending")
        endingLabel.visible = endingLabel.text.isNotBlank()
    }

    override fun update(deltaSeconds: Float) {
        val hx = ((if (input.isDown("right")) 1f else 0f) - (if (input.isDown("left")) 1f else 0f) + stick.axisX).coerceIn(-1f, 1f)
        val hy = ((if (input.isDown("down")) 1f else 0f) - (if (input.isDown("up")) 1f else 0f) + stick.axisY).coerceIn(-1f, 1f)
        val len = sqrt(hx * hx + hy * hy)
        val mx = hx / if (len > 1f) len else 1f
        val my = hy / if (len > 1f) len else 1f
        if (mx != 0f || my != 0f) { dirX = mx; dirY = my }

        attackCooldown = (attackCooldown - deltaSeconds).coerceAtLeast(0f)
        attackAnimRemaining = (attackAnimRemaining - deltaSeconds).coerceAtLeast(0f)
        dodgeCooldown = (dodgeCooldown - deltaSeconds).coerceAtLeast(0f)
        abilityCooldown = (abilityCooldown - deltaSeconds).coerceAtLeast(0f)
        val doAttack = input.wasPressed("attack") || touchAttack
        val doDodge = input.wasPressed("dodge") || touchDodge
        val doSkill = input.wasPressed("skill") || touchSkill
        val interact = input.wasPressed("interact")
        touchAttack = false; touchDodge = false; touchSkill = false

        if (!dialogueOpen) {
            if (doDodge && dodgeCooldown <= 0f) { dodgeRemaining = 0.24f; dodgeCooldown = 0.82f }
            val evading = dodgeRemaining > 0f
            player.movementProfile = if (evading) dodge else walk
            player.movementInput = if (evading) MovementInput(dirX, dirY) else MovementInput(mx, my)
            dodgeRemaining = (dodgeRemaining - deltaSeconds).coerceAtLeast(0f)
            if (evading) playerAnim?.play("dodge", false)
            else if (attackAnimRemaining > 0f) playerAnim?.play("attack", false)
            else if (mx != 0f || my != 0f) playerAnim?.play("walk", false)
            else playerAnim?.play("idle", false)
            if (doAttack && attackCooldown <= 0f) attack()
            if (doSkill && abilityCooldown <= 0f) skill()
            if (interact && distance(player, npc) < 118f) openDialogue()
            else if (interact && distance(player, 870f, 280f) < 105f) {
                gameState.putInt("player.hp", playerHealth.currentHealth.toInt())
                requestSceneChange(DungeonTrialScene())
            }
        } else player.movementInput = MovementInput()

        updateEnemy(deltaSeconds)
        super.update(deltaSeconds)
        player.renderLayer = player.y.toInt(); enemy.renderLayer = enemy.y.toInt(); npc.renderLayer = npc.y.toInt()
        rig.update(player, deltaSeconds)
        hud.text = "HP " + playerHealth.currentHealth.toInt() + "   GOLEM " + enemyHealth.currentHealth.toInt() +
            "   MIRA " + story.value("relationship") + if (enemyDefeated) "   •   GATE OPEN" else ""
        relation.text = "Отношения с Мирой: " + story.value("relationship")
    }

    override fun render(context: RenderContext) {
        context.clear(0xFF101B20.toInt())
        camera.withTransform(context) {
            context.drawRect(0f, 0f, 1400f, 900f, 0xFF20352F.toInt())
            for (x in 0..1400 step 72) for (y in 0..900 step 72) {
                context.drawRect(x.toFloat(), y.toFloat(), x + 70f, y + 70f,
                    if ((x / 72 + y / 72) % 2 == 0) 0xFF294238.toInt() else 0xFF263D35.toInt())
            }
            context.drawRoundRect(70f, 350f, 980f, 415f, 24f, 0xFF665744.toInt())
            context.drawRoundRect(820f, 220f, 930f, 360f, 22f, 0xFF493B68.toInt())
            context.drawRoundRect(835f, 236f, 915f, 345f, 18f, 0xFF8B65C7.toInt())
            for (i in 0 until 5) {
                val tx = 300f + i * 118f
                context.drawRoundRect(tx - 18f, 74f, tx + 48f, 124f, 20f, 0xFF35634B.toInt())
            }
            if (isEnemyTelegraphing) {
                ring(context, enemy.x + 31f, enemy.y + 31f, 92f, 0xFFFF5268.toInt(), 3f)
                ring(context, enemy.x + 31f, enemy.y + 31f, 66f, 0xFFFFA1A9.toInt(), 1.5f)
                context.drawText("DODGE!", enemy.x - 8f, enemy.y - 16f, 0xFFFFDCE0.toInt(), 16f, true)
            }
        }
        super.render(context)
    }

    internal fun forceEnemyTelegraph(time: Float = 0.05f) {
        enemyState = EnemyState.TELEGRAPH; telegraphRemaining = time; enemyAnim?.play("telegraph")
    }

    internal fun chooseDialogue(id: String): Boolean {
        if (!dialogue.choose(id)) return false
        gameState.putInt("story.relationship", story.value("relationship"))
        gameState.putBoolean("story.mira_helped", story.hasFlag("mira_helped"))
        gameState.putBoolean("story.mira_dismissed", story.hasFlag("mira_dismissed"))
        line.text = dialogue.currentNode?.text ?: "..."
        help.visible = false; dismiss.visible = false; close.visible = true
        return true
    }

    internal fun endingForRelationship(value: Int = story.value("relationship")): String = when {
        value >= 2 -> "GOOD ENDING — TRUST"
        value <= -1 -> "BITTER ENDING — DISTANCE"
        else -> "NEUTRAL ENDING — UNCERTAIN"
    }

    private fun openDialogue() {
        dialogueOpen = true; dialogue.start("meet"); line.text = dialogue.currentNode?.text ?: ""
        help.visible = true; dismiss.visible = true; close.visible = false; panel.visible = true
        stick.visible = false; dodgeBtn.visible = false; attackBtn.visible = false; skillBtn.visible = false
    }

    private fun closeDialogue() {
        dialogueOpen = false; panel.visible = false
        stick.visible = true; dodgeBtn.visible = true; attackBtn.visible = true; skillBtn.visible = true
    }

    private fun attack() {
        if (enemyDefeated) return
        if (distance(player, enemy) <= 58f) {
            combat.addHitbox(player, 12f, "player", setOf("enemy"), 0.12f)
            playerAnim?.play("attack"); attackAnimRemaining = 0.32f; attackCooldown = 0.38f
        } else attackCooldown = 0.12f
    }

    private fun skill() {
        if (!enemyDefeated && distance(player, enemy) <= 132f) {
            enemyHealth.applyDamage(24f); enemyAnim?.play("hit"); playerAnim?.play("attack"); attackAnimRemaining = 0.32f; rig.shake(4f, 0.12f)
        }
        abilityCooldown = 4.8f
    }

    private fun updateEnemy(dt: Float) {
        if (enemyDefeated) { deathRemaining -= dt; if (deathRemaining <= 0f) remove(enemy); return }
        val dx = player.x - enemy.x; val dy = player.y - enemy.y
        val dist = sqrt(dx * dx + dy * dy)
        when (enemyState) {
            EnemyState.IDLE -> {
                enemyAttackCooldown -= dt; enemy.movementProfile = TopDownMovementProfile(66f)
                enemy.movementInput = if (dist > 62f) MovementInput(dx / dist, dy / dist) else MovementInput()
                if (enemyAttackCooldown <= 0f) {
                    enemyState = EnemyState.TELEGRAPH; telegraphRemaining = 0.92f
                    enemy.movementInput = MovementInput(); enemyAnim?.play("telegraph")
                }
            }
            EnemyState.TELEGRAPH -> {
                enemy.movementInput = MovementInput(); telegraphRemaining -= dt
                if (telegraphRemaining <= 0f) {
                    enemyState = EnemyState.RECOVERY; recoveryRemaining = 0.48f
                    enemyAttackCooldown = 2.15f; enemyAnim?.play("attack")
                    if (dist <= 92f && dodgeRemaining <= 0f) playerHealth.applyDamage(25f)
                }
            }
            EnemyState.RECOVERY -> {
                recoveryRemaining -= dt
                if (recoveryRemaining <= 0f) { enemyState = EnemyState.IDLE; enemyAnim?.play("idle") }
            }
            EnemyState.DEFEATED -> Unit
        }
    }

    private fun onEnemyDefeated() {
        enemyDefeated = true; enemyState = EnemyState.DEFEATED
        enemyAnim?.play("hit"); enemy.movementInput = MovementInput()
        gameState.putBoolean("trial.golem_defeated", true); rig.shake(7f, 0.22f)
    }

    private fun loadSprites() {
        val texture = assets?.loadTexture("rift.trial.sheet", "sprites/rift_trial_sheet.png") ?: return
        val sheet = SpriteSheet(texture, 64, 64)
        player.renderDefaultVisual = false; enemy.renderDefaultVisual = false; npc.renderDefaultVisual = false
        playerAnim = anim(sheet, mapOf("idle" to listOf(0,1), "walk" to listOf(0,1,2,1), "attack" to listOf(2,3,2), "dodge" to listOf(1,3)), "idle")
        enemyAnim = anim(sheet, mapOf("idle" to listOf(4,5), "telegraph" to listOf(5,6), "attack" to listOf(6,7), "hit" to listOf(7)), "idle")
        player.addComponent(playerAnim!!); enemy.addComponent(enemyAnim!!)
        npc.addComponent(anim(sheet, mapOf("idle" to listOf(0,1)), "idle"))
    }

    private enum class EnemyState { IDLE, TELEGRAPH, RECOVERY, DEFEATED }
}

/** Side-view mini-zone with physics jump, blockers, platforms, a zoomed camera and route-dependent return. */
class DungeonTrialScene : Scene() {
    internal val player = GameObject(72f, 452f, 44f, 54f)
    private var touchJump = false
    private var animator: SpriteAnimationComponent? = null
    private val stick = VirtualJoystick(22f, 458f, 112f)
    private val jump = VirtualButton(820f, 462f, 116f, 88f, "JUMP", onPressed = { touchJump = true })
    private val status = UiLabel(18f, 16f, 900f, 26f, "DUNGEON TRIAL • JUMP THE BLOCKERS", 0xFFFFFFFF.toInt(), 18f, true)
    private val hint = UiLabel(18f, 44f, 900f, 22f, "A/D or joystick • SPACE / JUMP • E near exit: return", 0xFFDCE6E4.toInt(), 13f)

    init {
        input.bindKey("left", EngineKey.A); input.bindKey("left", EngineKey.LEFT)
        input.bindKey("right", EngineKey.D); input.bindKey("right", EngineKey.RIGHT)
        input.bindKey("jump", EngineKey.SPACE); input.bindKey("jump", EngineKey.W); input.bindKey("jump", EngineKey.UP)
        input.bindKey("interact", EngineKey.E)
        player.movementProfile = PlatformerMovementProfile(215f, 0f, 470f, 920f)
        physics.gravityX = 0f; physics.gravityY = 1020f
        physics.addBody(player, PhysicsBodyType.DYNAMIC)
        val ground = GameObject(0f, 520f, 1180f, 100f); physics.addBody(ground, PhysicsBodyType.STATIC); add(ground)
        val b1 = GameObject(230f, 468f, 68f, 52f); physics.addBody(b1, PhysicsBodyType.STATIC); add(b1)
        val b2 = GameObject(405f, 438f, 82f, 82f); physics.addBody(b2, PhysicsBodyType.STATIC); add(b2)
        val platform = GameObject(590f, 410f, 150f, 18f); physics.addBody(platform, PhysicsBodyType.STATIC); add(platform)
        val b3 = GameObject(820f, 468f, 72f, 52f); physics.addBody(b3, PhysicsBodyType.STATIC); add(b3)
        add(player); ui.add(status); ui.add(hint); ui.add(stick); ui.add(jump)
        camera.zoom = 1.16f
    }

    override fun onEnter() {
        val texture = assets?.loadTexture("rift.trial.sheet", "sprites/rift_trial_sheet.png") ?: return
        val sheet = SpriteSheet(texture, 64, 64); player.renderDefaultVisual = false
        animator = anim(sheet, mapOf("idle" to listOf(0,1), "walk" to listOf(0,1,2,1), "jump" to listOf(2,3)), "idle")
        player.addComponent(animator!!)
    }

    override fun update(deltaSeconds: Float) {
        val horizontal = ((if (input.isDown("right")) 1f else 0f) - (if (input.isDown("left")) 1f else 0f) + stick.axisX).coerceIn(-1f,1f)
        val jumping = input.wasPressed("jump") || touchJump; touchJump = false
        player.movementInput = MovementInput(horizontal = horizontal, jumpPressed = jumping)
        if (!player.isGrounded) animator?.play("jump", false)
        else if (horizontal != 0f) animator?.play("walk", false) else animator?.play("idle", false)
        super.update(deltaSeconds)
        player.renderLayer = player.y.toInt()
        camera.x = (player.x - 360f).coerceAtLeast(0f); camera.y = 0f
        if (input.wasPressed("interact") && player.x >= 928f) {
            gameState.putBoolean("dungeon.cleared", true)
            val relation = gameState.int("story.relationship")
            gameState.putString("story.ending", when {
                relation >= 2 -> "GOOD ENDING — TRUST"
                relation <= -1 -> "BITTER ENDING — DISTANCE"
                else -> "NEUTRAL ENDING — UNCERTAIN"
            })
            requestSceneChange(CombatTrialScene())
        }
        status.text = if (gameState.boolean("dungeon.cleared")) "DUNGEON CLEARED" else "DUNGEON TRIAL • CAMERA ZOOM 1.16x"
    }

    override fun render(context: RenderContext) {
        context.clear(0xFF151C2B.toInt())
        camera.withTransform(context) {
            context.drawRect(0f, 0f, 1180f, 620f, 0xFF202B3A.toInt())
            for (x in 0..1180 step 64) context.drawLine(x.toFloat(), 0f, x.toFloat(), 620f, 0x263F5268)
            context.drawRect(0f, 520f, 1180f, 620f, 0xFF394C43.toInt())
            context.drawRoundRect(1000f, 430f, 1084f, 520f, 12f, 0xFF7756B8.toInt())
            context.drawRoundRect(1012f, 442f, 1072f, 520f, 10f, 0xFFB28BEA.toInt())
            context.drawText("EXIT", 1018f, 420f, 0xFFFFFFFF.toInt(), 14f, true)
        }
        super.render(context)
    }
}

private fun anim(sheet: SpriteSheet, sets: Map<String, List<Int>>, initial: String): SpriteAnimationComponent {
    val animations = sets.mapValues { (_, ids) ->
        SpriteAnimation(ids.map { AnimationFrame(sheet.sprite(it), 1f / 3f) }, looping = true)
    }
    return SpriteAnimationComponent(animations, initial)
}
private fun distance(a: GameObject, b: GameObject): Float = distance(a, b.x + b.width * .5f, b.y + b.height * .5f)
private fun distance(a: GameObject, x: Float, y: Float): Float {
    val dx = a.x + a.width * .5f - x; val dy = a.y + a.height * .5f - y
    return sqrt(dx * dx + dy * dy)
}
private fun ring(c: RenderContext, x: Float, y: Float, r: Float, color: Int, width: Float) {
    var px = x + r; var py = y
    for (i in 1..40) {
        val angle = i * (Math.PI * 2.0 / 40.0)
        val nx = x + kotlin.math.cos(angle).toFloat() * r
        val ny = y + kotlin.math.sin(angle).toFloat() * r
        c.drawLine(px, py, nx, ny, color, width); px = nx; py = ny
    }
}
