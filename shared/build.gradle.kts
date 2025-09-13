plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    // ensure Kotlin compiles to JVM 17 for Android target
    jvmToolchain(17)

    androidTarget {
        compilations.all {
            compilerOptions.configure {
                // Kotlin JVM target
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            }
        }
    }

    sourceSets {
        val commonMain by getting
        val commonTest by getting
        val androidMain by getting
    }
}

android {
    namespace = "com.qandil.airesume.shared"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
    // align Java source/target to 17 as well
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
