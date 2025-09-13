plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.qandil.airesumeanalyser"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.qandil.airesumeanalyser"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // align Kotlin toolchain for this module too
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures { compose = true }
    // ❌ remove composeOptions { kotlinCompilerExtensionVersion = "..." }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(project(":shared"))

    // AndroidX / Compose (keep what you already have)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    // ✅ Unit tests
    testImplementation(libs.junit)

    // ✅ Instrumentation tests (keeps AGP happy even if you don’t run them)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // PDF text extraction
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")



}
