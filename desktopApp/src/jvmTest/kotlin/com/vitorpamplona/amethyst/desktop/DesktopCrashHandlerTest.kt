/**
 * Copyright (c) 2025 Vitor Pamplona
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.vitorpamplona.amethyst.desktop

import com.atna.bugreport.CrashHandler
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DesktopCrashHandlerTest {
    private val testDir = System.getProperty("java.io.tmpdir") + "/atna-test-crash"
    private val testCrashFile = "$testDir/crash_report.json"

    private fun cleanup() {
        File(testCrashFile).delete()
        File(testDir).delete()
    }

    @Test
    fun loadPendingCrashReportReturnsNullWhenNoFile() {
        cleanup()
        val report = CrashHandler.loadPendingCrashReport(testCrashFile)
        assertNull(report)
    }

    @Test
    fun loadPendingCrashReportReturnsReportWhenFileExists() {
        cleanup()
        File(testDir).mkdirs()
        val jsonContent =
            """
            {
                "title": "Crash: NullPointerException",
                "description": "The app crashed",
                "appVersion": "1.0.0",
                "platform": "Linux",
                "device": "testhost",
                "stackTrace": "at com.example.Main.main(Main.kt:10)",
                "stepsToReproduce": "",
                "storageBackend": "SQLite"
            }
            """.trimIndent()
        File(testCrashFile).writeText(jsonContent)

        val loaded = CrashHandler.loadPendingCrashReport(testCrashFile)
        assertNotNull(loaded)
        assertEquals("Crash: NullPointerException", loaded.title)
        assertEquals("The app crashed", loaded.description)
        assertEquals("at com.example.Main.main(Main.kt:10)", loaded.stackTrace)

        cleanup()
    }

    @Test
    fun clearPendingCrashReportDeletesFile() {
        cleanup()
        File(testDir).mkdirs()
        File(testCrashFile).writeText("{}")

        CrashHandler.clearPendingCrashReport(testCrashFile)
        assertNull(CrashHandler.loadPendingCrashReport(testCrashFile))

        cleanup()
    }

    @Test
    fun crashHandlerInstallDoesNotThrow() {
        val handler =
            CrashHandler(
                crashFilePath = testCrashFile,
                appVersion = "1.0.0-test",
                platform = "Linux Test",
                device = "test-machine",
            )
        handler.install()

        // Restore original handler
        Thread.setDefaultUncaughtExceptionHandler(null)
    }
}
