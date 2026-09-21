package pl.recipesforsoftware.signalbrief.desktop

import java.nio.file.Files
import java.nio.file.Path

private const val APPLICATION_DIRECTORY_NAME = "SignalBrief"
private const val DATABASE_FILE_NAME = "signalbrief.db"
private const val MACOS_APPLICATION_SUPPORT_DIRECTORY = "Application Support"

internal fun createDesktopDatabasePath(
    osName: String = System.getProperty("os.name").orEmpty(),
    userHome: String? = System.getProperty("user.home"),
    appData: String? = System.getenv("APPDATA"),
): String {
    val databaseDirectory =
        when {
            osName.contains("mac", ignoreCase = true) -> {
                requirePath(userHome, "user.home")
                    .resolve("Library")
                    .resolve(MACOS_APPLICATION_SUPPORT_DIRECTORY)
                    .resolve(APPLICATION_DIRECTORY_NAME)
            }

            osName.contains("windows", ignoreCase = true) -> {
                requirePath(appData, "APPDATA")
                    .resolve(APPLICATION_DIRECTORY_NAME)
            }

            else -> {
                throw UnsupportedOperationException(
                    "SignalBrief Desktop database paths are supported only on macOS and Windows.",
                )
            }
        }

    Files.createDirectories(databaseDirectory)
    return databaseDirectory
        .resolve(DATABASE_FILE_NAME)
        .toAbsolutePath()
        .normalize()
        .toString()
}

private fun requirePath(
    value: String?,
    propertyName: String,
): Path =
    value
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.let(Path::of)
        ?: error("$propertyName is missing or empty.")
