plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  kotlin("plugin.serialization") version "2.1.21"
  alias(libs.plugins.google.ksp) // ksp
  id("kotlin-kapt") // brv 必须引入此插件
}

android {
  signingConfigs {
    getByName("debug") {
      storeFile = file("D:\\jdy2002\\appkey\\jdy.jks")
      storePassword = "jdy200255"
      keyAlias = "jdy2002"
      keyPassword = "jdy200255"
    }
    create("release") {
      storeFile = file("D:\\jdy2002\\appkey\\jdy.jks")
      storePassword = "jdy200255"
      keyAlias = "jdy2002"
      keyPassword = "jdy200255"
    }
  }
  namespace = "xyz.jdynb.tv"
  compileSdk = 36

  defaultConfig {
    applicationId = "xyz.jdynb.tv"
    minSdk = 23
    //noinspection ExpiredTargetSdkVersion
    targetSdk = 28
    versionCode = 4
    versionName = "1.0.3"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    signingConfig = signingConfigs.getByName("debug")
  }

  buildFeatures {
    dataBinding = true
    viewBinding = true
    buildConfig = true
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  kotlinOptions {
    jvmTarget = "11"
  }

  aaptOptions {
    noCompress("apk")
  }
}

dependencies {

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  // implementation(libs.material)
  implementation(libs.androidx.recyclerview)
  implementation(libs.androidx.activity)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.engine)
  implementation(libs.brv)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.android)
  // https://mvnrepository.com/artifact/androidx.lifecycle/lifecycle-viewmodel
  implementation("androidx.lifecycle:lifecycle-viewmodel:2.10.0")
  // https://mvnrepository.com/artifact/androidx.fragment/fragment-ktx
  implementation("androidx.fragment:fragment-ktx:1.8.9")
  // https://mvnrepository.com/artifact/androidx.fragment/fragment
  implementation("androidx.fragment:fragment:1.8.9")
  // https://mvnrepository.com/artifact/androidx.activity/activity-ktx
  implementation("androidx.activity:activity-ktx:1.12.2")
  implementation("io.github.jonanorman.android.webviewup:core:0.1.0")
  implementation("io.github.jonanorman.android.webviewup:download-source:0.1.0")
  implementation(libs.androidx.localbroadcastmanager)
  // implementation(files("libs/tbs_sdk-44382-202411081743-release.aar"))
  // implementation("com.github.HeartHappy.webX5Core:webx5core_arm64_v8a:1.0.2")
  // implementation(project(":x5core_arm64_v8a"))
  // implementation("com.tencent.tbs:tbssdk:44286")
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
}