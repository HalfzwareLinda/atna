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
package com.vitorpamplona.amethyst.desktop.ui.note

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vitorpamplona.amethyst.commons.ui.components.UserAvatar
import com.vitorpamplona.amethyst.commons.util.toTimeAgo
import com.vitorpamplona.amethyst.desktop.cache.DesktopLocalCache
import com.vitorpamplona.amethyst.desktop.network.RelayConnectionManager
import com.vitorpamplona.amethyst.desktop.ui.toNoteDisplayData
import com.vitorpamplona.quartz.nip01Core.core.Event
import com.vitorpamplona.quartz.nip01Core.relay.client.reqs.IRequestListener
import com.vitorpamplona.quartz.nip01Core.relay.filters.Filter
import com.vitorpamplona.quartz.nip01Core.relay.normalizer.NormalizedRelayUrl
import com.vitorpamplona.quartz.nip01Core.relay.normalizer.RelayUrlNormalizer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * Cache for fetched embedded note events.
 */
object EmbeddedEventCache {
    private val cache = ConcurrentHashMap<String, Event>()
    private val pending = ConcurrentHashMap.newKeySet<String>()

    fun get(eventIdHex: String): Event? = cache[eventIdHex]

    fun put(
        eventIdHex: String,
        event: Event,
    ) {
        cache[eventIdHex] = event
        pending.remove(eventIdHex)
    }

    fun isPending(eventIdHex: String): Boolean = pending.contains(eventIdHex)

    fun markPending(eventIdHex: String): Boolean = pending.add(eventIdHex)
}

/**
 * Fetches an event by ID from connected relays using a one-shot subscription.
 * Returns the event if found within the timeout, null otherwise.
 */
private suspend fun fetchEventFromRelays(
    eventIdHex: String,
    relayHint: String?,
    relayManager: RelayConnectionManager,
): Event? =
    withTimeoutOrNull(5_000) {
        suspendCancellableCoroutine { continuation ->
            val connectedRelays = relayManager.connectedRelays.value
            if (connectedRelays.isEmpty()) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val subId = "emb-${eventIdHex.take(8)}"
            var resumed = false

            // Build relay set: hint relay + connected relays
            val relays =
                buildSet {
                    addAll(connectedRelays)
                    if (relayHint != null) {
                        runCatching { RelayUrlNormalizer.normalize(relayHint) }
                            .getOrNull()
                            ?.let { add(it) }
                    }
                }

            val filter =
                Filter(
                    ids = listOf(eventIdHex),
                    limit = 1,
                )

            relayManager.subscribe(
                subId = subId,
                filters = listOf(filter),
                relays = relays,
                listener =
                    object : IRequestListener {
                        override fun onEvent(
                            event: Event,
                            isLive: Boolean,
                            relay: NormalizedRelayUrl,
                            forFilters: List<Filter>?,
                        ) {
                            if (event.id == eventIdHex && !resumed) {
                                resumed = true
                                EmbeddedEventCache.put(eventIdHex, event)
                                relayManager.unsubscribe(subId)
                                if (continuation.isActive) {
                                    continuation.resume(event)
                                }
                            }
                        }
                    },
            )

            continuation.invokeOnCancellation {
                relayManager.unsubscribe(subId)
            }
        }
    }

/**
 * Renders an embedded/quoted note card for nevent1 and note1 references.
 * Shows author, timestamp, and truncated content in a bordered card.
 * Fetches from relays if the event is not in the local cache.
 */
@Composable
fun EmbeddedNoteCard(
    eventIdHex: String,
    relayHint: String? = null,
    localCache: DesktopLocalCache? = null,
    relayManager: RelayConnectionManager? = null,
    onNoteClick: ((String) -> Unit)? = null,
    onMentionClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val event by
        produceState<Event?>(
            initialValue = EmbeddedEventCache.get(eventIdHex) ?: localCache?.getNoteIfExists(eventIdHex)?.event,
            key1 = eventIdHex,
        ) {
            // If already have the event, we're done
            if (value != null) return@produceState

            // Check local cache for the note
            val cachedNote = localCache?.getNoteIfExists(eventIdHex)
            if (cachedNote?.event != null) {
                EmbeddedEventCache.put(eventIdHex, cachedNote.event!!)
                value = cachedNote.event
                return@produceState
            }

            // Fetch from relays if relay manager is available
            if (relayManager != null && EmbeddedEventCache.markPending(eventIdHex)) {
                val fetched = fetchEventFromRelays(eventIdHex, relayHint, relayManager)
                if (fetched != null) {
                    value = fetched
                }
            }
        }

    val borderColor = MaterialTheme.colorScheme.outlineVariant

    if (event != null) {
        val noteData = event!!.toNoteDisplayData(localCache)

        // Observe user metadata reactively — updates when profile (kind 0) loads
        val userInfo by noteData.user
            ?.metadataOrNull()
            ?.flow
            ?.collectAsState()
            ?: remember { mutableStateOf(null) }

        val displayName = userInfo?.info?.bestName() ?: noteData.pubKeyDisplay
        val pictureUrl = userInfo?.info?.picture ?: noteData.profilePictureUrl

        Column(
            modifier =
                modifier
                    .fillMaxWidth()
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                    .clickable { onNoteClick?.invoke(eventIdHex) }
                    .padding(12.dp),
        ) {
            // Compact author header
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UserAvatar(
                    userHex = noteData.pubKeyHex,
                    pictureUrl = pictureUrl,
                    size = 20.dp,
                    contentDescription = "Profile picture",
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = noteData.createdAt.toTimeAgo(withDot = true),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            // Content (truncated)
            if (noteData.content.isNotEmpty()) {
                Text(
                    text = noteData.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    } else {
        // Loading placeholder with spinner
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                modifier
                    .fillMaxWidth()
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                    .clickable { onNoteClick?.invoke(eventIdHex) }
                    .padding(12.dp),
        ) {
            if (relayManager != null) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = "nevent:${eventIdHex.take(8)}...${eventIdHex.takeLast(8)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
