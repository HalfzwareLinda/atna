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
package com.vitorpamplona.amethyst.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.atna.bugreport.BugReport
import com.atna.bugreport.GitHubIssueSubmitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DesktopBugReportScreen() {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var stepsToReproduce by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val osName = System.getProperty("os.name") ?: "Unknown"
    val osVersion = System.getProperty("os.version") ?: ""
    val javaVersion = System.getProperty("java.version") ?: ""

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "Report Bug",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Brief summary of the issue") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Describe what happened") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = stepsToReproduce,
            onValueChange = { stepsToReproduce = it },
            label = { Text("Steps to reproduce (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Platform: $osName $osVersion",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Java: $javaVersion",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "App: ATNA Desktop v1.0.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (title.isBlank() || description.isBlank()) return@Button
                isSubmitting = true
                statusMessage = null
                scope.launch {
                    val report =
                        BugReport(
                            title = title,
                            description = description,
                            stepsToReproduce = stepsToReproduce,
                            appVersion = "1.0.0",
                            platform = "$osName $osVersion",
                            device = System.getProperty("os.arch") ?: "unknown",
                        )
                    val submitter =
                        GitHubIssueSubmitter(
                            repoOwner = "HalfzwareLinda",
                            repoName = "atna",
                            token = "", // Token injected at build time or from settings
                        )
                    val result = withContext(Dispatchers.IO) { submitter.submit(report) }
                    isSubmitting = false
                    result.fold(
                        onSuccess = {
                            statusMessage = "Bug report submitted successfully"
                            title = ""
                            description = ""
                            stepsToReproduce = ""
                        },
                        onFailure = {
                            statusMessage = "Failed: ${it.message}"
                        },
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank() && description.isNotBlank() && !isSubmitting,
        ) {
            Text(if (isSubmitting) "Submitting..." else "Submit Bug Report")
        }

        statusMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (it.startsWith("Failed")) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
            )
        }
    }
}
