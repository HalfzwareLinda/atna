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
package com.vitorpamplona.amethyst.ui.screen.loggedIn.bugreport

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.atna.bugreport.BugReport
import com.atna.bugreport.GitHubIssueSubmitter
import com.vitorpamplona.amethyst.BuildConfig
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.ui.navigation.navs.INav
import com.vitorpamplona.amethyst.ui.navigation.topbars.TopBarWithBackButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BugReportScreen(nav: INav) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopBarWithBackButton(
                caption = stringResource(R.string.bug_report_title),
                popBack = { nav.popBack() },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        BugReportForm(
            modifier = Modifier.padding(padding),
            snackbarHostState = snackbarHostState,
        )
    }
}

@Composable
private fun BugReportForm(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var stepsToReproduce by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val successMessage = stringResource(R.string.bug_report_success)
    val errorMessage = stringResource(R.string.bug_report_error)

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(R.string.bug_report_title_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(R.string.bug_report_description_hint)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = stepsToReproduce,
            onValueChange = { stepsToReproduce = it },
            label = { Text(stringResource(R.string.bug_report_steps_hint)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Platform: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Device: ${Build.MANUFACTURER} ${Build.MODEL}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "App: ${BuildConfig.APPLICATION_ID} v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (title.isBlank() || description.isBlank()) return@Button
                isSubmitting = true
                scope.launch {
                    val report =
                        BugReport(
                            title = title,
                            description = description,
                            stepsToReproduce = stepsToReproduce,
                            appVersion = BuildConfig.VERSION_NAME,
                            platform = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                            device = "${Build.MANUFACTURER} ${Build.MODEL}",
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
                            snackbarHostState.showSnackbar(successMessage)
                            title = ""
                            description = ""
                            stepsToReproduce = ""
                        },
                        onFailure = {
                            snackbarHostState.showSnackbar("$errorMessage: ${it.message}")
                        },
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = title.isNotBlank() && description.isNotBlank() && !isSubmitting,
        ) {
            Text(
                if (isSubmitting) {
                    stringResource(R.string.bug_report_submitting)
                } else {
                    stringResource(R.string.bug_report_submit)
                },
            )
        }
    }
}
