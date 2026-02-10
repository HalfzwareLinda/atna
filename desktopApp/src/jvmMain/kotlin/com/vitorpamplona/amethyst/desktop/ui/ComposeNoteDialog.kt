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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.window.Dialog
import com.vitorpamplona.amethyst.commons.model.nip10TextNotes.PublishAction
import com.vitorpamplona.amethyst.desktop.account.AccountState
import com.vitorpamplona.amethyst.desktop.network.DesktopRelayConnectionManager
import com.vitorpamplona.quartz.nip19Bech32.entities.NEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ComposeNoteDialog(
    onDismiss: () -> Unit,
    relayManager: DesktopRelayConnectionManager,
    account: AccountState.LoggedIn,
    replyTo: com.vitorpamplona.quartz.nip01Core.core.Event? = null,
    quotedEvent: com.vitorpamplona.quartz.nip01Core.core.Event? = null,
) {
    // Pre-fill content with nostr:nevent reference when quoting
    val initialContent =
        remember(quotedEvent) {
            if (quotedEvent != null) {
                val nevent = NEvent.create(quotedEvent.id, quotedEvent.pubKey, quotedEvent.kind, null as com.vitorpamplona.quartz.nip01Core.relay.normalizer.NormalizedRelayUrl?)
                "\nnostr:$nevent"
            } else {
                ""
            }
        }
    var content by remember { mutableStateOf(initialContent) }
    var isPosting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val title =
        when {
            replyTo != null -> "Reply"
            quotedEvent != null -> "Quote Note"
            else -> "New Note"
        }

    Dialog(onDismissRequest = { if (!isPosting) onDismiss() }) {
        Card(
            modifier = Modifier.width(600.dp).padding(16.dp),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                replyTo?.let { reply ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Replying to: ${reply.content.take(50)}${if (reply.content.length > 50) "..." else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Quoted note preview
                quotedEvent?.let { quoted ->
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Quoting:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                quoted.content.take(140) + if (quoted.content.length > 140) "..." else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = {
                        content = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    label = { Text(if (quotedEvent != null) "Add your commentary..." else "What's on your mind?") },
                    placeholder = { Text(if (quotedEvent != null) "Write your thoughts on this note..." else "Write your note...") },
                    enabled = !isPosting,
                    maxLines = 10,
                )

                Spacer(Modifier.height(8.dp))

                // Character count
                Text(
                    "${content.length} characters",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                errorMessage?.let { error ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isPosting,
                    ) {
                        Text("Cancel")
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (content.isBlank()) {
                                errorMessage = "Note cannot be empty"
                                return@Button
                            }

                            scope.launch {
                                isPosting = true
                                errorMessage = null

                                try {
                                    publishNote(
                                        content = content,
                                        account = account,
                                        relayManager = relayManager,
                                        replyTo = replyTo,
                                        quotedEvent = quotedEvent,
                                    )
                                    onDismiss()
                                } catch (e: Exception) {
                                    errorMessage = "Failed to publish: ${e.message}"
                                } finally {
                                    isPosting = false
                                }
                            }
                        },
                        enabled = !isPosting && content.isNotBlank(),
                    ) {
                        Text(if (isPosting) "Publishing..." else "Publish")
                    }
                }
            }
        }
    }
}

/**
 * Publishes a text note to relays.
 * Uses the Account's key to sign the event.
 */
private suspend fun publishNote(
    content: String,
    account: AccountState.LoggedIn,
    relayManager: DesktopRelayConnectionManager,
    replyTo: com.vitorpamplona.quartz.nip01Core.core.Event?,
    quotedEvent: com.vitorpamplona.quartz.nip01Core.core.Event? = null,
) {
    withContext(Dispatchers.IO) {
        // Check read-only mode
        if (account.isReadOnly) {
            throw IllegalStateException("Cannot post in read-only mode")
        }

        // Use shared PublishAction from commons
        val signedEvent = PublishAction.publishTextNote(content, account.signer, replyTo, quotedEvent)

        // Broadcast to all configured relays
        relayManager.broadcastToAll(signedEvent)
    }
}
