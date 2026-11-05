plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.screenshot)
}

android {
    namespace = "com.xanderjs.haptimap.feature.onboarding"
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
    // capturas de las previews: updateDebugScreenshotTest / validateDebugScreenshotTest
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

// TutorialFirstViewTest lee las referencias de las capturas: que vuelva a correr cuando cambian
tasks.withType<Test>().configureEach {
    inputs.dir("src/screenshotTestDebug/reference").withPathSensitivity(PathSensitivity.RELATIVE)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:haptics"))
    implementation(project(":core:designsystem"))
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.lifecycle.viewmodel.compose)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.ui.tooling)
}
