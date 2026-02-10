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
import com.vitorpamplona.quartz.nip01Core.core.toHexKey
import com.vitorpamplona.quartz.nip01Core.crypto.KeyPair
import com.vitorpamplona.quartz.nip01Core.relay.normalizer.RelayUrlNormalizer
import com.vitorpamplona.quartz.nip01Core.signers.NostrSignerSync
import com.vitorpamplona.quartz.nip65RelayList.AdvertisedRelayListEvent
import com.vitorpamplona.quartz.nip65RelayList.tags.AdvertisedRelayInfo
import com.vitorpamplona.quartz.nip65RelayList.tags.AdvertisedRelayType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DesktopOutboxResolverTest {
    private fun createRelayListEvent(
        signer: NostrSignerSync,
        relays: List<String>,
    ): AdvertisedRelayListEvent {
        val relayInfos =
            relays.mapNotNull { url ->
                RelayUrlNormalizer.normalizeOrNull(url)?.let {
                    AdvertisedRelayInfo(it, AdvertisedRelayType.BOTH)
                }
            }
        return AdvertisedRelayListEvent.create(list = relayInfos, signer = signer)
    }

    @Test
    fun testEmptyUserListReturnsEmptyMap() {
        val cache = DesktopLocalCache()
        val resolver = DesktopOutboxResolver(cache)
        val fallback = setOf(RelayUrlNormalizer.normalizeOrNull("wss://relay.damus.io")!!)

        val result = resolver.resolveOutboxFilters(emptyList(), listOf(1), 50, fallback)
        assertTrue(result.isEmpty())
    }

    @Test
    fun testUsersWithRelayListsRouteToWriteRelays() {
        val cache = DesktopLocalCache()
        val resolver = DesktopOutboxResolver(cache)

        val keyPair1 = KeyPair()
        val signer1 = NostrSignerSync(keyPair1)
        val pubkey1 = keyPair1.pubKey.toHexKey()

        // Create and consume relay list for user 1
        val relayList1 = createRelayListEvent(signer1, listOf("wss://relay.user1.com", "wss://nos.lol"))
        cache.consumeRelayList(relayList1)

        val fallback = setOf(RelayUrlNormalizer.normalizeOrNull("wss://relay.damus.io")!!)
        val result = resolver.resolveOutboxFilters(listOf(pubkey1), listOf(1), 50, fallback)

        // Should route to user's relays, not fallback
        assertTrue(result.isNotEmpty(), "Should have outbox-routed filters")

        // Check that user1's pubkey appears in filters for their relays
        val allAuthors = result.values.flatMap { filters -> filters.flatMap { it.authors ?: emptyList() } }
        assertTrue(pubkey1 in allAuthors, "User1 should be in routed filters")
    }

    @Test
    fun testUsersWithoutRelayListsFallBackToDefaults() {
        val cache = DesktopLocalCache()
        val resolver = DesktopOutboxResolver(cache)

        val keyPair1 = KeyPair()
        val pubkey1 = keyPair1.pubKey.toHexKey()

        // Don't add any relay list for this user
        val fallbackRelay = RelayUrlNormalizer.normalizeOrNull("wss://relay.damus.io")!!
        val fallback = setOf(fallbackRelay)

        val result = resolver.resolveOutboxFilters(listOf(pubkey1), listOf(1), 50, fallback)

        // Should fall back to default relays
        assertTrue(result.isNotEmpty(), "Should have fallback filters")
        assertNotNull(result[fallbackRelay], "Should include fallback relay")

        val authorsOnFallback = result[fallbackRelay]!!.flatMap { it.authors ?: emptyList() }
        assertTrue(pubkey1 in authorsOnFallback, "User without relay list should be on fallback relay")
    }

    @Test
    fun testMixedUsersWithAndWithoutRelayLists() {
        val cache = DesktopLocalCache()
        val resolver = DesktopOutboxResolver(cache)

        // User WITH relay list
        val keyPair1 = KeyPair()
        val signer1 = NostrSignerSync(keyPair1)
        val pubkey1 = keyPair1.pubKey.toHexKey()
        val relayList1 = createRelayListEvent(signer1, listOf("wss://relay.user1.com"))
        cache.consumeRelayList(relayList1)

        // User WITHOUT relay list
        val keyPair2 = KeyPair()
        val pubkey2 = keyPair2.pubKey.toHexKey()

        val fallbackRelay = RelayUrlNormalizer.normalizeOrNull("wss://relay.damus.io")!!
        val fallback = setOf(fallbackRelay)

        val result = resolver.resolveOutboxFilters(listOf(pubkey1, pubkey2), listOf(1), 50, fallback)

        assertTrue(result.isNotEmpty())

        // User1 should be routed to their relay
        val user1Relay = RelayUrlNormalizer.normalizeOrNull("wss://relay.user1.com")!!
        val authorsOnUser1Relay = result[user1Relay]?.flatMap { it.authors ?: emptyList() } ?: emptyList()
        assertTrue(pubkey1 in authorsOnUser1Relay, "User1 should be on their own relay")

        // User2 should be on fallback
        val authorsOnFallback = result[fallbackRelay]?.flatMap { it.authors ?: emptyList() } ?: emptyList()
        assertTrue(pubkey2 in authorsOnFallback, "User2 should be on fallback relay")
    }

    @Test
    fun testCacheReturnsSameResultForSameInput() {
        val cache = DesktopLocalCache()
        val resolver = DesktopOutboxResolver(cache)

        val keyPair1 = KeyPair()
        val signer1 = NostrSignerSync(keyPair1)
        val pubkey1 = keyPair1.pubKey.toHexKey()
        val relayList1 = createRelayListEvent(signer1, listOf("wss://relay.user1.com"))
        cache.consumeRelayList(relayList1)

        val fallback = setOf(RelayUrlNormalizer.normalizeOrNull("wss://relay.damus.io")!!)

        val result1 = resolver.resolveOutboxFilters(listOf(pubkey1), listOf(1), 50, fallback)
        val result2 = resolver.resolveOutboxFilters(listOf(pubkey1), listOf(1), 50, fallback)

        // Should return the exact same cached instance
        assertTrue(result1 === result2, "Second call should return cached result")
    }

    @Test
    fun testCacheInvalidation() {
        val cache = DesktopLocalCache()
        val resolver = DesktopOutboxResolver(cache)

        val keyPair1 = KeyPair()
        val signer1 = NostrSignerSync(keyPair1)
        val pubkey1 = keyPair1.pubKey.toHexKey()
        val relayList1 = createRelayListEvent(signer1, listOf("wss://relay.user1.com"))
        cache.consumeRelayList(relayList1)

        val fallback = setOf(RelayUrlNormalizer.normalizeOrNull("wss://relay.damus.io")!!)

        val result1 = resolver.resolveOutboxFilters(listOf(pubkey1), listOf(1), 50, fallback)
        resolver.invalidateCache()
        val result2 = resolver.resolveOutboxFilters(listOf(pubkey1), listOf(1), 50, fallback)

        // After invalidation, should be a new result (not same reference)
        assertTrue(result1 !== result2, "After invalidation, result should be recomputed")
        // But content should be equivalent
        assertEquals(result1.keys, result2.keys, "Results should have same relay keys")
    }

    @Test
    fun testRelayHintFallback() {
        val cache = DesktopLocalCache()
        val resolver = DesktopOutboxResolver(cache)

        val keyPair1 = KeyPair()
        val pubkey1 = keyPair1.pubKey.toHexKey()

        // Add relay hint for user1 (simulates having seen their events on a relay)
        val hintRelay = RelayUrlNormalizer.normalizeOrNull("wss://hint-relay.example.com")!!
        cache.relayHints.addKey(pubkey1, hintRelay)

        val fallbackRelay = RelayUrlNormalizer.normalizeOrNull("wss://relay.damus.io")!!
        val fallback = setOf(fallbackRelay)

        val result = resolver.resolveOutboxFilters(listOf(pubkey1), listOf(1), 50, fallback)

        // Should use hint relay, not just fallback
        assertTrue(result.isNotEmpty())
        val authorsOnHintRelay = result[hintRelay]?.flatMap { it.authors ?: emptyList() } ?: emptyList()
        assertTrue(pubkey1 in authorsOnHintRelay, "User should be routed to hint relay")
    }
}
