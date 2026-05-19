package com.voiceguide.domain

import com.voiceguide.detection.ObstacleClass

class AlertManager(
    private val cooldownMillis: Long = 4_000L
) {
    private var lastAlertKey: AlertKey? = null
    private var lastAlertRisk: RiskLevel? = null
    private var lastAlertMillis: Long = Long.MIN_VALUE

    fun nextAlert(
        facts: List<GuidanceFact>,
        nowMillis: Long,
        forceRepeat: Boolean
    ): GuidanceAlert? {
        val selected = facts.maxWithOrNull(compareBy<GuidanceFact> { priorityOf(it) }.thenBy { it.confidence })
            ?: return if (forceRepeat) GuidanceAlert("감지된 주요 장애물이 없습니다.", RiskLevel.NORMAL) else null

        val key = AlertKey(selected.obstacleClass, selected.position)
        val withinCooldown = key == lastAlertKey && nowMillis - lastAlertMillis < cooldownMillis
        val riskIncreased = lastAlertRisk != null && selected.riskLevel.ordinal > lastAlertRisk!!.ordinal

        if (!forceRepeat && withinCooldown && !riskIncreased) {
            return null
        }

        lastAlertKey = key
        lastAlertRisk = selected.riskLevel
        lastAlertMillis = nowMillis

        return GuidanceAlert(messageOf(selected), selected.riskLevel)
    }

    private fun priorityOf(fact: GuidanceFact): Int {
        return when {
            fact.obstacleClass == ObstacleClass.STAIRS && fact.riskLevel == RiskLevel.DANGER -> 600
            fact.position == GuidancePosition.FRONT && fact.obstacleClass == ObstacleClass.PERSON && fact.riskLevel != RiskLevel.NORMAL -> 500
            fact.position == GuidancePosition.FRONT && fact.riskLevel != RiskLevel.NORMAL -> 400
            fact.position == GuidancePosition.FRONT && fact.obstacleClass == ObstacleClass.DOOR -> 300
            fact.position != GuidancePosition.FRONT && fact.riskLevel != RiskLevel.NORMAL -> 200
            else -> 100
        }
    }

    private fun messageOf(fact: GuidanceFact): String {
        val position = when (fact.position) {
            GuidancePosition.LEFT -> "왼쪽"
            GuidancePosition.FRONT -> "정면"
            GuidancePosition.RIGHT -> "오른쪽"
        }
        val subject = when (fact.obstacleClass) {
            ObstacleClass.PERSON -> "사람이"
            ObstacleClass.CHAIR -> "의자가"
            ObstacleClass.TABLE -> "책상이"
            ObstacleClass.DOOR -> "문이"
            ObstacleClass.STAIRS -> "계단이"
        }

        return when {
            fact.obstacleClass == ObstacleClass.STAIRS && fact.riskLevel == RiskLevel.DANGER -> "주의, 앞쪽에 계단이 있습니다."
            fact.riskLevel == RiskLevel.DANGER -> "주의, ${position} 가까이에 $subject 있습니다."
            fact.riskLevel == RiskLevel.NEAR -> "${position} 가까이에 $subject 있습니다."
            else -> "${position}에 $subject 있습니다."
        }
    }
}

data class GuidanceAlert(
    val message: String,
    val riskLevel: RiskLevel
)

private data class AlertKey(
    val obstacleClass: ObstacleClass,
    val position: GuidancePosition
)
