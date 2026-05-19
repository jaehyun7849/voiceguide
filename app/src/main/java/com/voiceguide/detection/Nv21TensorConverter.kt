package com.voiceguide.detection

import kotlin.math.max
import kotlin.math.min

object Nv21TensorConverter {
    fun toFloatRgbTensor(
        frame: FrameInput,
        targetWidth: Int,
        targetHeight: Int
    ): FloatArray {
        return FloatArray(targetWidth * targetHeight * 3).also { tensor ->
            writeFloatRgbTensor(
                frame = frame,
                targetWidth = targetWidth,
                targetHeight = targetHeight,
                output = tensor
            )
        }
    }

    fun writeFloatRgbTensor(
        frame: FrameInput,
        targetWidth: Int,
        targetHeight: Int,
        output: FloatArray
    ) {
        require(frame.format == FrameFormat.NV21) { "Only NV21 frames are supported." }
        require(frame.bytes.size >= frame.width * frame.height * 3 / 2) { "NV21 buffer is too small." }
        require(output.size >= targetWidth * targetHeight * 3) { "Output tensor buffer is too small." }

        val rotatedWidth = if (frame.rotationDegrees == 90 || frame.rotationDegrees == 270) frame.height else frame.width
        val rotatedHeight = if (frame.rotationDegrees == 90 || frame.rotationDegrees == 270) frame.width else frame.height
        var out = 0

        for (targetY in 0 until targetHeight) {
            val rotatedY = targetY * rotatedHeight / targetHeight
            for (targetX in 0 until targetWidth) {
                val rotatedX = targetX * rotatedWidth / targetWidth
                val (sourceX, sourceY) = sourceCoordinate(frame, rotatedX, rotatedY)
                val rgb = nv21ToRgb(frame, sourceX, sourceY)
                output[out++] = rgb.red / 255f
                output[out++] = rgb.green / 255f
                output[out++] = rgb.blue / 255f
            }
        }
    }

    private fun sourceCoordinate(frame: FrameInput, rotatedX: Int, rotatedY: Int): Pair<Int, Int> {
        val x: Int
        val y: Int
        when (((frame.rotationDegrees % 360) + 360) % 360) {
            90 -> {
                x = rotatedY
                y = frame.height - 1 - rotatedX
            }
            180 -> {
                x = frame.width - 1 - rotatedX
                y = frame.height - 1 - rotatedY
            }
            270 -> {
                x = frame.width - 1 - rotatedY
                y = rotatedX
            }
            else -> {
                x = rotatedX
                y = rotatedY
            }
        }
        return Pair(x.coerceIn(0, frame.width - 1), y.coerceIn(0, frame.height - 1))
    }

    private fun nv21ToRgb(frame: FrameInput, x: Int, y: Int): RgbPixel {
        val ySize = frame.width * frame.height
        val yValue = frame.bytes[y * frame.width + x].toInt() and 0xFF
        val uvRow = y / 2
        val uvCol = x and 1.inv()
        val vValue = frame.bytes[ySize + uvRow * frame.width + uvCol].toInt() and 0xFF
        val uValue = frame.bytes[ySize + uvRow * frame.width + uvCol + 1].toInt() and 0xFF

        val c = yValue - 16
        val d = uValue - 128
        val e = vValue - 128
        val red = clamp((298 * c + 409 * e + 128) shr 8)
        val green = clamp((298 * c - 100 * d - 208 * e + 128) shr 8)
        val blue = clamp((298 * c + 516 * d + 128) shr 8)
        return RgbPixel(red, green, blue)
    }

    private fun clamp(value: Int): Int = min(255, max(0, value))

    private data class RgbPixel(
        val red: Int,
        val green: Int,
        val blue: Int
    )
}
