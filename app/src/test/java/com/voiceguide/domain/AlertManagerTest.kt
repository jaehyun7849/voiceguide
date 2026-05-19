package com.voiceguide.domain

import com.voiceguide.detection.ObstacleClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlertManagerTest {
    @Test
    fun `chooses front danger before side near obstacle`() {
        val manager = AlertManager()
        val alert = manager.nextAlert(
            facts = listOf(
                fact(ObstacleClass.CHAIR, GuidancePosition.RIGHT, RiskLevel.NEAR),
                fact(ObstacleClass.PERSON, GuidancePosition.FRONT, RiskLevel.DANGER)
            ),
            nowMillis = 1_000L,
            forceRepeat = false
        )

        assertEquals("주의, 정면 가까이에 사람이 있습니다.", alert?.message)
        assertEquals(RiskLevel.DANGER, alert?.riskLevel)
    }

    @Test
    fun `suppresses repeated same alert within cooldown`() {
        val manager = AlertManager(cooldownMillis = 4_000L)
        val facts = listOf(fact(ObstacleClass.CHAIR, GuidancePosition.RIGHT, RiskLevel.NEAR))

        val first = manager.nextAlert(facts, nowMillis = 1_000L, forceRepeat = false)
        val second = manager.nextAlert(facts, nowMillis = 2_000L, forceRepeat = false)

        assertEquals("오른쪽 가까이에 의자가 있습니다.", first?.message)
        assertNull(second)
    }

    @Test
    fun `force repeat bypasses cooldown`() {
        val manager = AlertManager(cooldownMillis = 4_000L)
        val facts = listOf(fact(ObstacleClass.DOOR, GuidancePosition.FRONT, RiskLevel.NORMAL))

        manager.nextAlert(facts, nowMillis = 1_000L, forceRepeat = false)
        val repeated = manager.nextAlert(facts, nowMillis = 2_000L, forceRepeat = true)

        assertEquals("정면에 문이 있습니다.", repeated?.message)
    }

    private fun fact(
        obstacleClass: ObstacleClass,
        position: GuidancePosition,
        riskLevel: RiskLevel
    ) = GuidanceFact(
        obstacleClass = obstacleClass,
        position = position,
        riskLevel = riskLevel,
        confidence = 0.9f
    )
}
