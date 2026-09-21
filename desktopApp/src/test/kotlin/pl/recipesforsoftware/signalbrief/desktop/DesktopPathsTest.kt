package pl.recipesforsoftware.signalbrief.desktop

import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DesktopPathsTest {
    private val temporaryDirectories = mutableListOf<Path>()

    @AfterTest
    fun cleanUpTemporaryDirectories() {
        temporaryDirectories.forEach(::deleteRecursively)
    }

    @Test
    fun createDesktopDatabasePathUsesMacosApplicationSupportDirectory() {
        val userHome = createTemporaryDirectory()

        val databasePath =
            Path.of(
                createDesktopDatabasePath(
                    osName = "Mac OS X",
                    userHome = userHome.toString(),
                ),
            )

        assertEquals(
            userHome.resolve("Library/Application Support/SignalBrief/signalbrief.db").toAbsolutePath().normalize(),
            databasePath,
        )
        assertTrue(Files.isDirectory(databasePath.parent))
    }

    @Test
    fun createDesktopDatabasePathUsesWindowsAppDataDirectory() {
        val appData = createTemporaryDirectory()

        val databasePath =
            Path.of(
                createDesktopDatabasePath(
                    osName = "Windows 11",
                    appData = appData.toString(),
                ),
            )

        assertEquals(
            appData.resolve("SignalBrief/signalbrief.db").toAbsolutePath().normalize(),
            databasePath,
        )
        assertTrue(Files.isDirectory(databasePath.parent))
    }

    @Test
    fun createDesktopDatabasePathFailsWhenWindowsAppDataIsMissing() {
        assertFailsWith<IllegalStateException> {
            createDesktopDatabasePath(osName = "Windows 11", appData = null)
        }
    }

    @Test
    fun createDesktopDatabasePathFailsWhenMacosUserHomeIsMissing() {
        assertFailsWith<IllegalStateException> {
            createDesktopDatabasePath(osName = "Mac OS X", userHome = null)
        }
    }

    @Test
    fun createDesktopDatabasePathFailsForLinux() {
        assertFailsWith<UnsupportedOperationException> {
            createDesktopDatabasePath(osName = "Linux", userHome = createTemporaryDirectory().toString())
        }
    }

    private fun createTemporaryDirectory(): Path =
        Files.createTempDirectory("signalbrief-desktop-path-test").also(temporaryDirectories::add)

    private fun deleteRecursively(path: Path) {
        Files.walk(path).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }
}
