import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

kotlin {
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        getByName("desktopMain").dependencies {
            implementation(project(":core"))
            implementation(project(":shared"))
            implementation(project(":shared-ui"))
            implementation(compose.desktop.currentOs)
            implementation(compose.material3)
            implementation(libs.compose.multiplatform.resources)
            implementation(libs.ktor.client.core)
            implementation(libs.room.runtime)
        }
        getByName("desktopTest").dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

compose.desktop {
    application {
        mainClass = "pl.recipesforsoftware.signalbrief.desktop.MainKt"
    }
}

ktlint {
    version.set(libs.versions.ktlintCore)
}

detekt {
    toolVersion = libs.versions.detekt.get()
    buildUponDefaultConfig = true
    config.setFrom(rootProject.layout.projectDirectory.file("config/detekt/detekt.yml"))
    source.setFrom(files("src"))
}
