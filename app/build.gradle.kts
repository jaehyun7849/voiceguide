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
        versionCode = 3
        versionName = "0.1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            val useTfliteDetector = providers.gradleProperty("voiceguide.useTfliteDetector")
                .orElse("false")
                .get()
                .toBoolean()
            buildConfigField("boolean", "USE_TFLITE_DETECTOR", useTfliteDetector.toString())
        }
        release {
            buildConfigField("boolean", "USE_TFLITE_DETECTOR", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
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
