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
package com.vitorpamplona.amethyst.desktop.subscriptions

import com.vitorpamplona.amethyst.commons.model.Note
import com.vitorpamplona.amethyst.commons.relayClient.assemblers.FeedMetadataCoordinator
import com.vitorpamplona.amethyst.commons.relayClient.preload.ImagePrefetcher
import com.vitorpamplona.amethyst.commons.relayClient.preload.MetadataPreloader
import com.vitorpamplona.amethyst.commons.relayClient.preload.MetadataRateLimiter
import com.vitorpamplona.amethyst.desktop.cache.DesktopLocalCache
import com.vitorpamplona.quartz.experimental.relationshipStatus.ContactCardEvent
import com.vitorpamplona.quartz.nip01Core.core.HexKey
import com.vitorpamplona.quartz.nip01Core.metadata.MetadataEvent
import com.vitorpamplona.quartz.nip01Core.relay.client.INostrClient
import com.vitorpamplona.quartz.nip01Core.relay.client.single.newSubId
import com.vitorpamplona.quartz.nip01Core.relay.filters.Filter
import com.vitorpamplona.quartz.nip01Core.relay.normalizer.NormalizedRelayUrl
import kotlinx.coroutines.CoroutineScope

/**
 * Desktop-specific relay subscriptions coordinator.
 * Manages metadata and reactions loading with rate limiting and prioritization.
 *
 * This coordinator ensures:
 * - Display names and avatars load before reactions
 * - Metadata requests are rate-limited (20/sec) to avoid relay flooding
 * - Subscriptions are batched efficiently
 *
 * Usage:
 * ```
 * val coordinator = DesktopRelaySubscriptionsCoordinator(
 *     client = relayManager.client,
 *     scope = viewModelScope,
 *     indexRelaysProvider = { relayManager.availableRelays.value },
 * )
 * coordinator.start()
 *
 * // In screens:
 * LaunchedEffect(notes) {
 *     coordinator.loadMetadataForNotes(notes)
 * }
 * ```
 */
