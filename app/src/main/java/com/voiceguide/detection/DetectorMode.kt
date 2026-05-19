package com.voiceguide.detection

enum class DetectorMode {
    FAKE,
    TFLITE;

    companion object {
        fun resolve(
            isDebug: Boolean,
            hasModelAsset: Boolean,
            forceRealDetector: Boolean
        ): DetectorMode {
            if (!hasModelAsset) return FAKE
            return if (!isDebug || forceRealDetector) TFLITE else FAKE
        }
    }
}
