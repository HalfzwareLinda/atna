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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vitorpamplona.amethyst.commons.state.EventCollectionState
import com.vitorpamplona.amethyst.commons.ui.components.EmptyState
import com.vitorpamplona.amethyst.commons.ui.components.LoadingState
import com.vitorpamplona.amethyst.desktop.account.AccountState
import com.vitorpamplona.amethyst.desktop.cache.DesktopLocalCache
import com.vitorpamplona.amethyst.desktop.network.DesktopRelayConnectionManager
import com.vitorpamplona.amethyst.desktop.subscriptions.FilterBuilders
import com.vitorpamplona.amethyst.desktop.subscriptions.SubscriptionConfig
import com.vitorpamplona.amethyst.desktop.subscriptions.generateSubId
import com.vitorpamplona.amethyst.desktop.subscriptions.rememberSubscription
import com.vitorpamplona.amethyst.desktop.ui.note.NoteCard
import com.vitorpamplona.quartz.nip01Core.core.Event
import com.vitorpamplona.quartz.nip01Core.metadata.MetadataEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun HashtagFeedScreen(
    hashtag: String,
    relayManager: DesktopRelayConnectionManager,
    localCache: DesktopLocalCache,
    account: AccountState.LoggedIn? = null,
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToThread: (String) -> Unit = {},
    onNavigateToHashtag: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val relayStatuses by relayManager.relayStatuses.collectAsState()
    val scope = rememberCoroutineScope()
    val eventState =
        remember {
            EventCollectionState<Event>(
                getId = { it.id },
                sortComparator = compareByDescending { it.createdAt },
                maxSize = 200,
                scope = scope,
            )
        }
    val events by eventState.items.collectAsState()
    var eoseReceived by remember { mutableStateOf(false) }

    rememberSubscription(relayStatuses, hashtag, relayManager = relayManager) {
        val configuredRelays = relayStatuses.keys
        if (configuredRelays.isEmpty()) return@rememberSubscription null

        SubscriptionConfig(
            subId = generateSubId("hashtag-${hashtag.take(12)}"),
            filters =
                listOf(
                    FilterBuilders.byTags(
                        tags = mapOf("t" to listOf(hashtag.lowercase())),
                        kinds = listOf(1),
                        limit = 100,
                    ),
                ),
            relays = configuredRelays,
            onEvent = { event, _, _, _ ->
                if (event is MetadataEvent) {
                    localCache.consumeMetadata(event)
                } else if (event.kind == 1) {
                    eventState.addItem(event)
                }
            },
            onEose = { _, _ ->
                scope.launch(Dispatchers.Main) { eoseReceived = true }
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with back button and hashtag name
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "#$hashtag",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

        if (events.isEmpty() && !eoseReceived) {
            LoadingState("Loading #$hashtag...")
        } else if (events.isEmpty() && eoseReceived) {
            EmptyState(
                title = "No notes found",
                description = "No notes with #$hashtag were found on connected relays.",
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(events, key = { it.id }) { event ->
                    val noteData =
                        remember(event.id) {
                            event.toNoteDisplayData(localCache)
                        }

                    NoteCard(
                        note = noteData,
                        localCache = localCache,
                        onAuthorClick = onNavigateToProfile,
                        onMentionClick = onNavigateToProfile,
                        onNoteClick = onNavigateToThread,
                        onHashtagClick = onNavigateToHashtag,
                        onClick = { onNavigateToThread(event.id) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                }
            }
        }
    }
}
