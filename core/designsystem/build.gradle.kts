plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.screenshot)
}

android {
    namespace = "com.xanderjs.haptimap.designsystem"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        minSdk = 30
    }
    buildFeatures {
        compose = true
    }
    // capturas de las previews: updateDebugScreenshotTest / validateDebugScreenshotTest
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

dependencies {
    // sin :core:model a propósito: los componentes solo aceptan valores de UI (DESIGN_SYSTEM.md §1)
    api(platform(libs.compose.bom))
    api(libs.ui)
    api(libs.compose.foundation)
    api(libs.compose.material3)
    api(libs.ui.tooling.preview)
    api(libs.wear.tooling.preview)
    debugImplementation(libs.ui.tooling)
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.ui.tooling)
}
