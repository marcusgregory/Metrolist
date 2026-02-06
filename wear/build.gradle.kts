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

    buildTypes {
        release {
            isMinifyEnabled = true
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
    ksp(libs.hilt.compiler)
    // Fix for Kotlin 2.3.0 + Hilt compatibility
    ksp("org.jetbrains.kotlin:kotlin-metadata-jvm:2.3.0")

    // Compose for Wear OS
    implementation("androidx.wear.compose:compose-material3:1.0.0-alpha34")
    implementation("androidx.wear.compose:compose-foundation:1.5.0")
    implementation("androidx.wear.compose:compose-navigation:1.5.0")
    implementation(libs.activity)

    // Horologist (for Phase 2 - Player UI)
    // TODO: Add horologist-compose-layout and horologist-audio-ui in Phase 2

    // Utils
    implementation(libs.timber)

    // Desugaring
    coreLibraryDesugaring(libs.desugaring)
}
