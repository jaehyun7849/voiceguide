package com.voiceguide

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.voiceguide.camera.CameraFrameAnalyzer
import com.voiceguide.detection.DetectorFactory
import com.voiceguide.detection.ObstacleDetector
import com.voiceguide.detection.PerformanceOverlayFormatter
import com.voiceguide.domain.AlertManager
import com.voiceguide.domain.GuidanceFact
import com.voiceguide.domain.SceneInterpreter
import com.voiceguide.output.SpeechGuide
import com.voiceguide.output.VibrationGuide
import com.voiceguide.ui.DetectionOverlayView
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var detectionOverlayView: DetectionOverlayView
    private lateinit var statusText: TextView
    private lateinit var performanceText: TextView
    private lateinit var speechGuide: SpeechGuide
    private lateinit var vibrationGuide: VibrationGuide
    private lateinit var detector: ObstacleDetector
    private var guidancePaused = false
    @Volatile
    private var latestFacts: List<GuidanceFact> = emptyList()
    private val sceneInterpreter = SceneInterpreter()
    private val alertManager = AlertManager()
    private val cameraAnalysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        statusText.text = if (granted) "안내 중" else "카메라 권한이 필요합니다"
        if (granted) startCamera()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speechGuide = SpeechGuide(this)
        vibrationGuide = VibrationGuide(this)
        detector = DetectorFactory.create(this)
        setupUi()
        requestPermissionIfNeeded()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUi() {
        previewView = PreviewView(this)
        detectionOverlayView = DetectionOverlayView(this)
        statusText = TextView(this).apply {
            text = "안내 중"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0x66000000)
            setPadding(24, 12, 24, 12)
        }
        performanceText = TextView(this).apply {
            text = PerformanceOverlayFormatter.format(com.voiceguide.detection.PerformanceMetrics())
            textSize = 12f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0x66000000)
            setPadding(18, 12, 18, 12)
        }

        val root = FrameLayout(this)
        root.addView(
            previewView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        root.addView(
            detectionOverlayView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        root.addView(
            statusText,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = 24
                bottomMargin = 48
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.START
            }
        )
        root.addView(
            performanceText,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = 24
                topMargin = 48
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
            }
        )

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                guidancePaused = !guidancePaused
                statusText.text = if (guidancePaused) "일시정지" else "안내 중"
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                val alert = alertManager.nextAlert(
                    facts = latestFacts,
                    nowMillis = System.currentTimeMillis(),
                    forceRepeat = true
                )
                if (alert != null) {
                    deliverAlert(alert)
                } else {
                    statusText.text = if (guidancePaused) "일시정지" else "현재 상황 다시 설명"
                }
            }
        })

        root.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        setContentView(root)
    }

    private fun requestPermissionIfNeeded() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            this.cameraProvider = cameraProvider
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(
                        cameraAnalysisExecutor,
                        CameraFrameAnalyzer { frame ->
                            if (!guidancePaused) {
                                handleFrame(frame)
                            }
                        }
                    )
                }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )
        }, ContextCompat.getMainExecutor(this))
    }

    private fun handleFrame(frame: com.voiceguide.detection.FrameInput) {
        val result = detector.detect(frame)
        latestFacts = sceneInterpreter.interpret(result.obstacles)
        val alert = alertManager.nextAlert(
            facts = latestFacts,
            nowMillis = System.currentTimeMillis(),
            forceRepeat = false
        )
        runOnUiThread {
            detectionOverlayView.setDetections(result.obstacles)
            performanceText.text = PerformanceOverlayFormatter.format(result.metrics)
            if (alert != null) {
                deliverAlert(alert)
            } else if (result.obstacles.isEmpty()) {
                statusText.text = if (guidancePaused) "일시정지" else "감지된 주요 장애물이 없습니다."
            }
        }
    }

    private fun deliverAlert(alert: com.voiceguide.domain.GuidanceAlert) {
        statusText.text = alert.message
        speechGuide.speak(alert.message)
        vibrationGuide.vibrateFor(alert.riskLevel)
    }

    override fun onDestroy() {
        cameraProvider?.unbindAll()
        cameraAnalysisExecutor.shutdown()
        cameraAnalysisExecutor.awaitTermination(2, TimeUnit.SECONDS)
        detector.close()
        speechGuide.shutdown()
        super.onDestroy()
    }
}
