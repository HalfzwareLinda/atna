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
package com.vitorpamplona.amethyst.desktop.network

import com.vitorpamplona.amethyst.desktop.cache.DesktopLocalCache
import com.vitorpamplona.quartz.nip01Core.core.HexKey
import com.vitorpamplona.quartz.nip01Core.relay.filters.Filter
import com.vitorpamplona.quartz.nip01Core.relay.normalizer.NormalizedRelayUrl
import com.vitorpamplona.quartz.nip65RelayList.RelayListRecommendationProcessor

/**
 * Desktop NIP-65 outbox model resolver.
 *
 * Routes subscription filters to each user's WRITE relays using the greedy
 * set-cover algorithm from [RelayListRecommendationProcessor]. Users without
 * relay lists fall back to hint relays from the [DesktopLocalCache.relayHints]
 * bloom filter, then to the provided fallback relays.
 *
 * Results are cached (with 5-minute TTL) and invalidated when relay lists change.
 * All cache access is synchronized for thread safety.
 */
class DesktopOutboxResolver(
    private val localCache: DesktopLocalCache,
) {
    private val cacheLock = Any()

    @Volatile
    private var cachedUserSet: Set<String>? = null

    @Volatile
    private var cachedResult: Map<NormalizedRelayUrl, List<Filter>>? = null

    @Volatile
    private var cachedKinds: List<Int>? = null

    @Volatile
    private var cachedLimit: Int = 0

    @Volatile
    private var cachedTimestamp: Long = 0L

    private companion object {
        const val CACHE_TTL_MS = 5L * 60 * 1000 // 5 minutes
    }

    /**
     * Invalidates the cache. Call when relay lists (kind 10002) change.
     */
    fun invalidateCache() {
        synchronized(cacheLock) {
            cachedResult = null
            cachedUserSet = null
            cachedTimestamp = 0L
        }
    }

    /**
     * Resolves outbox-routed filters for a set of followed users.
     *
     * @param userPubkeys Pubkeys to route
     * @param kinds Event kinds to filter for
     * @param limit Per-filter limit
     * @param fallbackRelays Relays to use for users without relay lists or hints
     * @return Map of relay -> filters, ready for [INostrClient.openReqSubscription]
     */
    fun resolveOutboxFilters(
        userPubkeys: List<String>,
        kinds: List<Int>,
        limit: Int,
        fallbackRelays: Set<NormalizedRelayUrl>,
    ): Map<NormalizedRelayUrl, List<Filter>> {
        synchronized(cacheLock) {
            val userSet = userPubkeys.toSet()
            val cached = cachedResult
            val now = System.currentTimeMillis()
            if (cached != null &&
                userSet == cachedUserSet &&
                kinds == cachedKinds &&
                limit == cachedLimit &&
                (now - cachedTimestamp) < CACHE_TTL_MS
            ) {
                return cached
            }
        }

        val usersAndRelays = mutableMapOf<HexKey, Set<NormalizedRelayUrl>>()
        val noOutboxUsers = mutableListOf<HexKey>()

        userPubkeys.forEach { pubkey ->
            val user = localCache.getUserIfExists(pubkey)
            val writeRelays = user?.authorRelayList()?.writeRelaysNorm()
            if (writeRelays != null && writeRelays.isNotEmpty()) {
                usersAndRelays[pubkey] = writeRelays.toSet()
            } else {
                noOutboxUsers.add(pubkey)
            }
        }

        val result = mutableMapOf<NormalizedRelayUrl, MutableList<Filter>>()

        // Use greedy set-cover for users WITH relay lists
        if (usersAndRelays.isNotEmpty()) {
            val recommendations =
                RelayListRecommendationProcessor.reliableRelaySetFor(usersAndRelays)

            recommendations.forEach { rec ->
                val authors = rec.users.toList().sorted()
                if (authors.isNotEmpty()) {
                    result
                        .getOrPut(rec.relay) { mutableListOf() }
                        .add(Filter(kinds = kinds, authors = authors, limit = limit))
                }
            }
        }

        // Users without outbox: try hint relays, then fallback
        if (noOutboxUsers.isNotEmpty()) {
            val coveredByHints = mutableSetOf<HexKey>()

            noOutboxUsers.forEach { pubkey ->
                val hintRelays = localCache.relayHints.hintsForKey(pubkey)
                if (hintRelays.isNotEmpty()) {
                    coveredByHints.add(pubkey)
                    hintRelays.forEach { relay ->
                        result
                            .getOrPut(relay) { mutableListOf() }
                            .add(Filter(kinds = kinds, authors = listOf(pubkey), limit = limit))
                    }
                }
            }

            // Remaining users (no relay list AND no hints) go to all fallback relays
            val uncovered = noOutboxUsers.filter { it !in coveredByHints }.sorted()
            if (uncovered.isNotEmpty()) {
                fallbackRelays.forEach { relay ->
                    result
                        .getOrPut(relay) { mutableListOf() }
                        .add(Filter(kinds = kinds, authors = uncovered, limit = limit))
                }
            }
        }

        // Cache the result
        synchronized(cacheLock) {
            cachedUserSet = userPubkeys.toSet()
            cachedKinds = kinds
            cachedLimit = limit
            cachedTimestamp = System.currentTimeMillis()
            cachedResult = result
        }

        return result
    }
}
