import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("kotlin-kapt")
    kotlin("plugin.serialization") version "1.9.0"
}

android {
    namespace = "com.example.sotugyo_kenkyu"
    compileSdk {
        // 修正せずそのまま
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.sotugyo_kenkyu"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables.useSupportLibrary = true

        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }
        val apiKey = properties.getProperty("GEMINI_API_KEY") ?: ""

        // アプリ全体で BuildConfig.GEMINI_API_KEY として使えるようにする
        buildConfigField("String", "GEMINI_API_KEY", "\"$apiKey\"")
    }

    buildFeatures {
        buildConfig = true
        // ViewBindingも必要になるため、念のため有効化推奨ですが
        // 既存コードになければエラーになる可能性があるため、もしViewBindingエラーが出たら
        // viewBinding = true をここに追加してください
        viewBinding = true
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
    sourceSets {
        getByName("main") {
            assets {
                srcDirs("src\\main\\assets", "src\\main\\assets")
            }
        }
    }

    // ★★★ 追加: 重複ファイルエラー対策 ★★★
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
            excludes += "META-INF/*.kotlin_module"
        }
    }

    // ★★★ 追加: モデルファイルが圧縮されないようにする設定 ★★★
    aaptOptions {
        noCompress += listOf("tflite", "lite", "uuid", "dic", "fst", "raw", "conf", "json")
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

    //MDCライブラリの追加
    implementation("com.google.android.material:material:1.10.0")

    // Firebase Authentication 認証に必要
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Cloud Firestore DB利用に必要
    implementation("com.google.firebase:firebase-firestore")

    // Cloud Storage for Firebase 画像取り扱いに必要
    implementation("com.google.firebase:firebase-storage")
    implementation("com.firebaseui:firebase-ui-storage:8.0.0")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    kapt("com.github.bumptech.glide:compiler:4.15.1")

    // Firebase AILogic AIチャット機能実装に必要
    implementation("com.google.firebase:firebase-ai")

    //splashscreenを使用する際に必要
    implementation("androidx.core:core-splashscreen:1.0.1")

    // 下に引っ張って更新するためのライブラリ
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // REST API通信用 (OkHttp)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("androidx.fragment:fragment-ktx:1.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.3")

    // コルーチン
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Gson
    implementation("com.google.code.gson:gson:2.13.2")

    // Algoliaクライアント
    implementation("com.algolia:algoliasearch-client-kotlin:3.0.0")
    // 非同期処理
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // lifecycleScopeを使うため
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    // Ktorエンジン
    implementation("io.ktor:ktor-client-okhttp:2.3.12")
    // JSON翻訳機
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    //画像読み込み（中分類の絵文字風画像表示）
    implementation("io.coil-kt:coil:2.5.0")

    // ★★★ 追加: Vosk 音声認識ライブラリ ★★★
    implementation("com.alphacephei:vosk-android:0.3.47")
}