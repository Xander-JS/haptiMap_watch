plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.xanderjs.haptimap.core.location"
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
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
