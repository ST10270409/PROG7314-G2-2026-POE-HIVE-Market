// Top-level build file where you can add configuration options common to all sub-projects/modules.
//
// NOTE (Sept 2026): originally pinned to AGP 8.6.1 / Kotlin 1.9.24, which
// only supports Gradle 8.x. If your local Android Studio auto-generated a
// Gradle 9.x wrapper (check gradle/wrapper/gradle-wrapper.properties), you
// MUST bump the versions below to match — AGP 8.x cannot run on Gradle 9.x,
// and Gradle 9.x separately requires Kotlin Gradle Plugin >= 2.0.0.
//
// AGP version is capped by what your installed Android Studio release
// supports, NOT just by Gradle compatibility — if Android Studio reports
// "incompatible version of AGP, latest supported is X", set the version
// below to X regardless of what Gradle itself would otherwise accept.
//
// Safest fix: open this project in Android Studio and run
// Tools > AGP Upgrade Assistant — it picks exact compatible version numbers
// for your installed Gradle AND Android Studio release automatically. The
// versions below are a manual fallback; treat them as a floor, not a
// guarantee of the latest patch release.
plugins {
    id("com.android.application") version "9.3.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.google.dagger.hilt.android") version "2.57" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0" apply false
}
