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
import com.vitorpamplona.quartz.utils.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * Write-behind event persistence service using LMDB.
 *
 * Events are buffered in a channel and written to LMDB asynchronously.
 * Only events of useful kinds are persisted (profiles, notes, DMs,
 * reactions, zaps, trust assertions, user lists, etc.).
 *
 * Periodically prunes old events and enforces a size limit via
 * [LmdbSizeManager] with tiered pruning (normal / moderate / aggressive).
 *
 * Usage:
 * ```
 * val service = EventPersistenceService(scope, maxSizeMB = 4096)
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
    private val maxSizeMB: Int = 4096,
) {
    @Volatile
    private var store: LmdbEventStore? = null

    @Volatile
    private var sizeManager: LmdbSizeManager? = null

    /** Set to true during wipe to stop background jobs from touching LMDB. */
    @Volatile
    private var wiping = false
    private var dbPath: String? = null
    private var eventChannel = Channel<Event>(capacity = 2000, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var drainJob: Job? = null
    private var pruneJob: Job? = null

    val isRunning: Boolean get() = store != null

    /**
     * Returns (currentSizeMB, maxSizeMB) or null if the service is not running.
     */
    fun getDbSizeInfo(): Pair<Long, Int>? {
        val sm = sizeManager ?: return null
        return sm.getDbSizeMB() to maxSizeMB
    }

    fun start(dbPath: String) {
        if (store != null) return

        this.dbPath = dbPath
        File(dbPath).mkdirs()

        val lmdb = LmdbEventStore(dbPath)
        lmdb.open()
        store = lmdb
        sizeManager = LmdbSizeManager(dbPath, maxSizeMB, lmdb)

        drainJob =
            scope.launch(Dispatchers.IO) {
                val batch = mutableListOf<Event>()
                var insertsSinceCheck = 0
                for (event in eventChannel) {
                    if (wiping) break
                    batch.add(event)
                    // Drain up to BATCH_SIZE events from the channel without suspending
                    while (batch.size < BATCH_SIZE) {
                        val next = eventChannel.tryReceive().getOrNull() ?: break
                        batch.add(next)
                    }
                    try {
                        if (!wiping) {
                            lmdb.insertBatch(batch)
                            insertsSinceCheck += batch.size
                        }
                    } catch (e: Exception) {
                        if (!wiping) {
                            Log.w(TAG, "LMDB batch insert failed (${batch.size} events): ${e.message}", e)
                        }
                    }
                    batch.clear()

                    // Proactive size check every SIZE_CHECK_INTERVAL inserts
                    if (insertsSinceCheck >= SIZE_CHECK_INTERVAL) {
                        insertsSinceCheck = 0
                        try {
                            if (!wiping) {
                                sizeManager?.let { sm ->
                                    if (sm.isOverLimit()) {
                                        Log.d(TAG, "Proactive size check: over limit, enforcing")
                                        sm.enforceSizeLimit()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Proactive size enforcement failed: ${e.message}", e)
                        }
                    }
                }
            }

        pruneJob =
            scope.launch(Dispatchers.IO) {
                // Wait a bit before first prune to let startup finish
                delay(60_000L)
                while (true) {
                    if (wiping) break
                    try {
                        sizeManager?.enforceSizeLimit() ?: prune()
                    } catch (e: Exception) {
                        if (!wiping) {
                            Log.w(TAG, "LMDB prune failed: ${e.message}", e)
                        }
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
     * Strategy (by age, inspired by Damus/Notedeck analysis):
     * - Engagement (reposts, reactions, zaps, live chat, highlights): 14 days
     * - Content (notes, media, channels, Marmot, live activities, status): 60 days
     * - Private/long-lived (DMs, gift wraps, long-form, trust assertions): 180 days
     *
     * Never deletes: profiles, contact lists, relay lists, mute lists,
     *   trust provider lists, interest lists, user lists, pin lists
     */
    suspend fun prune() {
        sizeManager?.normalPrune() ?: run {
            val lmdb = store ?: return
            val now =
                com.vitorpamplona.quartz.utils.TimeUtils
                    .now()
            lmdb.delete(Filter(kinds = ENGAGEMENT_KINDS.toList(), until = now - 14 * DAY_SECS))
            lmdb.delete(Filter(kinds = CONTENT_KINDS.toList(), until = now - 60 * DAY_SECS))
            lmdb.delete(Filter(kinds = PRIVATE_KINDS.toList(), until = now - 180 * DAY_SECS))
        }
    }

    /**
     * Wipes all data from LMDB and restarts the service.
     * Used by the "Clear local database" settings option.
     *
     * Instead of calling NostrDatabase.wipe() (which fails with MDB_BAD_DBI
     * when internal DBI handles are stale), we close the database cleanly
     * and delete the LMDB files from disk, then reopen a fresh database.
     */
    suspend fun wipeAndRestart() {
        val path = dbPath ?: return
        // 1. Signal background jobs to stop touching LMDB immediately
        wiping = true
        // 2. Cancel prune job
        pruneJob?.cancel()
        pruneJob = null
        // 3. Close channel so drain loop's for-loop exits, then wait + cancel
        eventChannel.close()
        withTimeoutOrNull(DRAIN_TIMEOUT_MS) { drainJob?.join() }
        drainJob?.cancel()
        drainJob?.join() // ensure coroutine has fully exited
        drainJob = null
        // 4. Close the database before deleting files
        store?.close()
        store = null
        sizeManager = null
        dbPath = null
        // 5. Delete LMDB files from disk
        val dbDir = File(path)
        File(dbDir, "data.mdb").delete()
        File(dbDir, "lock.mdb").delete()
        Log.d(TAG, "LMDB files deleted from $path")
        // 6. Reset state and restart fresh
        wiping = false
        eventChannel = Channel(capacity = 2000, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        start(path)
    }

    fun stop() {
        pruneJob?.cancel()
        pruneJob = null
        // Close the channel so the drain loop finishes after processing remaining events,
        // then wait up to 5 seconds for it to complete.
        eventChannel.close()
        runBlocking {
            withTimeoutOrNull(DRAIN_TIMEOUT_MS) {
                drainJob?.join()
            }
        }
        drainJob?.cancel()
        drainJob = null
        store?.close()
        store = null
        sizeManager = null
        dbPath = null
        // Create a fresh channel for potential restart
        eventChannel = Channel(capacity = 2000, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    companion object {
        private const val TAG = "EventPersistenceService"
        internal const val DAY_SECS = 86400L
        private const val PRUNE_INTERVAL_MS = 6 * 60 * 60 * 1000L // 6 hours
        private const val DRAIN_TIMEOUT_MS = 5_000L
        private const val BATCH_SIZE = 100
        private const val SIZE_CHECK_INTERVAL = 1000

        /**
         * Kind groups organized by pruning tier. Inspired by Damus/Notedeck
         * nostrdb analysis: both store everything, but we keep kind-based
         * filtering with generous retention for a desktop app.
         *
         * Engagement kinds (14-day retention): transient social signals.
         */
        internal val ENGAGEMENT_KINDS =
            setOf(
                6, // Reposts
                7, // Reactions
                16, // Generic reposts
                1311, // Live chat messages (NIP-53)
                9734, // Zap requests
                9735, // Zap receipts
                9802, // Highlights (NIP-84)
            )

        /**
         * Content kinds (60-day retention): primary content and media.
         */
        internal val CONTENT_KINDS =
            setOf(
                1, // Text notes
                5, // Deletion events
                20, // Pictures — short-form (NIP-68)
                21, // Pictures — long-form (NIP-68)
                22, // Picture comments (NIP-68)
                40, // Channel create
                41, // Channel metadata
                42, // Channel message
                443, // Marmot key package
                444, // Marmot welcome
                445, // Marmot group
                1063, // File metadata (NIP-94)
                30311, // Live activities (NIP-53)
                30315, // User status (NIP-38)
                34235, // Videos — short-form (NIP-71)
                34236, // Videos — long-form (NIP-71)
            )

        /**
         * Private / long-lived kinds (180-day retention): DMs, long-form, trust.
         */
        internal val PRIVATE_KINDS =
            setOf(
                4, // Encrypted DMs (legacy)
                13, // Sealed DMs
                14, // Chat messages (NIP-17)
                15, // Group chat messages
                1059, // Gift wrap
                30023, // Long-form content
                30382, // Contact cards / trust assertions (NIP-85)
            )

        /**
         * Metadata & list kinds (never pruned under normal conditions).
         */
        internal val METADATA_KINDS =
            setOf(
                0, // Profiles (metadata)
                3, // Contact lists
                10000, // Mute list
                10002, // Relay lists (NIP-65)
                10015, // Interest list (NIP-51)
                10040, // Trust provider lists (NIP-85)
                10050, // Chat message relay list
                10051, // Marmot key package relay list
                30000, // People lists
                30001, // Bookmark lists
                30002, // Relay sets
                30003, // Labeled bookmark lists
                33888, // Pin lists
            )

        /**
         * All 44 event kinds worth persisting to LMDB.
         * Ephemeral events (20000-29999) are always skipped regardless.
         */
        private val PERSISTED_KINDS = ENGAGEMENT_KINDS + CONTENT_KINDS + PRIVATE_KINDS + METADATA_KINDS

        private fun shouldPersist(event: Event): Boolean {
            val kind = event.kind
            // Always skip ephemeral events
            if (kind in 20000 until 30000) return false
            return kind in PERSISTED_KINDS
        }
    }
}
