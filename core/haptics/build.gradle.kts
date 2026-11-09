plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.xanderjs.haptimap.core.haptics"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        minSdk = 30
    }
}

dependencies {
    api(project(":core:model"))
    // solo por NavigationCue en el adaptador HapticCues; HapticEngine no la usa (ARCHITECTURE.md)
    api(project(":core:navigation"))
    api(libs.kotlinx.coroutines.core)
    // solo las anotaciones @Inject/@Singleton; Hilt las procesa desde :app
    implementation(libs.javax.inject)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
