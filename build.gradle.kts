plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ktor) apply false
}

val releaseVersion = providers.gradleProperty("releaseVersion").orElse("1.0.0")

extra["releaseVersion"] = releaseVersion.get()
extra["androidVersionName"] = releaseVersion.get()
extra["androidVersionCode"] = providers.gradleProperty("androidVersionCode").orElse("1").get().toInt()
