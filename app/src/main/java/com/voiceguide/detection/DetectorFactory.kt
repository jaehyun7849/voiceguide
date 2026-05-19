package com.voiceguide.detection

import android.content.Context
import com.voiceguide.BuildConfig

object DetectorFactory {
    private const val MODEL_ASSET_NAME = "obstacle_detector.tflite"

    fun create(
        context: Context,
        isDebug: Boolean = BuildConfig.DEBUG,
        forceRealDetector: Boolean = BuildConfig.USE_TFLITE_DETECTOR,
        modelAssetName: String = MODEL_ASSET_NAME
    ): ObstacleDetector {
        val hasModelAsset = context.assets.list("")?.contains(modelAssetName) == true
        return when (
            DetectorMode.resolve(
                isDebug = isDebug,
                hasModelAsset = hasModelAsset,
                forceRealDetector = forceRealDetector
            )
        ) {
            DetectorMode.FAKE -> FakeObstacleDetector()
            DetectorMode.TFLITE -> TfliteObstacleDetector(context, modelAssetName)
        }
    }
}
