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
import rust.nostr.sdk.Alphabet
import rust.nostr.sdk.Coordinate
import rust.nostr.sdk.EventId
import rust.nostr.sdk.NostrDatabase
import rust.nostr.sdk.PublicKey
import rust.nostr.sdk.SingleLetterTag
import rust.nostr.sdk.Timestamp
import rust.nostr.sdk.Event as RustEvent
import rust.nostr.sdk.Filter as RustFilter
import rust.nostr.sdk.Kind as RustKind

/**
 * LMDB-backed event store using rust-nostr SDK's NostrDatabase.
 *
 * Uses JSON as the bridge format between Amethyst's Event/Filter types
 * and rust-nostr's Event/Filter types.
 *
 * @param dbPath Absolute path to the LMDB database directory
 */
class LmdbEventStore(
    private val dbPath: String,
) : NostrEventStore {
    @Volatile
    private var db: NostrDatabase? = null

    fun open() {
        db = NostrDatabase.lmdb(dbPath)
    }

    private fun requireDb(): NostrDatabase = db ?: throw IllegalStateException("LmdbEventStore not opened. Call open() first.")

    override suspend fun insert(event: Event): Boolean {
        val rustEvent = RustEvent.fromJson(event.toJson())
        val status = requireDb().saveEvent(rustEvent)
        return status.isSuccess()
    }

    /**
     * Inserts a batch of events, converting JSON once per event.
     * Reduces per-event coroutine overhead compared to individual insert() calls.
     */
    suspend fun insertBatch(events: List<Event>): Int {
        val database = requireDb()
        var count = 0
        events.forEach { event ->
            try {
                val rustEvent = RustEvent.fromJson(event.toJson())
                if (database.saveEvent(rustEvent).isSuccess()) count++
            } catch (_: Exception) {
                // Skip individual failures, continue batch
            }
        }
        return count
    }

    override suspend fun query(filter: Filter): List<Event> {
        val rustFilter = filter.toRustFilter()
        val events = requireDb().query(rustFilter)
        return events.toVec().map { Event.fromJson(it.asJson()) }
    }

    override suspend fun query(filters: List<Filter>): List<Event> {
        val seen = LinkedHashSet<String>()
        val result = mutableListOf<Event>()
        for (filter in filters) {
            for (event in query(filter)) {
                if (seen.add(event.id)) {
                    result.add(event)
                }
            }
        }
        return result
    }

    override suspend fun count(filter: Filter): Long {
        val rustFilter = filter.toRustFilter()
        return requireDb().count(rustFilter).toLong()
    }

    override suspend fun delete(filter: Filter) {
        requireDb().delete(filter.toRustFilter())
    }

    override suspend fun wipe() {
        requireDb().wipe()
    }

    override fun close() {
        db?.close()
        db = null
    }
}

/**
 * Converts an Amethyst Filter to a rust-nostr Filter.
 */
internal fun Filter.toRustFilter(): RustFilter {
    var f = RustFilter()

    ids?.let { idList ->
        f = f.ids(idList.map { EventId.parse(it) })
    }

    authors?.let { authorList ->
        f = f.authors(authorList.map { PublicKey.parse(it) })
    }

    kinds?.let { kindList ->
        f = f.kinds(kindList.map { RustKind(it.toUShort()) })
    }

    since?.let { f = f.since(Timestamp.fromSecs(it.toULong())) }
    until?.let { f = f.until(Timestamp.fromSecs(it.toULong())) }
    limit?.let { f = f.limit(it.toULong()) }
    search?.let { f = f.search(it) }

    tags?.let { tagMap -> f = applyTags(f, tagMap) }
    tagsAll?.let { tagMap -> f = applyTags(f, tagMap) }

    return f
}

/**
 * Maps Amethyst tag filter entries to rust-nostr Filter methods.
 *
 * Common single-letter tags (#p, #e, #a, #t, #d, #r) use dedicated type-safe
 * methods on the rust-nostr Filter. All other single-letter tags use customTags().
 */
private fun applyTags(
    filter: RustFilter,
    tagMap: Map<String, List<String>>,
): RustFilter {
    var f = filter
    for ((key, values) in tagMap) {
        if (values.isEmpty()) continue
        when (key) {
            "p" -> f = f.pubkeys(values.map { PublicKey.parse(it) })
            "e" -> f = f.events(values.map { EventId.parse(it) })
            "a" -> f = f.coordinates(values.map { Coordinate.parse(it) })
            "t" -> f = f.hashtags(values)
            "d" -> f = f.identifiers(values)
            "r" -> f = f.references(values)
            else -> {
                if (key.length == 1) {
                    val letter = key[0]
                    val alphabetValue = Alphabet.entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
                    if (alphabetValue != null) {
                        val tag =
                            if (letter.isUpperCase()) {
                                SingleLetterTag.uppercase(alphabetValue)
                            } else {
                                SingleLetterTag.lowercase(alphabetValue)
                            }
                        f = f.customTags(tag, values)
                    }
                }
                // Multi-character tag keys are not supported by rust-nostr's Filter
            }
        }
    }
    return f
}
