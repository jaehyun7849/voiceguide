package com.voiceguide.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.voiceguide.detection.DetectedObstacle
import java.util.Locale

class DetectionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 220, 140)
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private val labelBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(210, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    private val labelBounds = android.graphics.Rect()
    private var detections: List<DetectedObstacle> = emptyList()

    fun setDetections(nextDetections: List<DetectedObstacle>) {
        detections = nextDetections
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        detections.forEach { detection ->
            val box = detection.boundingBox
            val rect = RectF(
                box.left * width,
                box.top * height,
                box.right * width,
                box.bottom * height
            )
            canvas.drawRect(rect, boxPaint)
            drawLabel(canvas, rect, detection)
        }
    }

    private fun drawLabel(canvas: Canvas, rect: RectF, detection: DetectedObstacle) {
        val label = "${detection.obstacleClass.name} ${formatConfidence(detection.confidence)}"
        labelTextPaint.getTextBounds(label, 0, label.length, labelBounds)
        val paddingX = 14f
        val paddingY = 8f
        val labelHeight = labelBounds.height() + paddingY * 2
        val labelWidth = labelBounds.width() + paddingX * 2
        val left = rect.left.coerceAtMost(width - labelWidth).coerceAtLeast(0f)
        val top = (rect.top - labelHeight).takeIf { it >= 0f } ?: rect.top
        canvas.drawRoundRect(
            left,
            top,
            left + labelWidth,
            top + labelHeight,
            6f,
            6f,
            labelBackgroundPaint
        )
        canvas.drawText(label, left + paddingX, top + labelHeight - paddingY, labelTextPaint)
    }

    private fun formatConfidence(confidence: Float): String {
        return String.format(Locale.US, "%.0f%%", confidence * 100f)
    }
}
