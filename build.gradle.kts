import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootExtension

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kover) apply false
}

// Browser-enabled Wasm targets share this root environment. Use Node from PATH
// so Kotlin does not add its Node distribution Ivy repository.
gradle.projectsEvaluated {
    rootProject.extensions.configure<WasmNodeJsRootExtension>("kotlinWasmNodeJs") {
        @Suppress("DEPRECATION_ERROR")
        download = false
    }
}
