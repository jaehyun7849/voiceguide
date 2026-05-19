# 실내 장애물 음성 안내 앱 MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Kotlin 네이티브 Android로 실내 전방 장애물을 음성과 진동으로 안내하는 MVP 앱을 만든다.

**Architecture:** 앱은 CameraX로 후면 카메라 프레임을 받고, 탐지기 인터페이스를 통해 객체 후보를 얻은 뒤, 장면 해석기와 알림 관리자가 안내할 메시지와 진동을 결정한다. Android 의존성이 적은 핵심 판단 로직은 순수 Kotlin 모듈처럼 테스트 가능하게 만들고, CameraX/TextToSpeech/Vibrator/TFLite 연결은 Android 계층에 둔다.

**Tech Stack:** Kotlin, Android Native, Gradle, CameraX, Android TextToSpeech, Android Vibrator, TensorFlow Lite, JUnit.

---

## 파일 구조

- Create: `settings.gradle.kts`  
  Gradle 프로젝트 이름과 `:app` 모듈을 정의한다.
- Create: `build.gradle.kts`  
  Android Gradle Plugin과 Kotlin 플러그인 버전을 한 곳에서 관리한다.
- Create: `app/build.gradle.kts`  
  Android 앱 설정, CameraX, TFLite, 테스트 의존성을 정의한다.
- Create: `app/src/main/AndroidManifest.xml`  
  카메라와 진동 권한, `MainActivity`를 선언한다.
- Create: `app/src/main/res/values/styles.xml`  
  전체 화면 앱 테마를 정의한다.
- Create: `app/src/main/java/com/voiceguide/MainActivity.kt`  
  전체 화면 카메라 UI, 권한 요청, 탭/길게 누르기, 앱 상태 연결을 담당한다.
- Create: `app/src/main/java/com/voiceguide/camera/CameraFrameAnalyzer.kt`  
  CameraX `ImageAnalysis`에서 1.5~2초마다 한 프레임만 분석하도록 제한한다.
- Create: `app/src/main/java/com/voiceguide/detection/ObstacleDetector.kt`  
  탐지기 교체를 위한 인터페이스와 탐지 결과 모델을 정의한다.
- Create: `app/src/main/java/com/voiceguide/detection/FakeObstacleDetector.kt`  
  실제 모델 연결 전 UI와 안내 흐름을 검증하기 위한 가짜 탐지기를 제공한다.
- Create: `app/src/main/java/com/voiceguide/detection/TfliteObstacleDetector.kt`  
  TensorFlow Lite 모델을 연결할 실제 탐지기 골격을 제공한다.
- Create: `app/src/main/java/com/voiceguide/domain/SceneInterpreter.kt`  
  탐지 결과를 왼쪽/정면/오른쪽, 보통/가까움/위험으로 변환한다.
- Create: `app/src/main/java/com/voiceguide/domain/AlertManager.kt`  
  안내 우선순위, 쿨다운, 위험도 상승 시 즉시 안내 여부를 결정한다.
- Create: `app/src/main/java/com/voiceguide/output/SpeechGuide.kt`  
  Android TextToSpeech로 한국어 안내 문장을 말한다.
- Create: `app/src/main/java/com/voiceguide/output/VibrationGuide.kt`  
  가까움/위험 상태에 맞는 진동 패턴을 실행한다.
- Create: `app/src/test/java/com/voiceguide/domain/SceneInterpreterTest.kt`  
  위치와 위험도 분류를 검증한다.
- Create: `app/src/test/java/com/voiceguide/domain/AlertManagerTest.kt`  
  우선순위, 쿨다운, 위험도 상승 안내를 검증한다.

---

### Task 1: Android 프로젝트 뼈대 만들기

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Gradle 설정 파일을 만든다**

`settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "IndoorObstacleVoiceGuide"
include(":app")
```

`build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
}
```

- [ ] **Step 2: 앱 모듈 Gradle 파일을 만든다**

