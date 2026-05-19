package com.voiceguide.detection

import java.util.Locale

data class PerformanceMetrics(
    val frameIntervalMillis: Long = 0,
    val yuvToNv21Millis: Long = 0,
    val inputTransformMillis: Long = 0,
    val inferenceMillis: Long = 0,
    val postprocessMillis: Long = 0,
    val totalMillis: Long = 0,
    val detectedObstacleCount: Int = 0
)

object PerformanceOverlayFormatter {
    fun format(metrics: PerformanceMetrics): String {
        return listOf(
            "analysis: ${formatFps(metrics.frameIntervalMillis)} FPS",
            "YUV->NV21: ${metrics.yuvToNv21Millis} ms",
            "input: ${metrics.inputTransformMillis} ms",
            "inference: ${metrics.inferenceMillis} ms",
            "postprocess: ${metrics.postprocessMillis} ms",
            "total: ${metrics.totalMillis} ms",
            "detections: ${metrics.detectedObstacleCount}"
        ).joinToString(separator = "\n")
    }

    private fun formatFps(frameIntervalMillis: Long): String {
        if (frameIntervalMillis <= 0L) return "0.00"
        return String.format(Locale.US, "%.2f", 1_000.0 / frameIntervalMillis)
    }
}
