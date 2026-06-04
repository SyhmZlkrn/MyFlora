import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use(::load)
    }
}

fun localProperty(name: String): String {
    return providers.gradleProperty(name)
        .orElse(localProperties.getProperty(name, ""))
        .get()
}

android {
    namespace = "com.example.flora"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.flora"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "OPENWEATHER_API_KEY", "\"${localProperty("OPENWEATHER_API_KEY")}\"")
        buildConfigField("String", "ROBOFLOW_API_KEY", "\"${localProperty("ROBOFLOW_API_KEY")}\"")
        buildConfigField("String", "PLANTNET_API_KEY", "\"${localProperty("PLANTNET_API_KEY")}\"")
        // Web3Forms — handles the in-app Help & Support form. Sign up at web3forms.com,
        // verify support@myfloraapp.com, paste the access key into local.properties.
        buildConfigField("String", "WEB3FORMS_ACCESS_KEY", "\"${localProperty("WEB3FORMS_ACCESS_KEY")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        noCompress += "tflite"
    }
}

ksp {
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // CameraX
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    // Location
    implementation(libs.play.services.location)
    // Image loading
    implementation(libs.coil.compose)
    // On-device TFLite (YOLOv8 detector trained on Roboflow Malaysian Flower Detection v2)
    implementation(libs.tensorflow.lite)
    // Background reminders
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