`app/build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.voiceguide"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.voiceguide"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    val cameraxVersion = "1.4.0"

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.16.1")

    testImplementation("junit:junit:4.13.2")
}
```

- [ ] **Step 3: Android Manifest를 만든다**

`app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.VIBRATE" />

    <uses-feature
        android:name="android.hardware.camera"
        android:required="true" />

    <application
        android:allowBackup="false"
        android:label="실내 장애물 안내"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:screenOrientation="portrait">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 4: 최소 테마 파일을 만든다**

Create: `app/src/main/res/values/styles.xml`

```xml
<resources>
    <style name="AppTheme" parent="android:style/Theme.Material.NoActionBar">
        <item name="android:windowNoTitle">true</item>
        <item name="android:windowActionBar">false</item>
        <item name="android:windowFullscreen">true</item>
        <item name="android:windowLightStatusBar">false</item>
    </style>
</resources>
```

- [ ] **Step 5: 빌드가 되는지 확인한다**

Run: `./gradlew :app:assembleDebug`

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: 커밋한다**

```bash
git add settings.gradle.kts build.gradle.kts app/build.gradle.kts app/src/main/AndroidManifest.xml app/src/main/res/values/styles.xml
git commit -m "chore: scaffold Android app"
```

---

### Task 2: 탐지 결과 모델과 장면 해석기 만들기

**Files:**
- Create: `app/src/main/java/com/voiceguide/detection/ObstacleDetector.kt`
- Create: `app/src/main/java/com/voiceguide/domain/SceneInterpreter.kt`
- Create: `app/src/test/java/com/voiceguide/domain/SceneInterpreterTest.kt`

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`app/src/test/java/com/voiceguide/domain/SceneInterpreterTest.kt`:

```kotlin
package com.voiceguide.domain

import com.voiceguide.detection.BoundingBox
import com.voiceguide.detection.DetectedObstacle
import com.voiceguide.detection.ObstacleClass
import org.junit.Assert.assertEquals
import org.junit.Test

class SceneInterpreterTest {
    private val interpreter = SceneInterpreter()

    @Test
    fun `classifies horizontal position from bounding box center`() {
        val left = obstacle(0.05f, 0.2f)
        val front = obstacle(0.4f, 0.6f)
        val right = obstacle(0.8f, 0.95f)

        assertEquals(GuidancePosition.LEFT, interpreter.interpret(listOf(left)).first().position)
        assertEquals(GuidancePosition.FRONT, interpreter.interpret(listOf(front)).first().position)
        assertEquals(GuidancePosition.RIGHT, interpreter.interpret(listOf(right)).first().position)
    }

    @Test
    fun `marks centered large object as danger`() {
        val result = interpreter.interpret(listOf(obstacle(0.2f, 0.8f, top = 0.1f, bottom = 0.95f))).first()

        assertEquals(GuidancePosition.FRONT, result.position)
        assertEquals(RiskLevel.DANGER, result.riskLevel)
    }

    @Test
    fun `marks stairs as danger when confidence is high`() {
        val result = interpreter.interpret(
            listOf(obstacle(0.7f, 0.9f, obstacleClass = ObstacleClass.STAIRS, confidence = 0.82f))
        ).first()

        assertEquals(RiskLevel.DANGER, result.riskLevel)
    }

