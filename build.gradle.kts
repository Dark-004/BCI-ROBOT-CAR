// Top-level build.gradle.kts
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false

    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
