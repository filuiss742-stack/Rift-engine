package com.rift.engine.rts

import com.rift.engine.graphics3d.Vector3
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Platform-neutral RTS simulation primitives.
 *
 * The simulation is deliberately renderer-agnostic: the same world state can be
 * presented by an elevated RTS camera or inspected by tools and tests.
 */
enum class RtsTeam { PLAYER, ENEMY, NEUTRAL }
enum class RtsUnitRole { HERO, INFANTRY, CAVALRY, RANGED, MAGE, SIEGE, GOLEM }
enum class RtsBuildingType { HQ, BARRACKS, ACADEMY, TOWER, OUTPOST, RESOURCE_NODE }
enum class RtsMatchStatus { RUNNING, VICTORY, DEFEAT }

sealed interface RtsOrder {
    data class MoveTo(val destination: Vector3) : RtsOrder
    data class Attack(val targetId: String) : RtsOrder
    data class Capture(val buildingId: String) : RtsOrder
    data object Hold : RtsOrder
}

data class RtsUnitSpec(
    val role: RtsUnitRole,
    val maxHealth: Float,
    val moveSpeed: Float,
    val attackPower: Float,
    val attackRange: Float,
    val attackCooldownSeconds: Float,
    val resourceCost: Int = 0
) {
    init {
        require(maxHealth > 0f && maxHealth.isFinite())
        require(moveSpeed >= 0f && moveSpeed.isFinite())
        require(attackPower >= 0f && attackPower.isFinite())
        require(attackRange >= 0f && attackRange.isFinite())
        require(attackCooldownSeconds > 0f && attackCooldownSeconds.isFinite())
        require(resourceCost >= 0)
    }
}

data class RtsUnit(
    val id: String,
    val team: RtsTeam,
    val spec: RtsUnitSpec,
    var position: Vector3,
    var health: Float = spec.maxHealth,
    var order: RtsOrder = RtsOrder.Hold,
    var cooldownRemaining: Float = 0f,
    val isCampaignHero: Boolean = false
) {
    init {
        require(id.isNotBlank())
        require(health.isFinite() && health in 0f..spec.maxHealth)
        require(cooldownRemaining.isFinite() && cooldownRemaining >= 0f)
        require(!isCampaignHero || spec.role == RtsUnitRole.HERO) {
            "A campaign hero must use the HERO role."
        }
    }

    val isAlive: Boolean get() = health > 0f
}

data class RtsBuildingSpec(
    val type: RtsBuildingType,
    val maxHealth: Float,
    val resourceCost: Int,
    val productionCapacity: Int = 0
) {
    init {
        require(maxHealth > 0f && maxHealth.isFinite())
        require(resourceCost >= 0)
        require(productionCapacity >= 0)
    }
}

data class RtsBuilding(
    val id: String,
    val spec: RtsBuildingSpec,
    var team: RtsTeam,
    var position: Vector3,
    var health: Float = spec.maxHealth,
    var captureProgress: Float = 0f,
    val productionQueue: MutableList<RtsProductionOrder> = mutableListOf()
) {
    init {
        require(id.isNotBlank())
        require(health.isFinite() && health in 0f..spec.maxHealth)
        require(captureProgress.isFinite() && captureProgress in 0f..1f)
    }
}

data class RtsProductionOrder(val unitSpec: RtsUnitSpec, var remainingSeconds: Float) {
    init { require(remainingSeconds.isFinite() && remainingSeconds >= 0f) }
}

data class RtsEconomy(
    var resources: Int = 0,
    var incomePerMinute: Int = 0,
    var incomeAccumulator: Float = 0f
) {
    init {
        require(resources >= 0 && incomePerMinute >= 0)
        require(incomeAccumulator.isFinite() && incomeAccumulator >= 0f)
    }
}

data class RtsMatchRules(
    val heroDeathCausesDefeat: Boolean = true,
    val defeatWhenNoHeadquarters: Boolean = true,
    val victoryWhenAllEnemyHeadquartersLost: Boolean = true
)

/**
 * A deterministic, renderer-independent RTS match state.
 *
 * This first slice provides command/state/economy/production/capture and
 * campaign-hero defeat rules. Navigation meshes, obstacle-aware pathfinding,
 * combat presentation, fog-of-war and AI planning are separate systems.
 */
