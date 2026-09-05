import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

@OptIn(ExperimentalWasmDsl::class)
kotlin {
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":core"))
            implementation(project(":shared-ui"))
            implementation(libs.kotlinx.serialization.json)
            implementation(compose.runtime)
            implementation(compose.ui)
        }
        wasmJsTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.coroutines.test)
            implementation(libs.turbine)
        }
    }
}

// Use Node from PATH instead of Kotlin's automatic Node distribution download.
extensions.configure<WasmNodeJsEnvSpec>("kotlinWasmNodeJsSpec") {
    download.set(false)
}

// Use the system Binaryen installation (wasm-opt from PATH) for Wasm production builds.
plugins.withType<BinaryenPlugin> {
    the<BinaryenEnvSpec>().download.set(false)
}

ktlint { version.set(libs.versions.ktlintCore) }

detekt {
    toolVersion = libs.versions.detekt.get()
    buildUponDefaultConfig = true
    config.setFrom(rootProject.layout.projectDirectory.file("config/detekt/detekt.yml"))
    source.setFrom(files("src"))
}
