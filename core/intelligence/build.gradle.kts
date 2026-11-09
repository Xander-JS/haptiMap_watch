plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.xanderjs.haptimap.core.intelligence"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        minSdk = 30
    }
}

dependencies {
    api(project(":core:model"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