class RtsWorld(
    val rules: RtsMatchRules = RtsMatchRules(),
    val playerEconomy: RtsEconomy = RtsEconomy(),
    val enemyEconomy: RtsEconomy = RtsEconomy()
) {
    private val mutableUnits = linkedMapOf<String, RtsUnit>()
    private val mutableBuildings = linkedMapOf<String, RtsBuilding>()
    val units: List<RtsUnit> get() = mutableUnits.values.toList()
    val buildings: List<RtsBuilding> get() = mutableBuildings.values.toList()

    var status: RtsMatchStatus = RtsMatchStatus.RUNNING
        private set
    var elapsedSeconds: Double = 0.0
        private set

    fun addUnit(unit: RtsUnit) {
        check(status == RtsMatchStatus.RUNNING) { "Cannot add units after match end." }
        require(!mutableUnits.containsKey(unit.id)) { "Duplicate RTS unit id: ${unit.id}" }
        require(!(unit.isCampaignHero && mutableUnits.values.any { it.isCampaignHero && it.team == unit.team })) {
            "Only one campaign hero per team is supported in this match."
        }
        mutableUnits[unit.id] = unit
        evaluateTerminalState()
    }

    fun addBuilding(building: RtsBuilding) {
        check(status == RtsMatchStatus.RUNNING) { "Cannot add buildings after match end." }
        require(!mutableBuildings.containsKey(building.id)) { "Duplicate RTS building id: ${building.id}" }
        mutableBuildings[building.id] = building
        evaluateTerminalState()
    }

    fun unit(id: String): RtsUnit? = mutableUnits[id]
    fun building(id: String): RtsBuilding? = mutableBuildings[id]

    fun issueOrder(unitId: String, order: RtsOrder): Boolean {
        if (status != RtsMatchStatus.RUNNING) return false
        val unit = mutableUnits[unitId] ?: return false
        if (!unit.isAlive || unit.team == RtsTeam.NEUTRAL) return false
        when (order) {
            is RtsOrder.Attack -> {
                val target = mutableUnits[order.targetId] ?: return false
                if (!target.isAlive || target.team == unit.team || target.team == RtsTeam.NEUTRAL) return false
            }
            is RtsOrder.Capture -> {
                val target = mutableBuildings[order.buildingId] ?: return false
                if (target.team == unit.team) return false
            }
            is RtsOrder.MoveTo -> Unit
            RtsOrder.Hold -> Unit
        }
        unit.order = order
        return true
    }

    fun queueProduction(buildingId: String, unitSpec: RtsUnitSpec): Boolean {
        if (status != RtsMatchStatus.RUNNING) return false
        val building = mutableBuildings[buildingId] ?: return false
        if (building.team == RtsTeam.NEUTRAL || building.spec.productionCapacity <= 0) return false
        if (building.productionQueue.size >= building.spec.productionCapacity) return false
        val economy = economyFor(building.team)
        if (economy.resources < unitSpec.resourceCost) return false
        economy.resources -= unitSpec.resourceCost
        building.productionQueue += RtsProductionOrder(unitSpec, PRODUCTION_SECONDS_PER_UNIT)
        return true
    }

    /** Applies one bounded simulation step. Hosts should call this from a fixed-step accumulator. */
    fun update(deltaSeconds: Float) {
        require(deltaSeconds.isFinite() && deltaSeconds >= 0f)
        if (status != RtsMatchStatus.RUNNING || deltaSeconds == 0f) return
        var remaining = deltaSeconds
        while (remaining > 0f && status == RtsMatchStatus.RUNNING) {
            val step = min(remaining, MAX_STEP_SECONDS)
            simulateStep(step)
            remaining -= step
        }
    }

    /** Explicitly applies gameplay damage, allowing abilities and scripted events to share rules. */
    fun applyDamage(targetId: String, amount: Float): Boolean {
        require(amount.isFinite() && amount >= 0f)
        if (status != RtsMatchStatus.RUNNING) return false
        val target = mutableUnits[targetId] ?: return false
        if (!target.isAlive || amount == 0f) return false
        target.health = max(0f, target.health - amount)
        if (!target.isAlive) target.order = RtsOrder.Hold
        evaluateTerminalState()
        return true
    }

    fun heal(unitId: String, amount: Float): Boolean {
        require(amount.isFinite() && amount >= 0f)
        if (status != RtsMatchStatus.RUNNING) return false
        val target = mutableUnits[unitId] ?: return false
        if (!target.isAlive || amount == 0f) return false
        target.health = min(target.spec.maxHealth, target.health + amount)
        return true
    }

    private fun simulateStep(dt: Float) {
        elapsedSeconds += dt.toDouble()
        updateEconomy(playerEconomy, dt)
        updateEconomy(enemyEconomy, dt)
        updateProduction(dt)
        updateMovement(dt)
        updateCapture(dt)
        updateCombat(dt)
        mutableUnits.values.removeAll { !it.isAlive && !it.isCampaignHero }
        evaluateTerminalState()
    }

    private fun updateEconomy(economy: RtsEconomy, dt: Float) {
        economy.incomeAccumulator += economy.incomePerMinute * dt / 60f
        val whole = economy.incomeAccumulator.toInt()
        if (whole > 0) {
            economy.resources += whole
            economy.incomeAccumulator -= whole
        }
    }

    private fun updateProduction(dt: Float) {
        mutableBuildings.values.forEach { building ->
            if (building.team == RtsTeam.NEUTRAL || building.productionQueue.isEmpty()) return@forEach
            val order = building.productionQueue.first()
            order.remainingSeconds = max(0f, order.remainingSeconds - dt)
            if (order.remainingSeconds <= 0f) {
                val id = "${building.id}_unit_${nextUnitId++}"
                val spawn = building.position + Vector3(2f, 0f, 2f)
                mutableUnits[id] = RtsUnit(id, building.team, order.unitSpec, spawn)
                building.productionQueue.removeAt(0)
            }
        }
    }

    private fun updateMovement(dt: Float) {
        mutableUnits.values.forEach { unit ->
            if (!unit.isAlive) return@forEach
            val order = unit.order
            if (order is RtsOrder.MoveTo) {
                unit.position = moveToward(unit.position, order.destination, unit.spec.moveSpeed * dt)
                if (distanceOnGround(unit.position, order.destination) < ARRIVAL_EPSILON) {
                    unit.position = order.destination
                    unit.order = RtsOrder.Hold
                }
            }
        }
    }

    private fun updateCapture(dt: Float) {
        mutableUnits.values.forEach { unit ->
            val order = unit.order as? RtsOrder.Capture ?: return@forEach
            val building = mutableBuildings[order.buildingId] ?: run {
                unit.order = RtsOrder.Hold
                return@forEach
            }
            if (!unit.isAlive || distanceOnGround(unit.position, building.position) > CAPTURE_RADIUS) return@forEach
            building.captureProgress = min(1f, building.captureProgress + dt / CAPTURE_SECONDS)
            if (building.captureProgress >= 1f) {
                building.team = unit.team
                building.captureProgress = 0f
                unit.order = RtsOrder.Hold
                economyFor(unit.team).incomePerMinute += CAPTURED_OUTPOST_INCOME
            }
        }
    }

    private fun updateCombat(dt: Float) {
        mutableUnits.values.forEach { attacker ->
            if (!attacker.isAlive) return@forEach
            attacker.cooldownRemaining = max(0f, attacker.cooldownRemaining - dt)
            val order = attacker.order as? RtsOrder.Attack ?: return@forEach
            val target = mutableUnits[order.targetId]
            if (target == null || !target.isAlive || target.team == attacker.team) {
                attacker.order = RtsOrder.Hold
                return@forEach
            }
            if (attacker.cooldownRemaining <= 0f &&
                distanceOnGround(attacker.position, target.position) <= attacker.spec.attackRange
            ) {
                target.health = max(0f, target.health - attacker.spec.attackPower)
                attacker.cooldownRemaining = attacker.spec.attackCooldownSeconds
                if (!target.isAlive) {
                    attacker.order = RtsOrder.Hold
                    if (target.isCampaignHero && rules.heroDeathCausesDefeat) {
                        status = RtsMatchStatus.DEFEAT
                    }
                }
            }
        }
    }

    private fun evaluateTerminalState() {
        if (status != RtsMatchStatus.RUNNING) return
        if (rules.heroDeathCausesDefeat && mutableUnits.values.any {
                it.isCampaignHero && it.team == RtsTeam.PLAYER && !it.isAlive
            }) {
            status = RtsMatchStatus.DEFEAT
            return
        }
        if (rules.defeatWhenNoHeadquarters) {
            val playerHq = mutableBuildings.values.any { it.team == RtsTeam.PLAYER && it.spec.type == RtsBuildingType.HQ }
            if (!playerHq) {
                status = RtsMatchStatus.DEFEAT
                return
            }
        }
        if (rules.victoryWhenAllEnemyHeadquartersLost) {
            val enemyHqExists = mutableBuildings.values.any { it.team == RtsTeam.ENEMY && it.spec.type == RtsBuildingType.HQ }
            val anyEnemyHqWasAdded = mutableBuildings.values.any { it.spec.type == RtsBuildingType.HQ && it.team != RtsTeam.PLAYER }
            if (anyEnemyHqWasAdded && !enemyHqExists) status = RtsMatchStatus.VICTORY
        }
    }

    private fun economyFor(team: RtsTeam): RtsEconomy = when (team) {
        RtsTeam.PLAYER -> playerEconomy
        RtsTeam.ENEMY -> enemyEconomy
        RtsTeam.NEUTRAL -> error("Neutral entities do not own an economy.")
    }

    private fun moveToward(from: Vector3, to: Vector3, distance: Float): Vector3 {
        val dx = to.x - from.x
        val dz = to.z - from.z
        val length = sqrt(dx * dx + dz * dz)
        if (length <= distance || length < 1e-5f) return Vector3(to.x, from.y, to.z)
        val ratio = distance / length
        return Vector3(from.x + dx * ratio, from.y, from.z + dz * ratio)
    }

    private fun distanceOnGround(a: Vector3, b: Vector3): Float {
        val dx = a.x - b.x
        val dz = a.z - b.z
        return sqrt(dx * dx + dz * dz)
    }

    private var nextUnitId = 1

    companion object {
        private const val MAX_STEP_SECONDS = 0.05f
        private const val PRODUCTION_SECONDS_PER_UNIT = 3f
        private const val CAPTURE_RADIUS = 3f
        private const val CAPTURE_SECONDS = 5f
        private const val CAPTURED_OUTPOST_INCOME = 5
        private const val ARRIVAL_EPSILON = 0.05f
    }
}