    private fun obstacle(
        left: Float,
        right: Float,
        top: Float = 0.2f,
        bottom: Float = 0.7f,
        obstacleClass: ObstacleClass = ObstacleClass.PERSON,
        confidence: Float = 0.9f
    ) = DetectedObstacle(
        obstacleClass = obstacleClass,
        confidence = confidence,
        boundingBox = BoundingBox(left = left, top = top, right = right, bottom = bottom)
    )
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests com.voiceguide.domain.SceneInterpreterTest`

Expected: FAIL with unresolved references for `SceneInterpreter`, `DetectedObstacle`, and related types.

- [ ] **Step 3: 탐지 결과 모델을 구현한다**

`app/src/main/java/com/voiceguide/detection/ObstacleDetector.kt`:

```kotlin
package com.voiceguide.detection

interface ObstacleDetector {
    fun detect(frame: FrameInput): List<DetectedObstacle>
}

data class FrameInput(
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val bytes: ByteArray
)

data class DetectedObstacle(
    val obstacleClass: ObstacleClass,
    val confidence: Float,
    val boundingBox: BoundingBox
)

data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val centerX: Float = (left + right) / 2f
    val width: Float = right - left
    val height: Float = bottom - top
    val area: Float = width * height
}

enum class ObstacleClass {
    PERSON,
    CHAIR,
    TABLE,
    DOOR,
    STAIRS
}
```

- [ ] **Step 4: 장면 해석기를 구현한다**

`app/src/main/java/com/voiceguide/domain/SceneInterpreter.kt`:

```kotlin
package com.voiceguide.domain

import com.voiceguide.detection.DetectedObstacle
import com.voiceguide.detection.ObstacleClass

class SceneInterpreter {
    fun interpret(detections: List<DetectedObstacle>): List<GuidanceFact> {
        return detections
            .filter { it.confidence >= 0.5f }
            .map { detection ->
                GuidanceFact(
                    obstacleClass = detection.obstacleClass,
                    position = positionOf(detection),
                    riskLevel = riskOf(detection),
                    confidence = detection.confidence
                )
            }
    }

    private fun positionOf(detection: DetectedObstacle): GuidancePosition {
        val centerX = detection.boundingBox.centerX
        return when {
            centerX < 0.33f -> GuidancePosition.LEFT
            centerX > 0.66f -> GuidancePosition.RIGHT
            else -> GuidancePosition.FRONT
        }
    }

    private fun riskOf(detection: DetectedObstacle): RiskLevel {
        val isFront = positionOf(detection) == GuidancePosition.FRONT
        val area = detection.boundingBox.area
        return when {
            detection.obstacleClass == ObstacleClass.STAIRS && detection.confidence >= 0.75f -> RiskLevel.DANGER
            isFront && area >= 0.45f -> RiskLevel.DANGER
            area >= 0.24f || isFront -> RiskLevel.NEAR
            else -> RiskLevel.NORMAL
        }
    }
}

data class GuidanceFact(
    val obstacleClass: ObstacleClass,
    val position: GuidancePosition,
    val riskLevel: RiskLevel,
    val confidence: Float
)

enum class GuidancePosition {
    LEFT,
    FRONT,
    RIGHT
}

enum class RiskLevel {
    NORMAL,
    NEAR,
    DANGER
}
```

- [ ] **Step 5: 테스트 통과를 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests com.voiceguide.domain.SceneInterpreterTest`

Expected: PASS

- [ ] **Step 6: 커밋한다**

```bash
git add app/src/main/java/com/voiceguide/detection/ObstacleDetector.kt app/src/main/java/com/voiceguide/domain/SceneInterpreter.kt app/src/test/java/com/voiceguide/domain/SceneInterpreterTest.kt
git commit -m "feat: interpret obstacle positions and risk"
```

---

### Task 3: 알림 우선순위와 쿨다운 구현하기

**Files:**
- Create: `app/src/main/java/com/voiceguide/domain/AlertManager.kt`
- Create: `app/src/test/java/com/voiceguide/domain/AlertManagerTest.kt`

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`app/src/test/java/com/voiceguide/domain/AlertManagerTest.kt`:

