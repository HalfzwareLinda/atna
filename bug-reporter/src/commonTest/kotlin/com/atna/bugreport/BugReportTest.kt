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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BugReportTest {
    @Test
    fun bugReportCreation() {
        val report =
            BugReport(
                title = "Test crash",
                description = "App crashed",
                appVersion = "1.0.0",
                platform = "Android 14",
                device = "Pixel 6",
            )
        assertEquals("Test crash", report.title)
        assertEquals("App crashed", report.description)
        assertEquals("1.0.0", report.appVersion)
        assertEquals("Android 14", report.platform)
        assertEquals("Pixel 6", report.device)
        assertEquals("SQLite", report.storageBackend)
        assertNull(report.logs)
        assertNull(report.stackTrace)
    }

    @Test
    fun bugReportWithOptionalFields() {
        val report =
            BugReport(
                title = "Test",
                description = "Description",
                stepsToReproduce = "1. Open app\n2. Tap button",
                appVersion = "1.0.0",
                platform = "Ubuntu 24.04",
                device = "desktop",
                storageBackend = "nostrdb",
                logs = "some log output",
                stackTrace = "java.lang.NullPointerException",
            )
        assertEquals("nostrdb", report.storageBackend)
        assertEquals("some log output", report.logs)
        assertEquals("java.lang.NullPointerException", report.stackTrace)
    }

    @Test
    fun bugReportSerializationRoundTrip() {
        val report =
            BugReport(
                title = "Serialization test",
                description = "Testing JSON",
                appVersion = "2.0.0",
                platform = "Linux 6.8",
                device = "laptop",
                stackTrace = "at com.example.Main.main(Main.kt:42)",
            )
        val json = Json.encodeToString(report)
        val decoded = Json.decodeFromString<BugReport>(json)
        assertEquals(report, decoded)
    }

    @Test
    fun bugReportDefaultStepsEmpty() {
        val report =
            BugReport(
                title = "Test",
                description = "Desc",
                appVersion = "1.0.0",
                platform = "Android",
                device = "Device",
            )
        assertTrue(report.stepsToReproduce.isEmpty())
    }
}
