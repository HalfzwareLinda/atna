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

/**
 * Handles uncaught exceptions by saving crash data to a file.
 * On next app launch, the saved crash data can be loaded and
 * the user prompted to submit a bug report.
 *
 * @param crashFilePath Absolute path to the file where crash data is persisted
 * @param appVersion Current app version string
 * @param platform Platform description (e.g., "Android 14", "Ubuntu 24.04")
 * @param device Device description (e.g., "Pixel 6", hostname)
 */
class CrashHandler(
    private val crashFilePath: String,
    private val appVersion: String,
    private val platform: String,
    private val device: String,
) {
    private var previousHandler: Thread.UncaughtExceptionHandler? = null

    fun install() {
        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (isNonFatalNativeDisposal(throwable)) {
                // GStreamer native object disposal race — non-fatal, don't crash the app.
                // This happens when AWT-EventQueue is mid-paint while the GStreamer
                // pipeline is being torn down. Safe to swallow.
                return@setDefaultUncaughtExceptionHandler
            }
            saveCrashData(thread, throwable)
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Detects the GStreamer "Native object has been disposed" race condition
     * that occurs on AWT-EventQueue when a video composable is removed while
     * GStreamer is still painting a frame. This is non-fatal.
     */
    private fun isNonFatalNativeDisposal(throwable: Throwable): Boolean =
        throwable is IllegalStateException &&
            throwable.message?.contains("Native object has been disposed") == true

    private fun saveCrashData(
        thread: Thread,
        throwable: Throwable,
    ) {
        try {
            val stackTrace =
                buildString {
                    appendLine("Thread: ${thread.name}")
                    appendLine("Exception: ${throwable::class.simpleName}: ${throwable.message}")
                    appendLine()
                    for (element in throwable.stackTrace) {
                        appendLine("    at $element")
                    }
                    var cause = throwable.cause
                    while (cause != null) {
                        appendLine("Caused by: ${cause::class.simpleName}: ${cause.message}")
                        for (element in cause.stackTrace) {
                            appendLine("    at $element")
                        }
                        cause = cause.cause
                    }
                }

            val report =
                BugReport(
                    title = "Crash: ${throwable::class.simpleName}: ${throwable.message?.take(80) ?: "unknown"}",
                    description = "The app crashed with an uncaught exception.",
                    appVersion = appVersion,
                    platform = platform,
                    device = device,
                    stackTrace = stackTrace,
                )

            val json = Json.encodeToString(report)
            writeToFile(crashFilePath, json)
        } catch (_: Exception) {
            // Best-effort: don't let crash handler itself crash
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun loadPendingCrashReport(crashFilePath: String): BugReport? =
            try {
                val content = readFromFile(crashFilePath)
                if (content.isNullOrBlank()) {
                    null
                } else {
                    json.decodeFromString<BugReport>(content)
                }
            } catch (_: Exception) {
                null
            }

        fun clearPendingCrashReport(crashFilePath: String) {
            try {
                deleteFile(crashFilePath)
            } catch (_: Exception) {
                // Best-effort cleanup
            }
        }
    }
}

internal expect fun writeToFile(
    path: String,
    content: String,
)

internal expect fun readFromFile(path: String): String?

internal expect fun deleteFile(path: String)
