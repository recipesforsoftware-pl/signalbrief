import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

@OptIn(ExperimentalWasmDsl::class)
kotlin {
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    android {
        namespace = "pl.recipesforsoftware.signalbrief.sharedui"
        compileSdk = 37
        minSdk = 24

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        @Suppress("UnstableApiUsage")
        androidResources {
            enable = true
        }

        withHostTest {}
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SignalBriefSharedUi"
            isStatic = true
        }
    }

    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":sharedLogic"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.compose.material.icons.core)
            implementation(libs.compose.multiplatform.resources)
            implementation(libs.coil.compose)
            implementation(libs.compose.lifecycle.viewmodel.compose)
        }
        androidMain.dependencies {
            implementation(libs.coil.network.ktor3)
        }
        iosMain.dependencies {
            implementation(project(":sharedData"))
            implementation(libs.coil.network.ktor3)
            implementation(libs.koin.core)
        }
        named("desktopMain") {
            dependencies {
                implementation(libs.coil.network.ktor3)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.coroutines.test)
            implementation(libs.turbine)
        }
    }
}

ktlint {
    version.set(libs.versions.ktlintCore)
    filter {
        exclude("**/generated/**")
    }
}

detekt {
    toolVersion = libs.versions.detekt.get()
    buildUponDefaultConfig = true
    config.setFrom(rootProject.layout.projectDirectory.file("config/detekt/detekt.yml"))
    source.setFrom(
        kotlin.sourceSets
            .flatMap { it.kotlin.srcDirs }
            .filterNot { it.path.contains("build/generated") },
    )
}

compose.resources {
    publicResClass = true
    packageOfResClass = "pl.recipesforsoftware.signalbrief.sharedui.generated.resources"
}

kover {
    currentProject {
        createVariant("all") {
            addWithDependencies("android")
        }
    }
}
