import com.android.build.api.variant.FilterConfiguration
import java.text.SimpleDateFormat
import java.util.Date

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  kotlin("plugin.serialization") version "2.1.21"
  // alias(libs.plugins.google.ksp) // ksp
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
    minSdk = 21
    //noinspection ExpiredTargetSdkVersion
    targetSdk = 28
    versionCode = 10
    versionName = "1.0.6.4"

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

  flavorDimensions("example")

  productFlavors {
    create("webview") {
      // 默认配置
    }

    // X5 内核已收费，不支持，如果你有付费的，请开启注释，并按照文档进行配置
    /*create("x5") {
    }*/

    // 限制太多，不支持
    /*create("gecko") {
      applicationIdSuffix = ".gecko"
    }*/
  }

  /*splits {
    abi {
      isEnable = true // 开启不同cpu apk 拆分
      reset() // 重置默认的cpu平台
      include("armeabi-v7a", "arm64-v8a", "x86", "armeabi") // 只打包 v8、v7两种架构的安装包
      isUniversalApk = true // 全量包
    }
  }*/

  applicationVariants.all {
    outputs.all {

      this as com.android.build.gradle.internal.api.BaseVariantOutputImpl

      val flavorNames = productFlavors.map { it.name }
      val flavorNameStr = flavorNames.joinToString("_")
      val createTime = SimpleDateFormat("yyyyMMdd_HHmm").format(Date())
      val abiName = getFilter(FilterConfiguration.FilterType.ABI.name) ?: "all"

      val newName = "app_${flavorNameStr}_${buildType.name}_v${versionName}_${createTime}_$abiName.apk"
      outputFileName = newName
      println("配置 APK 文件名: $newName")
    }
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
  implementation(libs.androidx.lifecycle.viewmodel)
  // https://mvnrepository.com/artifact/androidx.fragment/fragment-ktx
  implementation(libs.androidx.fragment.ktx)
  // https://mvnrepository.com/artifact/androidx.fragment/fragment
  implementation(libs.androidx.fragment)
  // https://mvnrepository.com/artifact/androidx.activity/activity-ktx
  implementation(libs.androidx.activity.ktx)
  implementation("io.github.jonanorman.android.webviewup:core:0.1.0")
  implementation("io.github.jonanorman.android.webviewup:download-source:0.1.0")
  implementation(libs.androidx.localbroadcastmanager)
  // "x5Implementation"(files("libs/tbs_sdk-44382-202411081743-release.aar"))
  // "geckoImplementation"("org.mozilla.geckoview:geckoview:93.0.20210927210923")
  // implementation(files("libs/tbs_sdk-44382-202411081743-release.aar"))
  // "x5Implementation"(project(":x5core_arm64_v8a"))
  // "x5Implementation"("com.github.HeartHappy.webX5Core:webx5core_armeabi_v7a:1.0.2")
  // implementation(project(":x5core_arm64_v8a"))
  // "x5Implementation"(libs.tbssdk)
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
}