```kotlin
package com.voiceguide.domain

import com.voiceguide.detection.ObstacleClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlertManagerTest {
    @Test
    fun `chooses front danger before side near obstacle`() {
        val manager = AlertManager()
        val alert = manager.nextAlert(
            facts = listOf(
                fact(ObstacleClass.CHAIR, GuidancePosition.RIGHT, RiskLevel.NEAR),
                fact(ObstacleClass.PERSON, GuidancePosition.FRONT, RiskLevel.DANGER)
            ),
            nowMillis = 1_000L,
            forceRepeat = false
        )

        assertEquals("주의, 정면 가까이에 사람이 있습니다.", alert?.message)
        assertEquals(RiskLevel.DANGER, alert?.riskLevel)
    }

    @Test
    fun `suppresses repeated same alert within cooldown`() {
        val manager = AlertManager(cooldownMillis = 4_000L)
        val facts = listOf(fact(ObstacleClass.CHAIR, GuidancePosition.RIGHT, RiskLevel.NEAR))

        val first = manager.nextAlert(facts, nowMillis = 1_000L, forceRepeat = false)
        val second = manager.nextAlert(facts, nowMillis = 2_000L, forceRepeat = false)

        assertEquals("오른쪽 가까이에 의자가 있습니다.", first?.message)
        assertNull(second)
    }

    @Test
    fun `force repeat bypasses cooldown`() {
        val manager = AlertManager(cooldownMillis = 4_000L)
        val facts = listOf(fact(ObstacleClass.DOOR, GuidancePosition.FRONT, RiskLevel.NORMAL))

        manager.nextAlert(facts, nowMillis = 1_000L, forceRepeat = false)
        val repeated = manager.nextAlert(facts, nowMillis = 2_000L, forceRepeat = true)

        assertEquals("정면에 문이 있습니다.", repeated?.message)
    }

    private fun fact(
        obstacleClass: ObstacleClass,
        position: GuidancePosition,
        riskLevel: RiskLevel
    ) = GuidanceFact(
        obstacleClass = obstacleClass,
        position = position,
        riskLevel = riskLevel,
        confidence = 0.9f
    )
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests com.voiceguide.domain.AlertManagerTest`

Expected: FAIL with unresolved reference `AlertManager`.

- [ ] **Step 3: 알림 관리자를 구현한다**

`app/src/main/java/com/voiceguide/domain/AlertManager.kt`:

```kotlin
package com.voiceguide.domain

import com.voiceguide.detection.ObstacleClass

class AlertManager(
    private val cooldownMillis: Long = 4_000L
) {
    private var lastAlertKey: AlertKey? = null
    private var lastAlertRisk: RiskLevel? = null
    private var lastAlertMillis: Long = Long.MIN_VALUE

    fun nextAlert(
        facts: List<GuidanceFact>,
        nowMillis: Long,
        forceRepeat: Boolean
    ): GuidanceAlert? {
        val selected = facts.maxWithOrNull(compareBy<GuidanceFact> { priorityOf(it) }.thenBy { it.confidence })
            ?: return if (forceRepeat) GuidanceAlert("감지된 주요 장애물이 없습니다.", RiskLevel.NORMAL) else null

        val key = AlertKey(selected.obstacleClass, selected.position)
        val withinCooldown = key == lastAlertKey && nowMillis - lastAlertMillis < cooldownMillis
        val riskIncreased = lastAlertRisk != null && selected.riskLevel.ordinal > lastAlertRisk!!.ordinal

        if (!forceRepeat && withinCooldown && !riskIncreased) {
            return null
        }

        lastAlertKey = key
        lastAlertRisk = selected.riskLevel
        lastAlertMillis = nowMillis

        return GuidanceAlert(messageOf(selected), selected.riskLevel)
    }

    private fun priorityOf(fact: GuidanceFact): Int {
        return when {
            fact.obstacleClass == ObstacleClass.STAIRS && fact.riskLevel == RiskLevel.DANGER -> 600
            fact.position == GuidancePosition.FRONT && fact.obstacleClass == ObstacleClass.PERSON && fact.riskLevel != RiskLevel.NORMAL -> 500
            fact.position == GuidancePosition.FRONT && fact.riskLevel != RiskLevel.NORMAL -> 400
            fact.position == GuidancePosition.FRONT && fact.obstacleClass == ObstacleClass.DOOR -> 300
            fact.position != GuidancePosition.FRONT && fact.riskLevel != RiskLevel.NORMAL -> 200
            else -> 100
        }
    }

    private fun messageOf(fact: GuidanceFact): String {
        val position = when (fact.position) {
            GuidancePosition.LEFT -> "왼쪽"
            GuidancePosition.FRONT -> "정면"
            GuidancePosition.RIGHT -> "오른쪽"
        }
        val name = when (fact.obstacleClass) {
            ObstacleClass.PERSON -> "사람"
            ObstacleClass.CHAIR -> "의자"
            ObstacleClass.TABLE -> "책상"
            ObstacleClass.DOOR -> "문"
            ObstacleClass.STAIRS -> "계단"
        }
        val subject = when (fact.obstacleClass) {
            ObstacleClass.PERSON -> "사람이"
            ObstacleClass.CHAIR -> "의자가"
            ObstacleClass.TABLE -> "책상이"
            ObstacleClass.DOOR -> "문이"
            ObstacleClass.STAIRS -> "계단이"
        }

        return when {
            fact.obstacleClass == ObstacleClass.STAIRS && fact.riskLevel == RiskLevel.DANGER -> "주의, 앞쪽에 계단이 있습니다."
            fact.riskLevel == RiskLevel.DANGER -> "주의, $position 가까이에 $subject 있습니다."
            fact.riskLevel == RiskLevel.NEAR -> "$position 가까이에 $subject 있습니다."
            else -> "$position에 $subject 있습니다."
        }
    }
}

data class GuidanceAlert(
    val message: String,
    val riskLevel: RiskLevel
)

private data class AlertKey(
    val obstacleClass: ObstacleClass,
    val position: GuidancePosition
)
```

