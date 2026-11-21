package com.rift.engine.combat

import com.rift.engine.diagnostics.DiagnosticReporter
import com.rift.engine.entity.GameComponent
import com.rift.engine.entity.GameObject
import com.rift.engine.physics.Collision

/**
 * Reusable health state for an entity. Damage is clamped at zero and the
 * death callback fires once until the component is explicitly reset.
 */
class HealthComponent(
    val maxHealth: Float,
    initialHealth: Float = maxHealth,
    private val onDeath: () -> Unit = {}
) : GameComponent {
    var currentHealth: Float = initialHealth
        private set

    val isAlive: Boolean
        get() = currentHealth > 0f

    init {
        require(maxHealth.isFinite() && maxHealth > 0f) {
            "maxHealth must be finite and greater than zero."
        }
        require(initialHealth.isFinite() && initialHealth in 0f..maxHealth) {
            "initialHealth must be between zero and maxHealth."
        }
    }

    fun applyDamage(amount: Float): Float {
        require(amount.isFinite() && amount >= 0f) {
            "Damage must be finite and non-negative."
        }
        if (!isAlive || amount == 0f) return 0f
        val previous = currentHealth
        currentHealth = (currentHealth - amount).coerceAtLeast(0f)
        if (previous > 0f && currentHealth == 0f) onDeath()
        return previous - currentHealth
    }

    fun heal(amount: Float): Float {
        require(amount.isFinite() && amount >= 0f) {
            "Healing must be finite and non-negative."
        }
        if (!isAlive || amount == 0f) return 0f
        val previous = currentHealth
        currentHealth = (currentHealth + amount).coerceAtMost(maxHealth)
        return currentHealth - previous
    }

    fun reset(health: Float = maxHealth) {
        require(health.isFinite() && health in 0f..maxHealth) {
            "health must be between zero and maxHealth."
        }
        currentHealth = health
    }
}

class CombatHitbox internal constructor(
    val owner: GameObject,
    val damage: Float,
    val teamId: String,
    val targetTeams: Set<String>?,
    durationSeconds: Float?
) {
    internal val hitTargets = java.util.Collections.newSetFromMap(
        java.util.IdentityHashMap<GameObject, Boolean>()
    )
    internal var remainingSeconds: Float? = durationSeconds
    var enabled: Boolean = true
}

class CombatHurtbox internal constructor(
    val owner: GameObject,
    val health: HealthComponent,
    val teamId: String
) {
    var enabled: Boolean = true
}

/**
 * Lightweight real-time AABB combat resolver. PhysicsWorld remains responsible
 * for movement and physical response; CombatWorld handles damage and health.
 */
class CombatWorld(
    internal var diagnostics: DiagnosticReporter? = null
) {
    private val hitboxes = mutableListOf<CombatHitbox>()
    private val hurtboxes = mutableListOf<CombatHurtbox>()

    fun addHitbox(
        owner: GameObject,
        damage: Float,
        teamId: String,
        targetTeams: Set<String>? = null,
        durationSeconds: Float? = null
    ): CombatHitbox {
        require(damage.isFinite() && damage > 0f) {
            "Hitbox damage must be finite and greater than zero."
        }
        require(teamId.isNotBlank()) { "teamId must not be blank." }
        require(targetTeams == null || targetTeams.none { it.isBlank() }) {
            "targetTeams must not contain blank team IDs."
        }
        require(durationSeconds == null || durationSeconds.isFinite() && durationSeconds > 0f) {
            "durationSeconds must be null or finite and greater than zero."
        }
        return CombatHitbox(owner, damage, teamId, targetTeams?.toSet(), durationSeconds)
            .also(hitboxes::add)
    }

    fun addHurtbox(
        owner: GameObject,
        health: HealthComponent,
        teamId: String
    ): CombatHurtbox {
        require(teamId.isNotBlank()) { "teamId must not be blank." }
        require(hurtboxes.none { it.owner === owner && it.health === health }) {
            "This health component is already registered for this GameObject."
        }
        return CombatHurtbox(owner, health, teamId).also(hurtboxes::add)
    }

    fun removeHitbox(hitbox: CombatHitbox): Boolean =
        hitboxes.removeAll { it === hitbox }

    fun removeHurtbox(hurtbox: CombatHurtbox): Boolean =
        hurtboxes.removeAll { it === hurtbox }

    fun removeOwner(owner: GameObject) {
        hitboxes.removeAll { it.owner === owner }
        hurtboxes.removeAll { it.owner === owner }
    }

    fun clear() {
        hitboxes.clear()
        hurtboxes.clear()
    }

    fun hitboxes(): List<CombatHitbox> = hitboxes.toList()
    fun hurtboxes(): List<CombatHurtbox> = hurtboxes.toList()

    fun step(deltaSeconds: Float) {
        require(deltaSeconds.isFinite() && deltaSeconds >= 0f) {
            "deltaSeconds must be finite and non-negative."
        }

        val expired = mutableListOf<CombatHitbox>()
        hitboxes.toList().forEach hitboxLoop@ { hitbox ->
            if (!hitbox.enabled || !hitbox.owner.active) {
                if (hitbox.remainingSeconds != null) {
                    hitbox.remainingSeconds = hitbox.remainingSeconds!! - deltaSeconds
                    if (hitbox.remainingSeconds!! <= 0f) expired += hitbox
                }
                return@hitboxLoop
            }

            hurtboxes.toList().forEach hurtboxLoop@ { hurtbox ->
                if (!hurtbox.enabled || !hurtbox.owner.active ||
                    hurtbox.owner === hitbox.owner ||
                    hurtbox.teamId == hitbox.teamId ||
                    (hitbox.targetTeams != null && hurtbox.teamId !in hitbox.targetTeams) ||
                    hurtbox.owner in hitbox.hitTargets ||
                    !hurtbox.health.isAlive
                ) {
                    return@hurtboxLoop
                }

                if (Collision.intersects(hitbox.owner.bounds(), hurtbox.owner.bounds())) {
                    try {
                        hurtbox.health.applyDamage(hitbox.damage)
                        hitbox.hitTargets.add(hurtbox.owner)
                    } catch (exception: Exception) {
                        diagnostics?.reportException(
                            "combat.damage",
                            exception,
                            "Inspect the target HealthComponent death callback and hitbox configuration."
                        ) ?: throw exception
                        hitbox.hitTargets.add(hurtbox.owner)
                    }
                }
            }

            if (hitbox.remainingSeconds != null) {
                hitbox.remainingSeconds = hitbox.remainingSeconds!! - deltaSeconds
                if (hitbox.remainingSeconds!! <= 0f) expired += hitbox
            }
        }

        expired.forEach(::removeHitbox)
    }
}
