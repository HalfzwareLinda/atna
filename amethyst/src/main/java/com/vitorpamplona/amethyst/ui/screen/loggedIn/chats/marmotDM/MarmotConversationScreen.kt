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
package com.vitorpamplona.amethyst.ui.screen.loggedIn.chats.marmotDM

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.atna.marmot.MarmotMessage
import com.vitorpamplona.amethyst.Amethyst
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.ui.navigation.navs.INav
import com.vitorpamplona.amethyst.ui.navigation.topbars.TopBarWithBackButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MarmotConversationScreen(
    groupId: String,
    groupName: String,
    nav: INav,
) {
    val router = Amethyst.instance.marmotRouter
    val scope = rememberCoroutineScope()

    LaunchedEffect(groupId) {
        router.refreshMessagesForGroup(groupId)
    }

    val allMessages by router.messagesPerGroup.collectAsState()
    val messages = allMessages[groupId] ?: emptyList()
    val listState = rememberLazyListState()

    var messageText by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopBarWithBackButton(
                caption = groupName.ifEmpty { stringResource(R.string.marmot_conversation) },
                popBack = { nav.popBack() },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding(),
        ) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
            ) {
                // Background Marmot watermark
                MarmotWatermark(
                    modifier =
                        Modifier
                            .size(200.dp)
                            .align(Alignment.Center)
                            .alpha(0.06f),
                )

                if (messages.isEmpty()) {
                    EmptyConversationPlaceholder(
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        reverseLayout = true,
                        contentPadding =
                            androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 12.dp,
                                vertical = 8.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(
                            messages.sortedByDescending { it.timestamp },
                            key = { it.id },
                        ) { message ->
                            MessageBubble(message)
                        }
                    }
                }
            }

            // Message input
            MessageInputRow(
                text = messageText,
                onTextChange = { messageText = it },
                sending = sending,
                onSend = {
                    if (messageText.isNotBlank() && !sending) {
                        val text = messageText.trim()
                        messageText = ""
                        sending = true
                        scope.launch {
                            try {
                                val eventJson =
                                    router.sendMessage(
                                        groupId = groupId,
                                        senderKey = "",
                                        content = text,
                                    )
                                // Parse and broadcast the encrypted event
                                if (eventJson.isNotEmpty()) {
                                    try {
                                        val event =
                                            com.vitorpamplona.quartz.nip01Core.core.Event
                                                .fromJson(eventJson)
                                        val relays =
                                            Amethyst.instance.client
                                                .connectedRelaysFlow()
                                                .value
                                        if (relays.isNotEmpty()) {
                                            Amethyst.instance.client.send(event, relays)
                                        }
                                    } catch (e: Exception) {
                                        System.err.println("MarmotConversation: failed to broadcast: ${e.message}")
                                    }
                                }
                                router.refreshMessagesForGroup(groupId)
                            } catch (e: Exception) {
                                System.err.println("MarmotConversation: send failed: ${e.message}")
                            } finally {
                                sending = false
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
fun MarmotWatermark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.marmot_logo),
        contentDescription = stringResource(R.string.marmot_powered_by),
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun EmptyConversationPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.marmot_no_messages),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.marmot_powered_by),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun MessageBubble(message: MarmotMessage) {
    // For now treat all messages as "them" since we can't easily determine
    // the current user key at this layer. A future enhancement would pass
    // the user pubkey to distinguish own vs. other messages.
    val isOwnMessage = false
    val bubbleColor =
        if (isOwnMessage) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    val textColor =
        if (isOwnMessage) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = bubbleColor,
            shape =
                if (isOwnMessage) {
                    RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
                } else {
                    RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
                },
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Sender short key
                Text(
                    text = message.senderKey.take(8) + "...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

@Composable
private fun MessageInputRow(
    text: String,
    onTextChange: (String) -> Unit,
    sending: Boolean,
    onSend: () -> Unit,
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(stringResource(R.string.marmot_type_message))
                },
                shape = RoundedCornerShape(24.dp),
                singleLine = false,
                maxLines = 4,
                enabled = !sending,
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank() && !sending,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.marmot_send),
                    tint =
                        if (text.isNotBlank() && !sending) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        },
                )
            }
        }
    }
}

private fun formatTimestamp(epochSeconds: Long): String {
    if (epochSeconds <= 0) return ""
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(epochSeconds * 1000))
}
