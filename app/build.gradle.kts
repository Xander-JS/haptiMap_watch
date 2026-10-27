import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// La API key de Google Maps se lee de local.properties (NO versionado); sin ella, del marcador de
// local.defaults.properties (versionado) para que el proyecto compile.
val localProperties = Properties().apply {
    listOf("local.defaults.properties", "local.properties")
        .map(rootProject::file)
        .filter { it.exists() }
        .forEach { file -> file.inputStream().use { load(it) } }
}
val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY", "")

android {
    namespace = "com.xanderjs.haptimap"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.xanderjs.haptimap"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    useLibrary("wear-sdk")
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:location"))
    implementation(project(":core:navigation"))
    implementation(project(":core:haptics"))
    implementation(project(":core:network"))
    implementation(project(":core:intelligence"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:search"))
    implementation(project(":feature:navigation-ui"))
    implementation(project(":feature:onboarding"))

    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling)
    implementation(libs.core.splashscreen)
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.wear.tooling.preview)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.compose.navigation)
    implementation(libs.wear.input)
    implementation(libs.hilt.android)
    implementation(libs.hilt.lifecycle.viewmodel.compose)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.test.manifest)
    debugImplementation(libs.ui.tooling)
}
