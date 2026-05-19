package com.voiceguide.detection

import org.junit.Assert.assertEquals
import org.junit.Test

class Nv21TensorConverterTest {
    @Test
    fun `converts neutral black nv21 pixels to normalized rgb tensor`() {
        val frame = FrameInput(
            width = 2,
            height = 2,
            rotationDegrees = 0,
            format = FrameFormat.NV21,
            bytes = byteArrayOf(16, 16, 16, 16, 128.toByte(), 128.toByte())
        )

        val tensor = Nv21TensorConverter.toFloatRgbTensor(frame, targetWidth = 2, targetHeight = 2)

        assertEquals(12, tensor.size)
        tensor.forEach { value ->
            assertEquals(0f, value, 0.01f)
        }
    }
}
