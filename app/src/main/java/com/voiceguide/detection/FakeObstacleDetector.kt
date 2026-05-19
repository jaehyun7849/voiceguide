package com.voiceguide.detection

class FakeObstacleDetector : ObstacleDetector {
    override fun detect(frame: FrameInput): DetectionResult {
        val detections = listOf(
            DetectedObstacle(
                obstacleClass = ObstacleClass.PERSON,
                confidence = 0.9f,
                boundingBox = BoundingBox(left = 0.38f, top = 0.18f, right = 0.62f, bottom = 0.82f)
            )
        )
        return DetectionResult(
            obstacles = detections,
            metrics = frame.performance.copy(
                inferenceMillis = 0,
                postprocessMillis = 0,
                totalMillis = frame.performance.yuvToNv21Millis,
                detectedObstacleCount = detections.size
            )
        )
    }
}
