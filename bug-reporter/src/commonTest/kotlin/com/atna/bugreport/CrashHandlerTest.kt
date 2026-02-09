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
package com.atna.bugreport

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CrashHandlerTest {
    @Test
    fun loadPendingCrashReportReturnsNullForMissingFile() {
        val report = CrashHandler.loadPendingCrashReport("/tmp/nonexistent-crash-file-${System.currentTimeMillis()}.json")
        assertNull(report)
    }

    @Test
    fun loadPendingCrashReportParsesValidJson() {
        val testPath = "/tmp/test-crash-${System.currentTimeMillis()}.json"
        val bugReport =
            BugReport(
                title = "Crash: NPE",
                description = "The app crashed",
                appVersion = "1.0.0",
                platform = "Android 14",
                device = "Pixel 6",
                stackTrace = "java.lang.NullPointerException",
            )
        writeToFile(testPath, Json.encodeToString(bugReport))

        try {
            val loaded = CrashHandler.loadPendingCrashReport(testPath)
            assertNotNull(loaded)
            assertEquals("Crash: NPE", loaded.title)
            assertEquals("java.lang.NullPointerException", loaded.stackTrace)
        } finally {
            deleteFile(testPath)
        }
    }

    @Test
    fun clearPendingCrashReportDeletesFile() {
        val testPath = "/tmp/test-crash-clear-${System.currentTimeMillis()}.json"
        writeToFile(testPath, "{}")

        CrashHandler.clearPendingCrashReport(testPath)

        assertNull(readFromFile(testPath))
    }

    @Test
    fun loadPendingCrashReportReturnsNullForInvalidJson() {
        val testPath = "/tmp/test-crash-invalid-${System.currentTimeMillis()}.json"
        writeToFile(testPath, "not valid json")

        try {
            val loaded = CrashHandler.loadPendingCrashReport(testPath)
            assertNull(loaded)
        } finally {
            deleteFile(testPath)
        }
    }
}
