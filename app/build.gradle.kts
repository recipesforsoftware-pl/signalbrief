import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import java.util.Properties

val localProperties =
    Properties().apply {
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            load(localPropertiesFile.inputStream())
        }
    }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

android {
    namespace = "com.recipesforsoftware.mvvm"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.recipesforsoftware.mvvm"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "NEWS_API_KEY",
            "\"${localProperties.getProperty("NEWS_API_KEY", "")}\"",
        )
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ktlint {
    version.set(libs.versions.ktlintCore)
}

detekt {
    toolVersion = libs.versions.detekt.get()
    buildUponDefaultConfig = true
    config.setFrom(rootProject.layout.projectDirectory.file("config/detekt/detekt.yml"))
}

kover {
    currentProject {
        createVariant("all") {
            addWithDependencies("debug")
        }
    }
    reports {
        variant("all") {
            filters {
                excludes {
                    // AGP generated
                    classes("*.BuildConfig")
                    classes("*.R")
                    classes("*.R$*")
                    // Dagger/Hilt generated
                    classes("*Hilt*")
                    classes("*Dagger*")
                    classes("*_Factory")
                    classes("*_MembersInjector")
                    // Compose compiler generated
                    classes("*ComposableSingletons*")
                }
            }
            verify {
                rule {
                    bound {
                        minValue = 9
                        coverageUnits = CoverageUnit.LINE
                        aggregationForGroup = AggregationType.COVERED_PERCENTAGE
                    }
                }
            }
        }
    }
}

dependencies {
    // Shared KMP domain module
    implementation(project(":shared"))

    // Shared Compose Multiplatform UI (Top Headlines screen + presenter)
    implementation(project(":shared-ui"))

    // Aggregate :shared and :shared-ui coverage into :app's Kover "all" report variant
    kover(project(":shared"))
    kover(project(":shared-ui"))

    // Ktor client type used at the Hilt composition boundary
    implementation(libs.ktor.client.core)

    // Room database type used at the Hilt composition boundary
    implementation(libs.room.runtime)

    // Compose BOM
    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))
    testImplementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)

    // Activity & Lifecycle
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Image Loading
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Browser
    implementation(libs.browser)

    // DataStore Preferences
    implementation(libs.datastore.preferences)

    // Unit Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)

    // Android Testing
    androidTestImplementation(libs.junit.test.ext)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
