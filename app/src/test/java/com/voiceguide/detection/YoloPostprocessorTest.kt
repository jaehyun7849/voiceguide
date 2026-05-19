package com.voiceguide.detection

import org.junit.Assert.assertEquals
import org.junit.Test

class YoloPostprocessorTest {
    @Test
    fun `decodes yolo rows and suppresses overlapping lower confidence boxes`() {
        val rowSize = 85
        val output = FloatArray(rowSize * 2)
        writeCandidate(output, 0, rowSize, cx = 320f, cy = 320f, w = 128f, h = 64f, objectness = 0.9f, personScore = 0.8f)
        writeCandidate(output, 1, rowSize, cx = 322f, cy = 320f, w = 128f, h = 64f, objectness = 0.7f, personScore = 0.8f)

        val detections = YoloPostprocessor(
            inputWidth = 640,
            inputHeight = 640,
            confidenceThreshold = 0.5f,
            iouThreshold = 0.45f
        ).postprocess(output, intArrayOf(1, 2, rowSize))

        assertEquals(1, detections.size)
        assertEquals(ObstacleClass.PERSON, detections.first().obstacleClass)
        assertEquals(0.72f, detections.first().confidence, 0.001f)
        assertEquals(0.4f, detections.first().boundingBox.left, 0.001f)
        assertEquals(0.45f, detections.first().boundingBox.top, 0.001f)
        assertEquals(0.6f, detections.first().boundingBox.right, 0.001f)
        assertEquals(0.55f, detections.first().boundingBox.bottom, 0.001f)
    }

    @Test
    fun `decodes yolo26 nms free corner rows`() {
        val rowSize = 6
        val output = FloatArray(rowSize * 2)
        output[0] = 64f
        output[1] = 96f
        output[2] = 192f
        output[3] = 256f
        output[4] = 0.77f
        output[5] = 56f

        val detections = YoloPostprocessor(
            inputWidth = 320,
            inputHeight = 320,
            confidenceThreshold = 0.5f
        ).postprocess(output, intArrayOf(1, 2, rowSize))

        assertEquals(1, detections.size)
        assertEquals(ObstacleClass.CHAIR, detections.first().obstacleClass)
        assertEquals(0.77f, detections.first().confidence, 0.001f)
        assertEquals(0.2f, detections.first().boundingBox.left, 0.001f)
        assertEquals(0.3f, detections.first().boundingBox.top, 0.001f)
        assertEquals(0.6f, detections.first().boundingBox.right, 0.001f)
        assertEquals(0.8f, detections.first().boundingBox.bottom, 0.001f)
    }

    private fun writeCandidate(
        output: FloatArray,
        row: Int,
        rowSize: Int,
        cx: Float,
        cy: Float,
        w: Float,
        h: Float,
        objectness: Float,
        personScore: Float
    ) {
        val offset = row * rowSize
        output[offset] = cx
        output[offset + 1] = cy
        output[offset + 2] = w
        output[offset + 3] = h
        output[offset + 4] = objectness
        output[offset + 5] = personScore
    }
}
