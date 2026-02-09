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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.File

/**
 * Write-behind event persistence service using LMDB.
 *
 * Events are buffered in a channel and written to LMDB asynchronously.
 * If the write queue overflows, the oldest unwritten events are dropped
 * (they remain in the in-memory LocalCache).
 *
 * Usage:
 * ```
 * val service = EventPersistenceService(scope)
 * service.start("/path/to/nostrdb")
 * // ... events come in from relays
 * service.persistEvent(event)  // non-blocking
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
    }

    fun persistEvent(event: Event) {
        if (store == null) return
        eventChannel.trySend(event)
    }

    suspend fun loadEvents(filter: Filter): List<Event> = store?.query(filter) ?: emptyList()

    suspend fun eventCount(filter: Filter): Long = store?.count(filter) ?: 0

    fun stop() {
        drainJob?.cancel()
        drainJob = null
        store?.close()
        store = null
    }
}
