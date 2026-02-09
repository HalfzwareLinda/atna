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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * GitHub API response for issue creation.
 */
@Serializable
private data class GitHubIssueResponse(
    val html_url: String,
)

/**
 * GitHub API request body for creating an issue.
 */
@Serializable
private data class GitHubCreateIssueRequest(
    val title: String,
    val body: String,
    val labels: List<String> = listOf("bug"),
)

/**
 * Implementation of [BugReportSubmitter] that creates GitHub Issues.
 *
 * This submitter posts bug reports to GitHub's REST API as new issues.
 * It includes rate limiting (1 submission per 60 seconds) to prevent abuse.
 *
 * @property repoOwner GitHub repository owner
 * @property repoName GitHub repository name
 * @property token GitHub personal access token with `repo` scope
 * @property httpClient OkHttpClient instance for making HTTP requests
 */
class GitHubIssueSubmitter(
    private val repoOwner: String,
    private val repoName: String,
    private val token: String,
    private val httpClient: OkHttpClient = OkHttpClient(),
) : BugReportSubmitter {
    private val json = Json { ignoreUnknownKeys = true }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    // Rate limiting: track last submission time
    private var lastSubmissionTimeMs: Long = 0
    private val rateLimitMs: Long = 60_000 // 60 seconds

    /**
     * Submits a bug report by creating a GitHub Issue.
     *
     * @param report The bug report to submit
     * @return Result containing the issue URL on success, or an error on failure
     */
    override suspend fun submit(report: BugReport): Result<String> {
        return try {
            // Check rate limit
            val currentTime = System.currentTimeMillis()
            val timeSinceLastSubmission = currentTime - lastSubmissionTimeMs
            if (timeSinceLastSubmission < rateLimitMs) {
                val waitTimeSeconds = (rateLimitMs - timeSinceLastSubmission) / 1000
                return Result.failure(
                    IllegalStateException(
                        "Rate limited. Please wait $waitTimeSeconds seconds before submitting another report.",
                    ),
                )
            }

            // Format the issue body
            val issueBody = formatIssueBody(report)

            // Create the request payload
            val requestBody =
                GitHubCreateIssueRequest(
                    title = report.title,
                    body = issueBody,
                    labels = listOf("bug"),
                )

            val requestBodyJson = json.encodeToString(requestBody)

            // Build the HTTP request
            val url = "https://api.github.com/repos/$repoOwner/$repoName/issues"
            val request =
                Request
                    .Builder()
                    .url(url)
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .post(requestBodyJson.toRequestBody(mediaType))
                    .build()

            // Execute the request
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                return Result.failure(
                    IOException("Failed to create GitHub issue: ${response.code} - $errorBody"),
                )
            }

            val responseBody =
                response.body?.string()
                    ?: return Result.failure(IOException("Empty response body"))

            val issueResponse = json.decodeFromString<GitHubIssueResponse>(responseBody)

            // Update rate limit tracker
            lastSubmissionTimeMs = currentTime

            Result.success(issueResponse.html_url)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(IOException("Unexpected error submitting bug report", e))
        }
    }

    /**
     * Formats a bug report into a markdown issue body.
     */
    private fun formatIssueBody(report: BugReport): String =
        buildString {
            appendLine("## Description")
            appendLine(report.description)
            appendLine()

            if (report.stepsToReproduce.isNotBlank()) {
                appendLine("## Steps to Reproduce")
                appendLine(report.stepsToReproduce)
                appendLine()
            }

            appendLine("## Environment")
            appendLine("- **App Version:** ${report.appVersion}")
            appendLine("- **Platform:** ${report.platform}")
            appendLine("- **Device:** ${report.device}")
            appendLine("- **Storage Backend:** ${report.storageBackend}")
            appendLine()

            if (report.stackTrace != null) {
                appendLine("## Stack Trace")
                appendLine("```")
                appendLine(report.stackTrace)
                appendLine("```")
                appendLine()
            }

            if (report.logs != null) {
                appendLine("## Logs")
                appendLine("```")
                appendLine(report.logs)
                appendLine("```")
                appendLine()
            }

            appendLine("---")
            appendLine("*This issue was automatically generated by the ATNA bug reporter.*")
        }
}
