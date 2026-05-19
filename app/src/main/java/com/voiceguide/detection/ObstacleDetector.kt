package com.voiceguide.detection

interface ObstacleDetector {
    fun detect(frame: FrameInput): DetectionResult

    fun close() = Unit
}

data class FrameInput(
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val format: FrameFormat,
    val bytes: ByteArray,
    val performance: PerformanceMetrics = PerformanceMetrics()
)

enum class FrameFormat {
    NV21
}

data class DetectedObstacle(
    val obstacleClass: ObstacleClass,
    val confidence: Float,
    val boundingBox: BoundingBox
)

data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val centerX: Float = (left + right) / 2f
    val width: Float = right - left
    val height: Float = bottom - top
    val area: Float = width * height
}

enum class ObstacleClass {
    PERSON,
    CHAIR,
    TABLE,
    DOOR,
    STAIRS
}

data class DetectionResult(
    val obstacles: List<DetectedObstacle>,
    val metrics: PerformanceMetrics = PerformanceMetrics()
)
