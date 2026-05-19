package com.voiceguide.camera

data class YuvPlaneData(
    val bytes: ByteArray,
    val rowStride: Int,
    val pixelStride: Int
)

object Yuv420ToNv21Converter {
    fun convert(
        width: Int,
        height: Int,
        yPlane: YuvPlaneData,
        uPlane: YuvPlaneData,
        vPlane: YuvPlaneData
    ): ByteArray {
        val output = ByteArray(width * height + width * height / 2)
        var outputIndex = 0

        for (row in 0 until height) {
            val rowOffset = row * yPlane.rowStride
            for (col in 0 until width) {
                output[outputIndex++] = yPlane.bytes[rowOffset + col * yPlane.pixelStride]
            }
        }

        val chromaHeight = height / 2
        val chromaWidth = width / 2
        for (row in 0 until chromaHeight) {
            val uRowOffset = row * uPlane.rowStride
            val vRowOffset = row * vPlane.rowStride
            for (col in 0 until chromaWidth) {
                output[outputIndex++] = vPlane.bytes[vRowOffset + col * vPlane.pixelStride]
                output[outputIndex++] = uPlane.bytes[uRowOffset + col * uPlane.pixelStride]
            }
        }

        return output
    }
}