- [ ] **Step 4: 테스트 통과를 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests com.voiceguide.domain.AlertManagerTest`

Expected: PASS

- [ ] **Step 5: 커밋한다**

```bash
git add app/src/main/java/com/voiceguide/domain/AlertManager.kt app/src/test/java/com/voiceguide/domain/AlertManagerTest.kt
git commit -m "feat: choose obstacle voice alerts"
```

---

### Task 4: 전체 화면 카메라 UI와 권한 처리 만들기

**Files:**
- Create: `app/src/main/java/com/voiceguide/MainActivity.kt`

- [ ] **Step 1: MainActivity를 만든다**

`app/src/main/java/com/voiceguide/MainActivity.kt`:

```kotlin
package com.voiceguide

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var statusText: TextView
    private var guidancePaused = false

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        statusText.text = if (granted) "안내 중" else "카메라 권한이 필요합니다"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUi()
        requestPermissionIfNeeded()
    }

    private fun setupUi() {
        previewView = PreviewView(this)
        statusText = TextView(this).apply {
            text = "안내 중"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0x66000000)
            setPadding(24, 12, 24, 12)
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

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                guidancePaused = !guidancePaused
                statusText.text = if (guidancePaused) "일시정지" else "안내 중"
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                statusText.text = if (guidancePaused) "일시정지" else "현재 상황 다시 설명"
            }
        })

        root.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        setContentView(root)
    }

    private fun requestPermissionIfNeeded() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }
}
```

- [ ] **Step 2: 빌드가 되는지 확인한다**

Run: `./gradlew :app:assembleDebug`

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 커밋한다**

```bash
git add app/src/main/java/com/voiceguide/MainActivity.kt
git commit -m "feat: add full screen guidance UI"
```

---

### Task 5: CameraX 미리보기와 분석 주기 연결하기

**Files:**
- Modify: `app/src/main/java/com/voiceguide/MainActivity.kt`
- Create: `app/src/main/java/com/voiceguide/camera/CameraFrameAnalyzer.kt`
- Create: `app/src/main/java/com/voiceguide/detection/FakeObstacleDetector.kt`

- [ ] **Step 1: 가짜 탐지기를 만든다**

`app/src/main/java/com/voiceguide/detection/FakeObstacleDetector.kt`:

```kotlin
package com.voiceguide.detection

