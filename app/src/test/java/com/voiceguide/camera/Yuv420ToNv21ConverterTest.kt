package com.voiceguide.camera

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class Yuv420ToNv21ConverterTest {
    @Test
    fun `converts compact yuv420 planes to nv21`() {
        val y = byteArrayOf(10, 11, 12, 13, 14, 15, 16, 17)
        val u = byteArrayOf(20, 21)
        val v = byteArrayOf(30, 31)

        val nv21 = Yuv420ToNv21Converter.convert(
            width = 4,
            height = 2,
            yPlane = YuvPlaneData(y, rowStride = 4, pixelStride = 1),
            uPlane = YuvPlaneData(u, rowStride = 2, pixelStride = 1),
            vPlane = YuvPlaneData(v, rowStride = 2, pixelStride = 1)
        )

        assertArrayEquals(
            byteArrayOf(10, 11, 12, 13, 14, 15, 16, 17, 30, 20, 31, 21),
            nv21
        )
    }

    @Test
    fun `honors row stride and pixel stride while converting chroma planes`() {
        val y = byteArrayOf(
            1, 2, 3, 4, 99, 99,
            5, 6, 7, 8, 99, 99
        )
        val u = byteArrayOf(40, 99, 41, 99)
        val v = byteArrayOf(50, 99, 51, 99)

        val nv21 = Yuv420ToNv21Converter.convert(
            width = 4,
            height = 2,
            yPlane = YuvPlaneData(y, rowStride = 6, pixelStride = 1),
            uPlane = YuvPlaneData(u, rowStride = 4, pixelStride = 2),
            vPlane = YuvPlaneData(v, rowStride = 4, pixelStride = 2)
        )

        assertArrayEquals(
            byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 50, 40, 51, 41),
            nv21
        )
    }
}
