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

import kotlinx.serialization.Serializable

/**
 * Represents a bug report with all relevant information.
 *
 * @property title Brief summary of the bug
 * @property description Detailed description of the bug
 * @property stepsToReproduce Steps to reproduce the bug
 * @property appVersion Version of the app where the bug occurred
 * @property platform Platform information (e.g., "Android 14" or "Ubuntu 24.04")
 * @property device Device information (e.g., "Pixel 6" or hostname)
 * @property storageBackend Storage backend being used (default: "SQLite")
 * @property logs Optional log output
 * @property stackTrace Optional stack trace if available
 */
@Serializable
data class BugReport(
    val title: String,
    val description: String,
    val stepsToReproduce: String = "",
    val appVersion: String,
    val platform: String,
    val device: String,
    val storageBackend: String = "SQLite",
    val logs: String? = null,
    val stackTrace: String? = null,
)

/**
 * Interface for submitting bug reports.
 */
interface BugReportSubmitter {
    /**
     * Submits a bug report.
     *
     * @param report The bug report to submit
     * @return Result containing the issue URL on success, or an error on failure
     */
    suspend fun submit(report: BugReport): Result<String>
}
