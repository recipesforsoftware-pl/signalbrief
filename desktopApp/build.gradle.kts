import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":sharedLogic"))
    implementation(project(":sharedData"))
    implementation(project(":sharedUI"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(libs.compose.multiplatform.resources)
    implementation(libs.ktor.client.core)
    implementation(libs.koin.core)
    implementation(libs.room.runtime)

    testImplementation(libs.kotlin.test)
}

compose.desktop {
    application {
        mainClass = "pl.recipesforsoftware.signalbrief.desktop.MainKt"
    }
}

ktlint {
    version.set(libs.versions.ktlintCore)
    filter {
        exclude { element -> element.file.absolutePath.contains("build/generated") }
    }
}

detekt {
    toolVersion = libs.versions.detekt.get()
    buildUponDefaultConfig = true
    config.setFrom(rootProject.layout.projectDirectory.file("config/detekt/detekt.yml"))
    source.setFrom(files("src"))
}
