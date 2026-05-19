package com.voiceguide.camera

import android.graphics.ImageFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.voiceguide.detection.FrameFormat
import com.voiceguide.detection.FrameInput
import java.nio.ByteBuffer

class CameraFrameAnalyzer(
    private val intervalMillis: Long = DEFAULT_INTERVAL_MILLIS,
    private val onFrame: (FrameInput) -> Unit
) : ImageAnalysis.Analyzer {
    companion object {
        const val DEFAULT_INTERVAL_MILLIS: Long = 500L
    }

    private var lastAnalyzedAt = 0L

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastAnalyzedAt >= intervalMillis) {
            val frameIntervalMillis = if (lastAnalyzedAt == 0L) 0L else now - lastAnalyzedAt
            lastAnalyzedAt = now
            if (image.format == ImageFormat.YUV_420_888 && image.planes.size >= 3) {
                val yuvStartNanos = System.nanoTime()
                val nv21Bytes = Yuv420ToNv21Converter.convert(
                    width = image.width,
                    height = image.height,
                    yPlane = image.planes[0].toYuvPlaneData(),
                    uPlane = image.planes[1].toYuvPlaneData(),
                    vPlane = image.planes[2].toYuvPlaneData()
                )
                val yuvToNv21Millis = elapsedMillis(yuvStartNanos)
                onFrame(
                    FrameInput(
                        width = image.width,
                        height = image.height,
                        rotationDegrees = image.imageInfo.rotationDegrees,
                        format = FrameFormat.NV21,
                        bytes = nv21Bytes,
                        performance = com.voiceguide.detection.PerformanceMetrics(
                            frameIntervalMillis = frameIntervalMillis,
                            yuvToNv21Millis = yuvToNv21Millis
                        )
                    )
                )
            }
        }
        image.close()
    }

    private fun ImageProxy.PlaneProxy.toYuvPlaneData(): YuvPlaneData {
        return YuvPlaneData(
            bytes = buffer.toByteArray(),
            rowStride = rowStride,
            pixelStride = pixelStride
        )
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        val duplicate = duplicate()
        duplicate.rewind()
        return ByteArray(duplicate.remaining()).also { duplicate.get(it) }
    }

    private fun elapsedMillis(startNanos: Long): Long {
        return (System.nanoTime() - startNanos) / 1_000_000L
    }
}
