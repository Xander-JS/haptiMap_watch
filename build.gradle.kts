// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.screenshot) apply false
}

// Toolchain único del proyecto: todo compila a JVM 17, definido en un solo sitio (lo exige maps-compose, paso 22).
// Antes cada módulo repetía su versión; este fix la centraliza aquí y la retira de los módulos.
subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    }
    // Módulos Android: su nivel de bytecode Java (library y application por separado, cada extensión es concreta)
    plugins.withId("com.android.library") {
        extensions.configure<com.android.build.api.dsl.LibraryExtension> {
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
    plugins.withId("com.android.application") {
        extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
    // Módulos Kotlin puro (JVM, sin Android)
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
}

// Guardarraíl del design system (rediseño, cierre): falla el build si fuera de :core:designsystem aparece un color o
// una medida literal, o si en cualquier módulo hay un texto visible escrito en el código. Un grep y no detekt: tres
// reglas no justifican otra herramienta. shortcut: solo ve literales en la misma línea; si se cuela algo, pasar a detekt.
val checkDesignTokens by tasks.registering {
    group = "verification"
    description = "Fails on hardcoded colors/dimensions outside core:designsystem and hardcoded UI strings anywhere."
    val sources = fileTree(rootDir) {
        include("*/src/main/**/*.kt", "*/*/src/main/**/*.kt", "*/src/debug/**/*.kt", "*/src/release/**/*.kt")
        exclude("**/build/**")
    }
    val root = rootDir
    val marker = layout.buildDirectory.file("checkDesignTokens.ok")
    inputs.files(sources)
    outputs.file(marker)
    doLast {
        val raw = listOf(
            Regex("""\bColor\s*\(""") to "hardcoded color (use HaptiMapTheme.colors)",
            Regex("""\bColor\.(Red|Green|Blue|Black|White|Gray|DarkGray|LightGray|Yellow|Cyan|Magenta)\b""") to "hardcoded color (use HaptiMapTheme.colors)",
            Regex("""\b\d+(\.\d+)?f?\.(dp|sp)\b""") to "hardcoded dimension (use HaptiMapTheme.spacing/dimens/typography)",
        )
        val text = Regex("""(\bText\(\s*|\b(text|contentDescription|label|title|subtitle|caption)\s*=\s*)"[^"]+"""")
        val problems = sources.files.sorted().flatMap { file ->
            val path = file.relativeTo(root).invariantSeparatorsPath
            val rules = (if (path.startsWith("core/designsystem/")) emptyList() else raw) +
                (text to "hardcoded user-facing text (use strings.xml, en + es)")
            file.readLines().withIndex().flatMap { (i, line) ->
                val code = line.substringBefore("//")
                rules.filter { it.first.containsMatchIn(code) }.map { "$path:${i + 1}: ${it.second}\n    ${line.trim()}" }
            }
        }
        if (problems.isNotEmpty()) throw GradleException("Design system guardrail:\n" + problems.joinToString("\n"))
        marker.get().asFile.writeText("ok")
    }
}
subprojects {
    tasks.matching { it.name == "preBuild" || it.name == "check" }.configureEach { dependsOn(checkDesignTokens) }
}
