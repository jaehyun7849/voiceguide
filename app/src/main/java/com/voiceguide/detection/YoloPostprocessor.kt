package com.voiceguide.detection

import kotlin.math.max
import kotlin.math.min

class YoloPostprocessor(
    private val inputWidth: Int,
    private val inputHeight: Int,
    private val confidenceThreshold: Float = 0.25f,
    private val iouThreshold: Float = 0.45f
) {
    fun postprocess(rawOutput: FloatArray, outputShape: IntArray): List<DetectedObstacle> {
        val layout = OutputLayout.from(outputShape)
        val candidates = mutableListOf<DetectedObstacle>()

        for (row in 0 until layout.rows) {
            if (layout.nmsFree) {
                val confidence = layout.value(rawOutput, row, 4)
                val obstacleClass = classFor(layout.value(rawOutput, row, 5).toInt(), 80)
                if (confidence >= confidenceThreshold && obstacleClass != null) {
                    candidates += DetectedObstacle(
                        obstacleClass = obstacleClass,
                        confidence = confidence,
                        boundingBox = decodeCorners(
                            left = layout.value(rawOutput, row, 0),
                            top = layout.value(rawOutput, row, 1),
                            right = layout.value(rawOutput, row, 2),
                            bottom = layout.value(rawOutput, row, 3)
                        )
                    )
                }
                continue
            }

            val classStart = layout.classStart
            val objectness = if (layout.hasObjectness) layout.value(rawOutput, row, 4) else 1f
            var bestClassIndex = -1
            var bestClassScore = 0f
            for (classIndex in 0 until layout.classCount) {
                val score = layout.value(rawOutput, row, classStart + classIndex)
                if (score > bestClassScore) {
                    bestClassScore = score
                    bestClassIndex = classIndex
                }
            }

            val confidence = objectness * bestClassScore
            val obstacleClass = classFor(bestClassIndex, layout.classCount)
            if (confidence >= confidenceThreshold && obstacleClass != null) {
                candidates += DetectedObstacle(
                    obstacleClass = obstacleClass,
                    confidence = confidence,
                    boundingBox = decodeBox(
                        centerX = layout.value(rawOutput, row, 0),
                        centerY = layout.value(rawOutput, row, 1),
                        width = layout.value(rawOutput, row, 2),
                        height = layout.value(rawOutput, row, 3)
                    )
                )
            }
        }

        return nonMaxSuppression(candidates)
    }

    private fun decodeCorners(left: Float, top: Float, right: Float, bottom: Float): BoundingBox {
        val scaleX = if (left > 1f || right > 1f) inputWidth.toFloat() else 1f
        val scaleY = if (top > 1f || bottom > 1f) inputHeight.toFloat() else 1f
        return BoundingBox(
            left = (left / scaleX).coerceIn(0f, 1f),
            top = (top / scaleY).coerceIn(0f, 1f),
            right = (right / scaleX).coerceIn(0f, 1f),
            bottom = (bottom / scaleY).coerceIn(0f, 1f)
        )
    }

    private fun decodeBox(centerX: Float, centerY: Float, width: Float, height: Float): BoundingBox {
        val scaleX = if (centerX > 1f || width > 1f) inputWidth.toFloat() else 1f
        val scaleY = if (centerY > 1f || height > 1f) inputHeight.toFloat() else 1f
        return BoundingBox(
            left = ((centerX - width / 2f) / scaleX).coerceIn(0f, 1f),
            top = ((centerY - height / 2f) / scaleY).coerceIn(0f, 1f),
            right = ((centerX + width / 2f) / scaleX).coerceIn(0f, 1f),
            bottom = ((centerY + height / 2f) / scaleY).coerceIn(0f, 1f)
        )
    }

    private fun nonMaxSuppression(candidates: List<DetectedObstacle>): List<DetectedObstacle> {
        val selected = mutableListOf<DetectedObstacle>()
        for (candidate in candidates.sortedByDescending { it.confidence }) {
            val overlapsSelected = selected.any {
                it.obstacleClass == candidate.obstacleClass && iou(it.boundingBox, candidate.boundingBox) >= iouThreshold
            }
            if (!overlapsSelected) {
                selected += candidate
            }
        }
        return selected
    }

    private fun iou(first: BoundingBox, second: BoundingBox): Float {
        val left = max(first.left, second.left)
        val top = max(first.top, second.top)
        val right = min(first.right, second.right)
        val bottom = min(first.bottom, second.bottom)
        val intersection = max(0f, right - left) * max(0f, bottom - top)
        val union = first.area + second.area - intersection
        return if (union <= 0f) 0f else intersection / union
    }

    private fun classFor(classIndex: Int, classCount: Int): ObstacleClass? {
        if (classIndex < 0) return null
        if (classCount <= ObstacleClass.entries.size) {
            return ObstacleClass.entries.getOrNull(classIndex)
        }
        return when (classIndex) {
            0 -> ObstacleClass.PERSON
            56 -> ObstacleClass.CHAIR
            60 -> ObstacleClass.TABLE
            else -> null
        }
    }

    private data class OutputLayout(
        val rows: Int,
        val attributes: Int,
        val transposed: Boolean,
        val nmsFree: Boolean,
        val hasObjectness: Boolean
    ) {
        val classStart: Int = if (hasObjectness) 5 else 4
        val classCount: Int = attributes - classStart

        fun value(rawOutput: FloatArray, row: Int, attribute: Int): Float {
            return if (transposed) {
                rawOutput[attribute * rows + row]
            } else {
                rawOutput[row * attributes + attribute]
            }
        }

        companion object {
            fun from(shape: IntArray): OutputLayout {
                require(shape.size == 3 && shape[0] == 1) { "Expected YOLO output shape [1, rows, attrs] or [1, attrs, rows]." }
                val second = shape[1]
                val third = shape[2]
                val transposed = second < third && second >= 6
                val rows = if (transposed) third else second
                val attributes = if (transposed) second else third
                val nmsFree = attributes == 6
                val hasObjectness = !nmsFree && attributes != 84 && attributes >= 6
                require(attributes >= 6) { "YOLO output must include boxes and class scores." }
                return OutputLayout(rows, attributes, transposed, nmsFree, hasObjectness)
            }
        }
    }
}
