plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("kotlin-parcelize")
    alias(libs.plugins.compose.compiler)

}

android {
    namespace = "com.neurodrive.smartcar"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.neurodrive.smartcar"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "APP_NAME", "\"NeuroDrive\"")
        buildConfigField("boolean", "ENABLE_LOGGING", "true")
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/NOTICE.md",
                "META-INF/LICENSE.md",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/NOTICE.txt",
                "META-INF/LICENSE.txt",
                "META-INF/DEPENDENCIES",
                "META-INF/INDEX.LIST",
                "META-INF/ASL2.0",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/*.kotlin_module",
                "META-INF/mailcap",
                "META-INF/mimetypes.default",
                "META-INF/gfprobe-provider.xml"
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    kapt {
        correctErrorTypes = true
        useBuildCache = true

        arguments {
            arg("dagger.fastInit", "enabled")
            arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")


    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")





    // Room (for local caching)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.biometric:biometric:1.1.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // ══════════════════════════════════════════════
    //  Firebase BOM (Bill of Materials)
    // ══════════════════════════════════════════════
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth-ktx")

    // Firebase Firestore
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Firebase Realtime Database (optional)
    implementation("com.google.firebase:firebase-database-ktx")

    // Firebase Storage (for profile pictures, etc.)
    implementation("com.google.firebase:firebase-storage-ktx")

    // Firebase Analytics (optional)
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Firebase Crashlytics (optional)
    implementation("com.google.firebase:firebase-crashlytics-ktx")

    // Google Play Services Auth (for Google Sign-In)
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.google.android.gms:play-services-base:18.5.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    // Coil for image loading in Compose
    implementation("io.coil-kt:coil-compose:2.5.0")
    // ✅ JavaMail for sending emails from Android
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
}

kapt {
    correctErrorTypes = true
}