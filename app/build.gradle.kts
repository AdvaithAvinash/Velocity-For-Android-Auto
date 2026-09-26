plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.velocity.auto"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.velocity.auto"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources.excludes.add("META-INF/**")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.webkit:webkit:1.11.0")

    // Playback engine for the native, lightweight YouTube player.
    implementation("androidx.media3:media3-exoplayer:1.4.0")
    implementation("androidx.media3:media3-ui:1.4.0")

    // MediaSession wiring - hardware media buttons, steering-wheel/Bluetooth
    // play-pause, and lock-screen controls, all driven by the same player.
    implementation("androidx.media3:media3-session:1.4.0")

    // Shared OkHttp-backed data source + on-disk playback cache, so
    // switching videos and replays reuse pooled connections instead of
    // paying a fresh TLS handshake/full re-download every time.
    implementation("androidx.media3:media3-datasource-okhttp:1.4.0")

    // Direct-link streams from addons are often HLS (.m3u8), not plain mp4.
    implementation("androidx.media3:media3-exoplayer-hls:1.4.0")

    // The actual in-car surface (home grid, search, video) rendered on the
    // Android Auto head unit screen once an unlocker like AAEnabler lets
    // Velocity through.
    implementation("androidx.car.app:app:1.4.0")

    // NewPipeExtractor powers YouTube search + stream resolution without the
    // heavy official YouTube app/website - this is what keeps Velocity fast.
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.24.4")

    // Backs the Downloader NewPipeExtractor needs, and general networking.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Lightweight image loading for thumbnails / custom app icons.
    implementation("io.coil-kt:coil:2.6.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
}
