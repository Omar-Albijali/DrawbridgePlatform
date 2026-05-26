
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
            implementation(projects.composeApp)
            implementation(projects.shared)
//            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
        }

    }

    jvmToolchain(21)
}

android {
    namespace = "uqu.drawbridge.platform"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "uqu.drawbridge.platform"
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
    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}


dependencies {
//    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.compose.uiTooling)
}
