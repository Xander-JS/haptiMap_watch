plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.xanderjs.haptimap.designsystem"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        minSdk = 30
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // CategoryChip expone PlaceCategory.label en su API pública
    api(project(":core:model"))
    api(platform(libs.compose.bom))
    api(libs.ui)
    api(libs.compose.foundation)
    api(libs.compose.material3)
    api(libs.ui.tooling.preview)
    api(libs.wear.tooling.preview)
    debugImplementation(libs.ui.tooling)
}