class FakeObstacleDetector : ObstacleDetector {
    override fun detect(frame: FrameInput): List<DetectedObstacle> {
        return listOf(
            DetectedObstacle(
                obstacleClass = ObstacleClass.PERSON,
                confidence = 0.9f,
                boundingBox = BoundingBox(left = 0.38f, top = 0.18f, right = 0.62f, bottom = 0.82f)
            )
        )
    }
}
```

- [ ] **Step 2: 분석 주기 제한기를 만든다**

`app/src/main/java/com/voiceguide/camera/CameraFrameAnalyzer.kt`:

```kotlin
package com.voiceguide.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.voiceguide.detection.FrameInput

class CameraFrameAnalyzer(
    private val intervalMillis: Long = 1_500L,
    private val onFrame: (FrameInput) -> Unit
) : ImageAnalysis.Analyzer {
    private var lastAnalyzedAt = 0L

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastAnalyzedAt >= intervalMillis) {
            lastAnalyzedAt = now
            onFrame(
                FrameInput(
                    width = image.width,
                    height = image.height,
                    rotationDegrees = image.imageInfo.rotationDegrees,
                    bytes = ByteArray(0)
                )
            )
        }
        image.close()
    }
}
```

- [ ] **Step 3: MainActivity에 CameraX를 연결한다**

Modify `MainActivity.kt` by adding fields:

```kotlin
private val detector = com.voiceguide.detection.FakeObstacleDetector()
private val sceneInterpreter = com.voiceguide.domain.SceneInterpreter()
private val alertManager = com.voiceguide.domain.AlertManager()
```

Add `startCamera()` after permission is granted:

```kotlin
private fun startCamera() {
    val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(this)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = androidx.camera.core.Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val analysis = androidx.camera.core.ImageAnalysis.Builder()
            .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(
                    ContextCompat.getMainExecutor(this),
                    com.voiceguide.camera.CameraFrameAnalyzer { frame ->
                        if (!guidancePaused) {
                            val facts = sceneInterpreter.interpret(detector.detect(frame))
                            val alert = alertManager.nextAlert(facts, System.currentTimeMillis(), forceRepeat = false)
                            if (alert != null) {
                                statusText.text = alert.message
                            }
                        }
                    }
                )
            }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            this,
            androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            analysis
        )
    }, ContextCompat.getMainExecutor(this))
}
```

Update `requestPermissionIfNeeded()` so granted permission starts the camera:

```kotlin
private fun requestPermissionIfNeeded() {
    val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    if (granted) {
        startCamera()
    } else {
        requestCameraPermission.launch(Manifest.permission.CAMERA)
    }
}
```

Update the permission callback:

```kotlin
private val requestCameraPermission = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
) { granted ->
    statusText.text = if (granted) "안내 중" else "카메라 권한이 필요합니다"
    if (granted) startCamera()
}
```

- [ ] **Step 4: 빌드가 되는지 확인한다**

Run: `./gradlew :app:assembleDebug`

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋한다**

```bash
git add app/src/main/java/com/voiceguide/MainActivity.kt app/src/main/java/com/voiceguide/camera/CameraFrameAnalyzer.kt app/src/main/java/com/voiceguide/detection/FakeObstacleDetector.kt
git commit -m "feat: connect CameraX analysis loop"
```

---

### Task 6: 음성 안내와 진동 연결하기

**Files:**
- Create: `app/src/main/java/com/voiceguide/output/SpeechGuide.kt`
- Create: `app/src/main/java/com/voiceguide/output/VibrationGuide.kt`
- Modify: `app/src/main/java/com/voiceguide/MainActivity.kt`

- [ ] **Step 1: 음성 안내 클래스를 만든다**

`app/src/main/java/com/voiceguide/output/SpeechGuide.kt`:

```kotlin
package com.voiceguide.output

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class SpeechGuide(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts?.language = Locale.KOREAN
        }
    }

    fun speak(message: String) {
        if (ready) {
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "voice-guide-alert")
        }
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
```

- [ ] **Step 2: 진동 클래스를 만든다**

`app/src/main/java/com/voiceguide/output/VibrationGuide.kt`:

```kotlin
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
```

- [ ] **Step 3: MainActivity에서 alert 발생 시 음성과 진동을 실행한다**

Add fields:

```kotlin
private lateinit var speechGuide: com.voiceguide.output.SpeechGuide
private lateinit var vibrationGuide: com.voiceguide.output.VibrationGuide
```

Initialize in `onCreate()` before permission check:

```kotlin
speechGuide = com.voiceguide.output.SpeechGuide(this)
vibrationGuide = com.voiceguide.output.VibrationGuide(this)
```

When an alert is produced:

```kotlin
if (alert != null) {
    statusText.text = alert.message
    speechGuide.speak(alert.message)
    vibrationGuide.vibrateFor(alert.riskLevel)
}
```

Add cleanup:

```kotlin
override fun onDestroy() {
    speechGuide.shutdown()
    super.onDestroy()
}
```

- [ ] **Step 4: 빌드가 되는지 확인한다**

Run: `./gradlew :app:assembleDebug`

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋한다**

```bash
git add app/src/main/java/com/voiceguide/MainActivity.kt app/src/main/java/com/voiceguide/output/SpeechGuide.kt app/src/main/java/com/voiceguide/output/VibrationGuide.kt
git commit -m "feat: speak and vibrate obstacle alerts"
```

---

### Task 7: 길게 누르기 현재 상황 다시 설명 연결하기

**Files:**
- Modify: `app/src/main/java/com/voiceguide/MainActivity.kt`

- [ ] **Step 1: MainActivity에 최근 장면 저장 필드를 추가한다**

```kotlin
private var latestFacts: List<com.voiceguide.domain.GuidanceFact> = emptyList()
```

- [ ] **Step 2: 분석 결과를 latestFacts에 저장한다**

Inside the analyzer callback:

```kotlin
latestFacts = sceneInterpreter.interpret(detector.detect(frame))
val alert = alertManager.nextAlert(latestFacts, System.currentTimeMillis(), forceRepeat = false)
```

- [ ] **Step 3: 길게 누르기에서 강제 반복 안내를 호출한다**

Replace `onLongPress` body:

```kotlin
override fun onLongPress(e: MotionEvent) {
    val alert = alertManager.nextAlert(latestFacts, System.currentTimeMillis(), forceRepeat = true)
    if (alert != null) {
        statusText.text = alert.message
        speechGuide.speak(alert.message)
        vibrationGuide.vibrateFor(alert.riskLevel)
    }
}
```

- [ ] **Step 4: 빌드가 되는지 확인한다**

Run: `./gradlew :app:assembleDebug`

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋한다**

```bash
git add app/src/main/java/com/voiceguide/MainActivity.kt
git commit -m "feat: repeat current scene on long press"
```

---

### Task 8: TensorFlow Lite 탐지기 골격 추가하기

**Files:**
- Create: `app/src/main/java/com/voiceguide/detection/TfliteObstacleDetector.kt`
- Modify: `app/src/main/java/com/voiceguide/MainActivity.kt`

- [ ] **Step 1: TFLite 탐지기 골격을 만든다**

`app/src/main/java/com/voiceguide/detection/TfliteObstacleDetector.kt`:

```kotlin
package com.voiceguide.detection

