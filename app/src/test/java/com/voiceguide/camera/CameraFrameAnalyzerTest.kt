package com.voiceguide.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraFrameAnalyzerTest {
    @Test
    fun `defaults to two analysis frames per second`() {
        assertEquals(500L, CameraFrameAnalyzer.DEFAULT_INTERVAL_MILLIS)
    }
}
