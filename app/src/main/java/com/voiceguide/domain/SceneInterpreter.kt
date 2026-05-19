package com.voiceguide.domain

import com.voiceguide.detection.DetectedObstacle
import com.voiceguide.detection.ObstacleClass

class SceneInterpreter {
    fun interpret(detections: List<DetectedObstacle>): List<GuidanceFact> {
        return detections
            .filter { it.confidence >= 0.5f }
            .map { detection ->
                GuidanceFact(
                    obstacleClass = detection.obstacleClass,
                    position = positionOf(detection),
                    riskLevel = riskOf(detection),
                    confidence = detection.confidence
                )
            }
    }

    private fun positionOf(detection: DetectedObstacle): GuidancePosition {
        val centerX = detection.boundingBox.centerX
        return when {
            centerX < 0.33f -> GuidancePosition.LEFT
            centerX > 0.66f -> GuidancePosition.RIGHT
            else -> GuidancePosition.FRONT
        }
    }

    private fun riskOf(detection: DetectedObstacle): RiskLevel {
        val isFront = positionOf(detection) == GuidancePosition.FRONT
        val area = detection.boundingBox.area
        return when {
            detection.obstacleClass == ObstacleClass.STAIRS && detection.confidence >= 0.75f -> RiskLevel.DANGER
            isFront && area >= 0.45f -> RiskLevel.DANGER
            area >= 0.24f || isFront -> RiskLevel.NEAR
            else -> RiskLevel.NORMAL
        }
    }
}

data class GuidanceFact(
    val obstacleClass: ObstacleClass,
    val position: GuidancePosition,
    val riskLevel: RiskLevel,
    val confidence: Float
)

enum class GuidancePosition {
    LEFT,
    FRONT,
    RIGHT
}

enum class RiskLevel {
    NORMAL,
    NEAR,
    DANGER
}
