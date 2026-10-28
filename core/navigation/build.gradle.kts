import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Kotlin puro, sin Android: la máquina de estados de la sesión se prueba en la JVM
plugins {
    alias(libs.plugins.kotlin.jvm)
    // DTOs de la caché de ruta (paso 06); :core:model no sabe nada de serialización
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions { jvmTarget = JvmTarget.JVM_11 }
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)
    // solo las anotaciones @Inject/@Singleton; Hilt las procesa desde :app
    implementation(libs.javax.inject)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
