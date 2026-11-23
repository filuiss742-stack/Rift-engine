package com.rift.engine.rts

import com.rift.engine.graphics3d.Vector3
import org.junit.Assert.*
import org.junit.Test

class RtsWorldTest {
    private val heroSpec = RtsUnitSpec(
        role = RtsUnitRole.HERO,
        maxHealth = 100f,
        moveSpeed = 5f,
        attackPower = 20f,
        attackRange = 2f,
        attackCooldownSeconds = 1f
    )

    private val infantrySpec = RtsUnitSpec(
        role = RtsUnitRole.INFANTRY,
        maxHealth = 40f,
        moveSpeed = 2f,
        attackPower = 5f,
        attackRange = 1.5f,
        attackCooldownSeconds = 1f,
        resourceCost = 10
    )

    private fun addHeadquarters(world: RtsWorld) {
        val hq = RtsBuildingSpec(RtsBuildingType.HQ, 1000f, 0)
        world.addBuilding(RtsBuilding("player_hq", hq, RtsTeam.PLAYER, Vector3.ZERO))
        world.addBuilding(RtsBuilding("enemy_hq", hq, RtsTeam.ENEMY, Vector3(50f, 0f, 50f)))
    }

    @Test fun campaignHeroDeathDefeatsTheWholeMatch() {
        val world = RtsWorld()
        addHeadquarters(world)
        world.addUnit(RtsUnit("hero", RtsTeam.PLAYER, heroSpec, Vector3.ZERO, isCampaignHero = true))
        assertEquals(RtsMatchStatus.RUNNING, world.status)

        assertTrue(world.applyDamage("hero", 100f))
        assertEquals(RtsMatchStatus.DEFEAT, world.status)
    }

    @Test fun moveOrderAdvancesOnGroundPlane() {
        val world = RtsWorld()
        addHeadquarters(world)
        val unit = RtsUnit("scout", RtsTeam.PLAYER, infantrySpec, Vector3.ZERO)
        world.addUnit(unit)

        assertTrue(world.issueOrder("scout", RtsOrder.MoveTo(Vector3(10f, 0f, 0f))))
        world.update(1f)

        assertEquals(2f, unit.position.x, 0.001f)
        assertEquals(0f, unit.position.z, 0.001f)
    }

    @Test fun productionSpendsResourcesAndCreatesAUnit() {
        val world = RtsWorld(playerEconomy = RtsEconomy(resources = 20))
        addHeadquarters(world)
        val barracksSpec = RtsBuildingSpec(RtsBuildingType.BARRACKS, 300f, 20, productionCapacity = 2)
        world.addBuilding(RtsBuilding("barracks", barracksSpec, RtsTeam.PLAYER, Vector3.ZERO))

        assertTrue(world.queueProduction("barracks", infantrySpec))
        assertEquals(10, world.playerEconomy.resources)
        world.update(3.1f)

        assertEquals(1, world.units.size)
        assertEquals(RtsUnitRole.INFANTRY, world.units.single().spec.role)
    }

    @Test fun enemyCannotBeIssuedAsAttackTargetToSameTeam() {
        val world = RtsWorld()
        addHeadquarters(world)
        world.addUnit(RtsUnit("a", RtsTeam.PLAYER, infantrySpec, Vector3.ZERO))
        world.addUnit(RtsUnit("b", RtsTeam.PLAYER, infantrySpec, Vector3(1f, 0f, 0f)))

        assertFalse(world.issueOrder("a", RtsOrder.Attack("b")))
        assertTrue(world.issueOrder("a", RtsOrder.MoveTo(Vector3(4f, 0f, 4f))))
    }
}
