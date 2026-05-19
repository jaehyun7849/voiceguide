package com.voiceguide.output

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.voiceguide.domain.RiskLevel

class VibrationGuide(context: Context) {
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun vibrateFor(riskLevel: RiskLevel) {
        val duration = when (riskLevel) {
            RiskLevel.NORMAL -> 0L
            RiskLevel.NEAR -> 120L
            RiskLevel.DANGER -> 450L
        }
        if (duration <= 0L) return
        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
