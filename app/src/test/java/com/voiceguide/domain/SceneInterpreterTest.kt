package com.voiceguide.domain

import com.voiceguide.detection.BoundingBox
import com.voiceguide.detection.DetectedObstacle
import com.voiceguide.detection.ObstacleClass
import org.junit.Assert.assertEquals
import org.junit.Test

class SceneInterpreterTest {
    private val interpreter = SceneInterpreter()

    @Test
    fun `classifies horizontal position from bounding box center`() {
        val left = obstacle(0.05f, 0.2f)
        val front = obstacle(0.4f, 0.6f)
        val right = obstacle(0.8f, 0.95f)

        assertEquals(GuidancePosition.LEFT, interpreter.interpret(listOf(left)).first().position)
        assertEquals(GuidancePosition.FRONT, interpreter.interpret(listOf(front)).first().position)
        assertEquals(GuidancePosition.RIGHT, interpreter.interpret(listOf(right)).first().position)
    }

    @Test
    fun `marks centered large object as danger`() {
        val result = interpreter.interpret(listOf(obstacle(0.2f, 0.8f, top = 0.1f, bottom = 0.95f))).first()

        assertEquals(GuidancePosition.FRONT, result.position)
        assertEquals(RiskLevel.DANGER, result.riskLevel)
    }

    @Test
    fun `marks stairs as danger when confidence is high`() {
        val result = interpreter.interpret(
            listOf(obstacle(0.7f, 0.9f, obstacleClass = ObstacleClass.STAIRS, confidence = 0.82f))
        ).first()

        assertEquals(RiskLevel.DANGER, result.riskLevel)
    }

    private fun obstacle(
        left: Float,
        right: Float,
        top: Float = 0.2f,
        bottom: Float = 0.7f,
        obstacleClass: ObstacleClass = ObstacleClass.PERSON,
        confidence: Float = 0.9f
    ) = DetectedObstacle(
        obstacleClass = obstacleClass,
        confidence = confidence,
        boundingBox = BoundingBox(left = left, top = top, right = right, bottom = bottom)
    )
}
