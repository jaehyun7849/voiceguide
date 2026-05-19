package com.voiceguide.detection

import org.junit.Assert.assertTrue
import org.junit.Test

class PerformanceOverlayFormatterTest {
    @Test
    fun `formats every measured stage for the overlay`() {
        val text = PerformanceOverlayFormatter.format(
            PerformanceMetrics(
                frameIntervalMillis = 1_500,
                yuvToNv21Millis = 4,
                inputTransformMillis = 8,
                inferenceMillis = 33,
                postprocessMillis = 5,
                totalMillis = 50,
                detectedObstacleCount = 2
            )
        )

        assertTrue(text.contains("analysis: 0.67 FPS"))
        assertTrue(!text.contains("frame interval: 1500 ms"))
        assertTrue(text.contains("YUV->NV21: 4 ms"))
        assertTrue(text.contains("input: 8 ms"))
        assertTrue(text.contains("inference: 33 ms"))
        assertTrue(text.contains("postprocess: 5 ms"))
        assertTrue(text.contains("total: 50 ms"))
        assertTrue(text.contains("detections: 2"))
    }
}