class DesktopRelaySubscriptionsCoordinator(
    private val client: INostrClient,
    private val scope: CoroutineScope,
    private val indexRelaysProvider: () -> Set<NormalizedRelayUrl>,
    private val localCache: DesktopLocalCache,
) {
    // Rate limiter: 20 requests per second to avoid flooding relays
    private val rateLimiter = MetadataRateLimiter(maxRequestsPerSecond = 20, scope = scope)

    // Avatar image prefetcher using Coil's singleton loader
    private val imagePrefetcher =
        object : ImagePrefetcher {
            override fun prefetch(url: String) {
                val context = coil3.PlatformContext.INSTANCE
                val loader = coil3.SingletonImageLoader.get(context)
                val request =
                    coil3.request.ImageRequest
                        .Builder(context)
                        .data(url)
                        .size(64, 64)
                        .build()
                loader.enqueue(request)
            }
        }

    // Preloader handles metadata + avatar prefetching
    private val preloader = MetadataPreloader(rateLimiter, imagePrefetcher = imagePrefetcher)

    // Feed metadata coordinator with priority queue
    val feedMetadata =
        FeedMetadataCoordinator(
            client = client,
            scope = scope,
            indexRelaysProvider = indexRelaysProvider,
            preloader = preloader,
            onEvent = { event, _ ->
                // Consume metadata events into local cache
                if (event is MetadataEvent) {
                    localCache.consumeMetadata(event)
                }
            },
        )

    /**
     * Start the coordinator.
     * Call once when app starts or user logs in.
     */
    fun start() {
        // Start rate limiter with batched processing — sends one subscription
        // per batch (up to 20 pubkeys) instead of one subscription per pubkey
        rateLimiter.startBatched { pubkeys ->
            if (pubkeys.isEmpty()) return@startBatched
            val relays = indexRelaysProvider()
            if (relays.isEmpty()) return@startBatched
            client.openReqSubscription(
                filters =
                    relays.associateWith {
                        listOf(
                            com.vitorpamplona.quartz.nip01Core.relay.filters.Filter(
                                kinds = listOf(com.vitorpamplona.quartz.nip01Core.metadata.MetadataEvent.KIND),
                                authors = pubkeys,
                                limit = pubkeys.size,
                            ),
                        )
                    },
            )
        }

        // Start feed metadata coordinator
        feedMetadata.start()
    }

    /**
     * Load metadata and reactions for notes.
     * Delegates to FeedMetadataCoordinator.
     */
    fun loadMetadataForNotes(notes: List<Note>) {
        feedMetadata.loadMetadataForNotes(notes)
    }

    /**
     * Load metadata for specific pubkeys.
     * Filters out pubkeys that already have metadata in cache to avoid
     * wasted subscriptions, and retries previously-queued pubkeys whose
     * metadata never arrived.
     */
    fun loadMetadataForPubkeys(pubkeys: List<HexKey>) {
        val unresolved =
            pubkeys.filter { pk ->
                localCache.getUserIfExists(pk)?.metadataOrNull() == null
            }
        if (unresolved.isEmpty()) return

        // Allow retry for pubkeys that were queued before but never resolved
        feedMetadata.clearQueued(unresolved)
        feedMetadata.loadMetadataForPubkeys(unresolved)
    }

    /**
     * Load reactions for specific notes.
     */
    fun loadReactionsForNotes(noteIds: List<HexKey>) {
        feedMetadata.loadReactionsForNotes(noteIds)
    }

    /**
     * Load trust provider list for a user (kind 10040).
     * Fetches via index relays through FeedMetadataCoordinator.
     */
    fun loadTrustProviderList(pubkey: HexKey) {
        feedMetadata.loadTrustProviderList(pubkey)
    }

    /**
     * Load contact cards (kind 30382) from a trust provider's relay.
     * Subscribes directly to the provider's relay with "d" tag targeting
     * the requested pubkeys, mirroring Android's UserCardsSubAssembler pattern.
     *
     * @param targetPubkeys Pubkeys of users to get trust cards for
     * @param providerRelay The trust provider's relay URL (e.g. wss://nip85.brainstorm.world)
     * @param providerPubkey The trust provider's pubkey
     */
    fun loadContactCardsFromProvider(
        targetPubkeys: List<HexKey>,
        providerRelay: NormalizedRelayUrl,
        providerPubkey: HexKey,
    ) {
        if (targetPubkeys.isEmpty()) return

        val filter =
            Filter(
                kinds = listOf(ContactCardEvent.KIND),
                authors = listOf(providerPubkey),
                tags = mapOf("d" to targetPubkeys.sorted()),
            )

        client.openReqSubscription(
            subId = newSubId(),
            filters = mapOf(providerRelay to listOf(filter)),
        )
    }

    /**
     * Load NIP-65 relay lists (kind 10002) for followed users who don't yet
     * have relay lists in cache. This bootstraps the outbox model data so
     * subsequent feed subscriptions can route filters to the correct relays.
     */
    fun loadRelayLists(pubkeys: List<HexKey>) {
        val missing = pubkeys.filter { localCache.getUserIfExists(it)?.authorRelayList() == null }
        if (missing.isEmpty()) return

        val relays = indexRelaysProvider()
        if (relays.isEmpty()) return

        client.openReqSubscription(
            subId = newSubId(),
            filters =
                relays.associateWith {
                    listOf(
                        Filter(
                            kinds = listOf(com.vitorpamplona.quartz.nip65RelayList.AdvertisedRelayListEvent.KIND),
                            authors = missing.sorted(),
                            limit = missing.size,
                        ),
                    )
                },
        )
    }

    /**
     * Clear all queued requests.
     * Call when switching accounts or during cleanup.
     */
    fun clear() {
        feedMetadata.clear()
        rateLimiter.reset()
    }
}
