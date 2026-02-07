plugins {
    id("com.android.application")
    kotlin("android")
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.metrolist.music.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.metrolist.music.wear"
        minSdk = 30  // Wear OS 3.0+
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        getByName("debug") {
            // Uses default debug keystore
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Projeto
    implementation(project(":innertube"))

    // Guava & Coroutines
    implementation(libs.guava)
    implementation(libs.coroutines.guava)

    // Media3
    implementation(libs.media3)
    implementation(libs.media3.session)
    implementation(libs.media3.okhttp)

    // Hilt
    implementation(libs.hilt)
    implementation(libs.hilt.navigation)
    ksp(libs.hilt.compiler)
    // Fix for Kotlin 2.3.0 + Hilt compatibility
    ksp("org.jetbrains.kotlin:kotlin-metadata-jvm:2.3.0")

    // Compose for Wear OS
    implementation("androidx.wear.compose:compose-material3:1.0.0-alpha34")
    implementation("androidx.wear.compose:compose-foundation:1.5.0")
    implementation("androidx.wear.compose:compose-navigation:1.5.0")
    implementation("androidx.wear:wear-input:1.2.0-alpha02")
    implementation(libs.activity)

    // Images
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)

    // Lifecycle
    implementation(libs.viewmodel.compose)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")

    // Utils
    implementation(libs.timber)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Horologist - Wear OS utilities (audio UI + ambient mode + media3 backend)
    implementation("com.google.android.horologist:horologist-audio-ui:0.6.22")
    implementation("com.google.android.horologist:horologist-audio:0.6.22")
    implementation("com.google.android.horologist:horologist-compose-layout:0.6.22")
    implementation("com.google.android.horologist:horologist-media3-backend:0.6.22")

    // Wear Ambient Mode
    implementation("androidx.wear:wear:1.3.0")

    // Desugaring
    coreLibraryDesugaring(libs.desugaring)
}
