import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.screenshot)
    // caché del desvío (paso 20): persiste el POI y la ruta original en JSON
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.xanderjs.haptimap.feature.navigationui"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        minSdk = 30
    }
    // maps-compose (paso 22) se compila con JVM 17; este módulo, el único que lo usa, lo iguala (el resto sigue en 11)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    // capturas de las previews: updateDebugScreenshotTest / validateDebugScreenshotTest
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

kotlin {
    compilerOptions { jvmTarget = JvmTarget.JVM_17 }
}

// AmbientPositionTest lee las referencias de las capturas: que vuelva a correr cuando cambian
tasks.withType<Test>().configureEach {
    inputs.dir("src/screenshotTestDebug/reference").withPathSensitivity(PathSensitivity.RELATIVE)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:navigation"))
    implementation(project(":core:haptics"))
    implementation(project(":core:intelligence"))
    implementation(project(":core:network"))
    implementation(project(":core:location"))
    implementation(project(":core:designsystem"))
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.lifecycle.viewmodel.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    // mapa continuo (paso 22): GoogleMap en Compose + Maps SDK (transitivo)
    implementation(libs.maps.compose)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.ui.tooling)
}
