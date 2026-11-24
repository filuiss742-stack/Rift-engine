package com.example.game

import com.rift.engine.combat.HealthComponent
import com.rift.engine.entity.GameObject
import com.rift.engine.graphics.CameraRig
import com.rift.engine.input.EngineKey
import com.rift.engine.movement.MovementInput
import com.rift.engine.movement.TopDownMovementProfile
import com.rift.engine.physics.Aabb
import com.rift.engine.scene.Scene
import com.rift.engine.ui.UiLabel
import com.rift.engine.ui.VirtualButton
import com.rift.engine.ui.VirtualJoystick
import kotlin.math.sqrt

/**
 * Small acceptance game built only from Rift's public game-facing APIs.
 * It exercises movement, physics ownership, camera follow, combat, enemy AI,
 * pickups, HUD, keyboard, and multi-touch controls.
 */
class WorldScene : Scene() {
    internal val player = GameObject(
        x = 120f,
        y = 180f,
        width = 30f,
        height = 30f,
        color = 0xFF67B7FF.toInt()
    )
    internal val enemy = GameObject(
        x = 310f,
        y = 180f,
        width = 30f,
        height = 30f,
        color = 0xFFE36B6B.toInt()
    )
    internal val enemyHealth = HealthComponent(30f, onDeath = ::onEnemyDefeated)
    internal var coin: GameObject? = null
        private set
    internal var coins: Int = 0
        private set
    internal var enemyDefeated: Boolean = false
        private set

    internal val joystick = VirtualJoystick(24f, 460f, 112f)
    internal val attackButton = VirtualButton(
        824f, 460f, 100f, 100f, "HIT",
        onPressed = { touchAttackPressed = true }
    )
    internal val dodgeButton = VirtualButton(
        700f, 460f, 100f, 100f, "DODGE",
        onPressed = { touchDodgePressed = true }
    )
    internal val statusLabel = UiLabel(16f, 16f, 640f, 28f, "Enemy: 30 HP   Coins: 0")
    internal val cameraRig = CameraRig(
        camera = camera,
        viewportWidth = 960f,
        viewportHeight = 600f,
        smoothing = 9f,
        deadZoneWidth = 72f,
        deadZoneHeight = 48f,
        worldBounds = Aabb(0f, 0f, 1800f, 1200f)
    )

    private val walkingProfile = TopDownMovementProfile(speed = 180f)
    private val dodgingProfile = TopDownMovementProfile(speed = 430f)
    private var attackCooldown = 0f
    private var dodgeCooldown = 0f
    private var dodgeRemaining = 0f
    private var lastDirectionX = 1f
    private var lastDirectionY = 0f
    private var touchAttackPressed = false
    private var touchDodgePressed = false

    init {
        input.bindKey("move.left", EngineKey.A)
        input.bindKey("move.left", EngineKey.LEFT)
        input.bindKey("move.right", EngineKey.D)
        input.bindKey("move.right", EngineKey.RIGHT)
        input.bindKey("move.up", EngineKey.W)
        input.bindKey("move.up", EngineKey.UP)
        input.bindKey("move.down", EngineKey.S)
        input.bindKey("move.down", EngineKey.DOWN)
        input.bindKey("attack", EngineKey.SPACE)
        input.bindKey("dodge", EngineKey.SHIFT)

        player.movementProfile = walkingProfile
        enemy.movementProfile = TopDownMovementProfile(speed = 68f)
        enemy.addComponent(enemyHealth)

        physics.gravityX = 0f
        physics.gravityY = 0f
        physics.addBody(player, gravityScale = 0f)

        add(player)
        add(enemy)
        combat.addHurtbox(enemy, teamId = "enemy")

        ui.add(statusLabel)
        ui.add(joystick)
        ui.add(dodgeButton)
        ui.add(attackButton)

        cameraRig.snapTo(player)
    }

    override fun update(deltaSeconds: Float) {
        val horizontal = (
            (if (input.isDown("move.right")) 1f else 0f) -
                (if (input.isDown("move.left")) 1f else 0f) +
                joystick.axisX
            ).coerceIn(-1f, 1f)
        val vertical = (
            (if (input.isDown("move.down")) 1f else 0f) -
                (if (input.isDown("move.up")) 1f else 0f) +
                joystick.axisY
            ).coerceIn(-1f, 1f)

        val lengthSquared = horizontal * horizontal + vertical * vertical
        val normalization = if (lengthSquared > 1f) 1f / sqrt(lengthSquared) else 1f
        val moveX = horizontal * normalization
        val moveY = vertical * normalization

        if (moveX != 0f || moveY != 0f) {
            lastDirectionX = moveX
            lastDirectionY = moveY
        }

        attackCooldown = (attackCooldown - deltaSeconds).coerceAtLeast(0f)
        dodgeCooldown = (dodgeCooldown - deltaSeconds).coerceAtLeast(0f)

        val attackPressed = input.wasPressed("attack") || touchAttackPressed
        val dodgePressed = input.wasPressed("dodge") || touchDodgePressed
        touchAttackPressed = false
        touchDodgePressed = false

        if (dodgePressed && dodgeCooldown <= 0f) {
            dodgeRemaining = 0.16f
            dodgeCooldown = 0.75f
        }

        val isDodging = dodgeRemaining > 0f
        player.movementProfile = if (isDodging) dodgingProfile else walkingProfile
        player.movementInput = if (isDodging) {
            MovementInput(lastDirectionX, lastDirectionY)
        } else {
            MovementInput(moveX, moveY)
        }

        if (isDodging) {
            dodgeRemaining = (dodgeRemaining - deltaSeconds).coerceAtLeast(0f)
        }

        val enemyDx = player.x - enemy.x
        val enemyDy = player.y - enemy.y
        val enemyLength = sqrt(enemyDx * enemyDx + enemyDy * enemyDy)
        enemy.movementInput = if (!enemyDefeated && enemyLength > 0.001f) {
            MovementInput(enemyDx / enemyLength, enemyDy / enemyLength)
        } else {
            MovementInput()
        }

        if (attackPressed && attackCooldown <= 0f && !enemyDefeated) {
            combat.addHitbox(
                owner = player,
                damage = 10f,
                teamId = "player",
                targetTeams = setOf("enemy"),
                durationSeconds = 0.09f
            )
            attackCooldown = 0.28f
        }

        super.update(deltaSeconds)

        collectCoinIfTouched()
        cameraRig.update(player, deltaSeconds)
        statusLabel.text = if (enemyDefeated) {
            "Enemy defeated   Coins: $coins"
        } else {
            "Enemy: ${enemyHealth.currentHealth.toInt()} HP   Coins: $coins"
        }
    }

    private fun onEnemyDefeated() {
        if (enemyDefeated) return
        enemyDefeated = true
        val dropped = GameObject(
            x = enemy.x + enemy.width + 12f,
            y = enemy.y + 8f,
            width = 14f,
            height = 14f,
            color = 0xFFFFD166.toInt()
        )
        dropped.renderLayer = 2
        coin = dropped
        remove(enemy)
        add(dropped)
        cameraRig.shake(amplitude = 5f, durationSeconds = 0.14f)
    }

    private fun collectCoinIfTouched() {
        val dropped = coin ?: return
        val playerBounds = player.bounds()
        val coinBounds = dropped.bounds()
        val overlaps = playerBounds.left < coinBounds.right &&
            playerBounds.right > coinBounds.left &&
            playerBounds.top < coinBounds.bottom &&
            playerBounds.bottom > coinBounds.top
        if (overlaps) {
            coins += 1
            remove(dropped)
            coin = null
        }
    }
}
