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
package com.atna.ndb

import com.vitorpamplona.quartz.nip01Core.core.Event
import com.vitorpamplona.quartz.nip01Core.relay.filters.Filter
import com.vitorpamplona.quartz.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Write-behind event persistence service using LMDB.
 *
 * Events are buffered in a channel and written to LMDB asynchronously.
 * Only events of useful kinds are persisted (profiles, notes, DMs,
 * reactions, zaps, trust assertions, user lists, etc.).
 *
 * Periodically prunes old events to keep the database size reasonable.
 *
 * Usage:
 * ```
 * val service = EventPersistenceService(scope)
 * service.start("/path/to/nostrdb")
 * // ... events come in from relays
 * service.persistEvent(event)  // non-blocking, filters by kind
 * // ... on next startup
 * val events = service.loadEvents(filter)
 * // ... on shutdown
 * service.stop()
 * ```
 */
class EventPersistenceService(
    private val scope: CoroutineScope,
) {
    private var store: LmdbEventStore? = null
    private val eventChannel = Channel<Event>(capacity = 1000, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var drainJob: Job? = null
    private var pruneJob: Job? = null

    val isRunning: Boolean get() = store != null

    fun start(dbPath: String) {
        if (store != null) return

        File(dbPath).mkdirs()

        val lmdb = LmdbEventStore(dbPath)
        lmdb.open()
        store = lmdb

        drainJob =
            scope.launch(Dispatchers.IO) {
                for (event in eventChannel) {
                    try {
                        lmdb.insert(event)
                    } catch (e: Exception) {
                        // Log but don't crash — LMDB write failures shouldn't
                        // affect the rest of the app
                    }
                }
            }

        pruneJob =
            scope.launch(Dispatchers.IO) {
                // Wait a bit before first prune to let startup finish
                delay(60_000L)
                while (true) {
                    try {
                        prune()
                    } catch (e: Exception) {
                        // Log but don't crash
                    }
                    delay(PRUNE_INTERVAL_MS)
                }
            }
    }

    fun persistEvent(event: Event) {
        if (store == null) return
        if (!shouldPersist(event)) return
        eventChannel.trySend(event)
    }

    suspend fun loadEvents(filter: Filter): List<Event> = store?.query(filter) ?: emptyList()

    suspend fun eventCount(filter: Filter): Long = store?.count(filter) ?: 0

    /**
     * Prunes old events from LMDB to keep the database size reasonable.
     *
     * Strategy (by age):
     * - Reposts (kind 6, 16), reactions (7), zaps (9734/9735): delete after 7 days
     * - Notes (kind 1): delete after 30 days
     * - Deletion events (kind 5): delete after 30 days
     * - Marmot events (443-445): delete after 30 days
     * - Channel events (40-42): delete after 30 days
     * - DMs/gift wraps (kind 4, 13, 14, 15, 1059): delete after 90 days
     * - Long-form (kind 30023): delete after 90 days
     * - Contact cards / trust assertions (kind 30382): delete after 90 days
     *
     * Never deletes: profiles (0), contact lists (3), relay lists (10002),
     *   trust provider lists (10040), mute lists (10000), chat relay lists (10050),
     *   Marmot relay lists (10051), people lists (30000), bookmark lists (30001),
     *   relay sets (30002), labeled bookmarks (30003), pin lists (33888)
     */
    suspend fun prune() {
        val lmdb = store ?: return
        val now = TimeUtils.now()

        // Reposts and generic reposts older than 7 days
        lmdb.delete(Filter(kinds = listOf(6, 16), until = now - 7 * DAY_SECS))

        // Reactions older than 7 days
        lmdb.delete(Filter(kinds = listOf(7), until = now - 7 * DAY_SECS))

        // Zap events older than 7 days
        lmdb.delete(Filter(kinds = listOf(9734, 9735), until = now - 7 * DAY_SECS))

        // Notes older than 30 days
        lmdb.delete(Filter(kinds = listOf(1), until = now - 30 * DAY_SECS))

        // Deletion events older than 30 days
        lmdb.delete(Filter(kinds = listOf(5), until = now - 30 * DAY_SECS))

        // Marmot events older than 30 days
        lmdb.delete(Filter(kinds = listOf(443, 444, 445), until = now - 30 * DAY_SECS))

        // Channel events older than 30 days
        lmdb.delete(Filter(kinds = listOf(40, 41, 42), until = now - 30 * DAY_SECS))

        // DMs and gift wraps older than 90 days
        lmdb.delete(Filter(kinds = listOf(4, 13, 14, 15, 1059), until = now - 90 * DAY_SECS))

        // Long-form content older than 90 days
        lmdb.delete(Filter(kinds = listOf(30023), until = now - 90 * DAY_SECS))

        // Contact cards (trust assertions) older than 90 days
        lmdb.delete(Filter(kinds = listOf(30382), until = now - 90 * DAY_SECS))
    }

    fun stop() {
        pruneJob?.cancel()
        pruneJob = null
        drainJob?.cancel()
        drainJob = null
        store?.close()
        store = null
    }

    companion object {
        private const val DAY_SECS = 86400L
        private const val PRUNE_INTERVAL_MS = 6 * 60 * 60 * 1000L // 6 hours

        /**
         * Event kinds worth persisting to LMDB. Ephemeral events (20000-29999)
         * are always skipped. Reactions, zaps, and reposts are kept with short
         * retention (7 days). User lists and trust provider config are kept forever.
         */
        private val PERSISTED_KINDS =
            setOf(
                0, // Profiles (metadata)
                1, // Text notes
                3, // Contact lists
                4, // Encrypted DMs (legacy)
                5, // Deletion events
                6, // Reposts
                7, // Reactions
                13, // Sealed DMs
                14, // Chat messages (NIP-17)
                15, // Group chat messages
                16, // Generic reposts
                40, // Channel create
                41, // Channel metadata
                42, // Channel message
                443, // Marmot key package
                444, // Marmot welcome
                445, // Marmot group
                1059, // Gift wrap
                9734, // Zap requests
                9735, // Zap receipts
                10000, // Mute list
                10002, // Relay lists
                10040, // Trust provider lists (NIP-85)
                10050, // Chat message relay list
                10051, // Marmot key package relay list
                30000, // People lists
                30001, // Bookmark lists
                30002, // Relay sets
                30003, // Labeled bookmark lists
                30023, // Long-form content
                30382, // Contact cards / trust assertions (NIP-85)
                33888, // Pin lists
            )

        private fun shouldPersist(event: Event): Boolean {
            val kind = event.kind
            // Always skip ephemeral events
            if (kind in 20000 until 30000) return false
            return kind in PERSISTED_KINDS
        }
    }
}
