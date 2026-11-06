import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.screenshot)
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
// La de DeepSeek (versión 2) igual: local.properties o el marcador, SOLO en debug. En release va siempre vacía.
val deepSeekApiKey: String = localProperties.getProperty("DEEPSEEK_API_KEY", "")

// Firma de release (paso 15A): keystore.properties en la raíz (NO versionado), con el keystore fuera del repo.
// Sin el fichero, release compila sin firmar con un aviso claro; ver README.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) keystorePropertiesFile.inputStream().use { load(it) }
}

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
        versionName = "1.0.0-rc1"

        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
        // debug hereda este valor; release lo sobreescribe a "" (abajo)
        buildConfigField("String", "DEEPSEEK_API_KEY", "\"$deepSeekApiKey\"")
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) create("release") {
            storeFile = file(keystoreProperties.getProperty("storeFile"))
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            signingConfig = signingConfigs.findByName("release")
            if (signingConfig == null) logger.warn("HaptiMap: sin keystore.properties en la raíz; la build de release irá SIN FIRMAR. Crea el fichero (ver README) para firmarla.")
            // la clave de DeepSeek nunca viaja en release
            buildConfigField("String", "DEEPSEEK_API_KEY", "\"\"")
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
    // capturas de las previews: updateDebugScreenshotTest / validateDebugScreenshotTest
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
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
    implementation(project(":feature:settings"))

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
    implementation(libs.wear.ongoing)
    implementation(libs.core)
    implementation(libs.hilt.android)
    implementation(libs.hilt.lifecycle.viewmodel.compose)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.test.manifest)
    debugImplementation(libs.ui.tooling)
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.ui.tooling)
}

// EdgeButtonLabelTest lee el código y los textos de todos los módulos: sin esto Gradle no lo repetiría al cambiarlos
tasks.withType<Test>().configureEach {
    inputs.files(
        rootProject.fileTree(rootDir) {
            include("*/src/main/**/*.kt", "*/*/src/main/**/*.kt", "*/src/debug/**/*.kt", "**/src/main/res/values*/strings.xml")
            exclude("**/build/**")
        },
    ).withPathSensitivity(PathSensitivity.RELATIVE)
}
