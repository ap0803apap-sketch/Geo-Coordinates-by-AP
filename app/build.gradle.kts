plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.gps.locationtracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gps.locationtracker"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.2.0(12-03-26)"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Vector drawable support
        vectorDrawables.useSupportLibrary = true
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        dataBinding = true
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = false
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/proguard/androidx-*.pro"
        }
    }

    lint {
        disable += "MissingTranslationDialog"
        disable += "ExtraTranslation"
        abortOnError = false
    }
}

dependencies {

    // ==================== CORE ANDROID ====================
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation("androidx.fragment:fragment-ktx:1.7.1")
    implementation(libs.androidx.activity.compose)

    // ==================== UI & MATERIAL ====================
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // Compose BOM and libraries
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.foundation:foundation:1.6.8")
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // ==================== LIFECYCLE & ARCHITECTURE ====================
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-process:2.8.0")

    // ==================== COROUTINES ====================
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")

    // ==================== ROOM DATABASE ====================
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ==================== DATA PERSISTENCE ====================
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation(libs.androidx.datastore.preferences)

    // EncryptedSharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ==================== GOOGLE SERVICES ====================
    implementation(libs.gms.auth)
    implementation("com.google.android.gms:play-services-auth-api-phone:18.0.0")
    implementation(libs.gms.location)
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // ==================== BIOMETRIC AUTHENTICATION ====================
    implementation(libs.biometric)

    // ==================== NETWORKING ====================
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // ==================== JSON SERIALIZATION ====================
    implementation(libs.gson)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // ==================== IMAGE LOADING & PROCESSING ====================
    implementation(libs.glide)
    ksp(libs.glide.compiler)

    // ==================== LOGGING ====================
    implementation(libs.timber)

    // ==================== WORK SCHEDULING ====================
    implementation(libs.androidx.work.runtime)

    // ==================== PERMISSIONS ====================
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    implementation("pub.devrel:easypermissions:3.0.0")

    // ==================== TESTING ====================
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
