package com.voiceguide.detection

import org.junit.Assert.assertEquals
import org.junit.Test

class DetectorModeTest {
    @Test
    fun `debug builds use fake detector by default`() {
        assertEquals(
            DetectorMode.FAKE,
            DetectorMode.resolve(isDebug = true, hasModelAsset = true, forceRealDetector = false)
        )
    }

    @Test
    fun `release builds use tflite when the model asset exists`() {
        assertEquals(
            DetectorMode.TFLITE,
            DetectorMode.resolve(isDebug = false, hasModelAsset = true, forceRealDetector = false)
        )
    }

    @Test
    fun `missing model asset falls back to fake detector`() {
        assertEquals(
            DetectorMode.FAKE,
            DetectorMode.resolve(isDebug = false, hasModelAsset = false, forceRealDetector = false)
        )
    }

    @Test
    fun `force flag uses tflite in debug when the model asset exists`() {
        assertEquals(
            DetectorMode.TFLITE,
            DetectorMode.resolve(isDebug = true, hasModelAsset = true, forceRealDetector = true)
        )
    }
}
