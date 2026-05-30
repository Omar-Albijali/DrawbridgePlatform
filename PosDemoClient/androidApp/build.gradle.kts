plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidApplication)
}

val androidVersionName = rootProject.extra["androidVersionName"] as String
val androidVersionCode = rootProject.extra["androidVersionCode"] as Int

kotlin {
    androidTarget()

    sourceSets {
        androidMain.dependencies {
            implementation(projects.posDemoClient.composeApp)
            implementation(projects.shared)
            implementation(libs.androidx.activity.compose)
        }
    }

    jvmToolchain(21)
}

android {
    namespace = "uqu.drawbridge.posdemo.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "uqu.drawbridge.posdemo.android"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = androidVersionCode
        versionName = androidVersionName
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}
