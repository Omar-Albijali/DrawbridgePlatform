

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidApplication)
}


kotlin {
    androidTarget()

    sourceSets {
        androidMain.dependencies {
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
        versionCode = 1
        versionName = "1.0"
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
//    implementation(projects.composeApp)
//    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.compose.uiTooling)
}