import android.content.Context
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.tensorflow.lite.Interpreter

class TfliteObstacleDetector(
    context: Context,
    modelAssetName: String = "obstacle_detector.tflite"
) : ObstacleDetector {
    private val interpreter: Interpreter

    init {
        val modelBytes = context.assets.open(modelAssetName).use { input ->
            input.readBytes()
        }
        val modelBuffer = ByteBuffer.allocateDirect(modelBytes.size)
            .order(ByteOrder.nativeOrder())
            .put(modelBytes)
        modelBuffer.rewind()
        interpreter = Interpreter(modelBuffer)
    }

    override fun detect(frame: FrameInput): List<DetectedObstacle> {
        return emptyList()
    }

    fun close() {
        interpreter.close()
    }
}
```

- [ ] **Step 2: 실제 모델이 없을 때는 FakeObstacleDetector를 유지한다**

Keep this field in `MainActivity.kt` until `app/src/main/assets/obstacle_detector.tflite` exists:

```kotlin
private val detector = com.voiceguide.detection.FakeObstacleDetector()
```

- [ ] **Step 3: 빌드가 되는지 확인한다**

Run: `./gradlew :app:assembleDebug`

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: 커밋한다**

```bash
git add app/src/main/java/com/voiceguide/detection/TfliteObstacleDetector.kt app/src/main/java/com/voiceguide/MainActivity.kt
git commit -m "feat: add TensorFlow Lite detector shell"
```

---

### Task 9: 실제 기기에서 MVP 흐름 확인하기

**Files:**
- Modify only if manual testing reveals a build or runtime issue.

- [ ] **Step 1: 디버그 APK를 빌드한다**

Run: `./gradlew :app:assembleDebug`

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: 연결된 Android 기기를 확인한다**

Run: `adb devices`

Expected: at least one device in `device` state.

- [ ] **Step 3: 앱을 설치한다**

Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

Expected: `Success`

- [ ] **Step 4: 앱을 실행한다**

Run: `adb shell am start -n com.voiceguide/.MainActivity`

Expected: app opens full screen and requests camera permission if needed.

- [ ] **Step 5: 수동 확인한다**

Expected results:

- 카메라 권한 허용 후 후면 카메라 미리보기가 보인다.
- 상태 텍스트가 "안내 중"으로 보인다.
- Fake detector 때문에 약 1.5초 뒤 "주의, 정면 가까이에 사람이 있습니다." 또는 정면 사람 안내가 표시되고 음성으로 나온다.
- 가까움/위험 안내에서 진동이 발생한다.
- 화면을 탭하면 "일시정지"가 표시되고 안내가 멈춘다.
- 다시 탭하면 "안내 중" 상태로 돌아간다.
- 길게 누르면 현재 상황 설명이 다시 나온다.

- [ ] **Step 6: 커밋한다**

```bash
git status --short
git add app/src/main/java app/src/main/AndroidManifest.xml app/build.gradle.kts
git commit -m "fix: polish device guidance flow"
```

Commit only if files changed during manual testing. If no files changed, do not create an empty commit.

---

## 자체 검토

설계 문서의 주요 요구사항은 이 계획에 모두 연결되어 있다.

- Kotlin 네이티브 Android 프로젝트: Task 1
- 후면 카메라 미리보기와 프레임 분석: Task 4, Task 5
- 1.5~2초 분석 주기: Task 5
- 객체 탐지 교체 가능 구조: Task 2, Task 5, Task 8
- 위치/위험도 판단: Task 2
- 자연스러운 한국어 안내와 위험 시 "주의": Task 3, Task 6
- 짧은/긴 진동: Task 6
- 탭 일시정지/재시작: Task 4, Task 5
- 길게 누르기 현재 상황 다시 설명: Task 7
- 실제 기기 검증: Task 9

계획에는 의도적인 유보가 하나 있다. 실제 `.tflite` 모델 파일과 입출력 후처리는 모델 선택 뒤에 구현한다. 이 MVP 구현 계획은 먼저 앱 구조와 안내 흐름을 완성하고, 모델 연결 지점을 `TfliteObstacleDetector`로 고정하는 데 초점을 둔다.
