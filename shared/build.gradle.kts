import org.jetbrains.kotlin.gradle.dsl.JvmTarget

import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.buildConfig)
}

val localEnv = Properties().apply {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        load(FileInputStream(envFile))
    }
}

buildConfig {
    packageName("uqu.drawbridge.platform.shared")
    buildConfigField("String", "API_BASE_URL", "\"${localEnv.getProperty("COMPOSE_API_BASE_URL") ?: "http://10.0.2.2:8080/api"}\"")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    iosArm64()
    iosSimulatorArm64()
    
    jvm()
    
    js {
        outputModuleName = "shared"
        browser()
        binaries.library()
        generateTypeScriptDefinitions()
        compilerOptions {
            target = "es2015"
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }
        val iosArm64Main by getting {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        val iosSimulatorArm64Main by getting {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "uqu.drawbridge.platform.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
