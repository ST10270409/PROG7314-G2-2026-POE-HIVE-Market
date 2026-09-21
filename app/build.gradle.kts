plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("kotlin-kapt")
}

android {
    namespace = "com.hivemarket.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hivemarket.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-prototype"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // NOTE: as of Kotlin 2.0+, the Compose compiler is a standalone Kotlin
    // compiler plugin (org.jetbrains.kotlin.plugin.compose, applied above)
    // rather than something AGP bundles — this composeOptions block is kept
    // only for older-AGP compatibility and has no effect under the plugin
    // versions pinned in the top-level build.gradle.kts. Safe to delete if
    // Android Studio flags it as unused.
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Core / Compose
    implementation("androidx.core:core-ktx:1.13.1")
    // AppCompatDelegate.setApplicationLocales is the standard per-app
    // language mechanism (FR6) — it works without an AppCompatActivity base
    // class, so it's safe to add alongside a pure-Compose UI.
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // Hilt (dependency injection)
    implementation("com.google.dagger:hilt-android:2.57")
    kapt("com.google.dagger:hilt-android-compiler:2.57")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Dagger/Hilt bundles its own reader for Kotlin's metadata format, and
    // that reader lags behind whatever Kotlin version is actually producing
    // the metadata — this is what caused:
    //   "[Hilt] Provided Metadata instance has version 2.3.0, while maximum
    //    supported version is 2.2.0."
    // Since Dagger 2.57, that reader is no longer bundled/hidden, so a
    // newer copy can be added directly here to override it. If this error
    // recurs with a different version number after a future Kotlin bump,
    // bump the version below to match — it should equal (or exceed) the
    // version number the error message reports as "Provided".
    kapt("org.jetbrains.kotlin:kotlin-metadata-jvm:2.3.0-Beta1")

    // Firebase (BoM manages versions)
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Networking — Retrofit + OkHttp + kotlinx.serialization
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Room (local persistence / offline cache — FR4/FR9)
    // NOTE: Room bundles its own internal ("jarjarred"/relocated) copy of
    // the kotlinx-metadata-jvm reader for annotation processing, separate
    // from Dagger's. Unlike Dagger, Room's copy can't be overridden with an
    // extra dependency (it's relocated into androidx.room.jarjarred.*, not
    // just shaded) — the only fix when it falls behind Kotlin's metadata
    // format is bumping Room itself. 2.6.1 hit exactly this wall:
    //   "Provided Metadata instance has version 2.2.0, while maximum
    //    supported version is 2.0.0."
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    kapt("androidx.room:room-compiler:2.8.4")

    // WorkManager (background sync — FR4)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Biometric (FR3)
    implementation("androidx.biometric:biometric-ktx:1.2.0-alpha05")

    // Coil (image loading for listing photos)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation("io.mockk:mockk:1.13.11")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
