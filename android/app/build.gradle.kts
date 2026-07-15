import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
}

// ── Read local.properties at top-level (accessible in all blocks below) ──────
// Set these in local.properties (do NOT commit this file to git):
//   KEYSTORE_PATH=../classflow-release-key.jks
//   KEYSTORE_PASSWORD=your_store_password
//   KEY_ALIAS=classflow
//   KEY_PASSWORD=your_key_password
//
// Generate keystore once:
//   keytool -genkey -v -keystore classflow-release-key.jks \
//     -keyalg RSA -keysize 2048 -validity 10000 -alias classflow
val keystoreProps = Properties()
rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { keystoreProps.load(it) }
val hasKeystore = keystoreProps.getProperty("KEYSTORE_PATH") != null

android {
    namespace = "com.anish18.classflow"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.anish18.classflow"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    if (hasKeystore) {
        signingConfigs {
            create("release") {
                storeFile = file(keystoreProps.getProperty("KEYSTORE_PATH"))
                storePassword = keystoreProps.getProperty("KEYSTORE_PASSWORD")
                keyAlias = keystoreProps.getProperty("KEY_ALIAS")
                keyPassword = keystoreProps.getProperty("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Use release keystore for Play Store uploads; debug keystore for local testing
            signingConfig = if (hasKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}


dependencies {
    // Core AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // Lifecycle Viewmodel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Coil Image Loading (Useful for backgrounds/wallpapers)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // JSON Serialization
    implementation("com.google.code.gson:gson:2.10.1")

    // QR Code generation and decoding
    implementation("com.google.zxing:core:3.5.3")

    // Realtime backdrop blur library (Haze)
    implementation("dev.chrisbanes.haze:haze:0.6.0")

    // Google Code Scanner for live QR scanning
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
