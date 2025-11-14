plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.sotugyo_kenkyu"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.sotugyo_kenkyu"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //BOM Firebase利用に必要
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))

    // Firebase Authentication 認証に必要
    implementation("com.google.firebase:firebase-auth")

    // Cloud Firestore DB利用に必要
    implementation("com.google.firebase:firebase-firestore")

    // Cloud Storage for Firebase 画像取り扱いに必要
    implementation("com.google.firebase:firebase-storage")
    implementation("com.firebaseui:firebase-ui-storage:8.0.0")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    kapt("com.github.bumptech.glide:compiler:4.15.1")

    // Cloud Functions for Firebase 外部APIとの接続等に必要
    implementation("com.google.firebase:firebase-functions")

    // Firebase AILogic AIチャット機能実装に必要
    implementation("com.google.firebase:firebase-ai")
}