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
package com.vitorpamplona.amethyst.commons.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Generic event collection state with deduplication, batching, sorting, and size limits.
 *
 * Provides efficient management of event/item collections with:
 * - Automatic deduplication by ID
 * - Batched updates (250ms default) to reduce recomposition
 * - Optional sorting via comparator
 * - Automatic trimming to max size
 * - Thread-safe operations
 *
 * @param T The type of items to collect (must have a unique ID)
 * @param getId Function to extract unique ID from an item
 * @param sortComparator Optional comparator for sorting items (null = prepend newest first)
 * @param maxSize Maximum number of items to keep (older items trimmed)
 * @param batchDelayMs Delay in milliseconds before flushing batched updates (default 250ms)
 * @param scope CoroutineScope for batching jobs
 *
 * Usage example:
 * ```
 * val feedState = EventCollectionState<Event>(
 *     getId = { it.id },
 *     sortComparator = compareByDescending { it.createdAt },
 *     maxSize = 200,
 *     scope = viewModelScope
 * )
 *
 * // Add items (batched automatically)
 * feedState.addItem(event)
 * feedState.addItems(eventList)
 *
 * // Observe
 * val items by feedState.items.collectAsState()
 * ```
 */
class EventCollectionState<T : Any>(
    private val getId: (T) -> String,
    private val sortComparator: Comparator<T>? = null,
    private val maxSize: Int = 200,
    private val batchDelayMs: Long = 250,
    private val scope: CoroutineScope,
) {
    private val _items = MutableStateFlow<List<T>>(emptyList())
    val items: StateFlow<List<T>> = _items.asStateFlow()

    private val seenIds = mutableSetOf<String>()
    private val pendingItems = mutableListOf<T>()
    private val mutex = Mutex()
    private var batchJob: Job? = null

    // Hold mode: when true, new items go to a separate buffer instead of the main list.
    // Used by FeedScreen to show "X new notes" indicator when the user has scrolled down.
    @Volatile
    private var holdNewItems = false
    private val pendingNewBuffer = mutableListOf<T>()
    private val _pendingNewCount = MutableStateFlow(0)
    val pendingNewCount: StateFlow<Int> = _pendingNewCount.asStateFlow()

    /**
     * Enable or disable hold mode. When enabled, new items are buffered
     * separately and not merged into the main list until [releasePending] is called.
     * When disabled, any buffered items are released automatically.
     */
    fun setHoldNewItems(hold: Boolean) {
        holdNewItems = hold
        if (!hold) {
            releasePending()
        }
    }

    /**
     * Release all held new items into the main list.
     */
    fun releasePending() {
        scope.launch {
            mutex.withLock {
                if (pendingNewBuffer.isNotEmpty()) {
                    pendingItems.addAll(pendingNewBuffer)
                    pendingNewBuffer.clear()
                    _pendingNewCount.value = 0
                    scheduleBatchUpdate()
                }
            }
        }
    }

    /**
     * Add a single item to the collection.
     * Updates are batched and applied after batchDelayMs.
     * If hold mode is active and the main list is non-empty, the item
     * is buffered separately for the "new notes" indicator — but only
     * if it sorts BEFORE (i.e., is newer than) the current head of the list.
     * Late-arriving old events from slow relays go directly to the main list.
     *
     * @param item The item to add
     */
    fun addItem(item: T) {
        scope.launch {
            mutex.withLock {
                val itemId = getId(item)
                if (itemId !in seenIds) {
                    seenIds.add(itemId)
                    if (holdNewItems && _items.value.isNotEmpty() && isNewerThanHead(item)) {
                        pendingNewBuffer.add(item)
                        _pendingNewCount.value = pendingNewBuffer.size
                    } else {
                        pendingItems.add(item)
                        scheduleBatchUpdate()
                    }
                }
            }
        }
    }

    /**
     * Add multiple items to the collection.
     * Updates are batched and applied after batchDelayMs.
     *
     * @param items The items to add
     */
    fun addItems(items: List<T>) {
        scope.launch {
            mutex.withLock {
                val newItems = items.distinctBy { getId(it) }.filter { getId(it) !in seenIds }
                if (newItems.isNotEmpty()) {
                    newItems.forEach { seenIds.add(getId(it)) }
                    if (holdNewItems && _items.value.isNotEmpty()) {
                        // Split: only items newer than the current head go to the pending buffer
                        val (newer, older) = newItems.partition { isNewerThanHead(it) }
                        if (older.isNotEmpty()) {
                            pendingItems.addAll(older)
                            scheduleBatchUpdate()
                        }
                        if (newer.isNotEmpty()) {
                            pendingNewBuffer.addAll(newer)
                            _pendingNewCount.value = pendingNewBuffer.size
                        }
                    } else {
                        pendingItems.addAll(newItems)
                        scheduleBatchUpdate()
                    }
                }
            }
        }
    }

    /**
     * Checks whether [item] sorts before (is "newer" than) the current head of the list.
     * When no comparator is set, all items are considered newer (original behavior).
     */
    private fun isNewerThanHead(item: T): Boolean {
        val comp = sortComparator ?: return true
        val head = _items.value.firstOrNull() ?: return true
        return comp.compare(item, head) < 0
    }

    /**
     * Remove an item by ID.
     *
     * @param id The ID of the item to remove
     */
    fun removeItem(id: String) {
        scope.launch {
            mutex.withLock {
                seenIds.remove(id)
                _items.value = _items.value.filter { getId(it) != id }
            }
        }
    }

    /**
     * Remove multiple items by ID.
     *
     * @param ids The IDs of items to remove
     */
    fun removeItems(ids: Set<String>) {
        scope.launch {
            mutex.withLock {
                seenIds.removeAll(ids)
                _items.value = _items.value.filter { getId(it) !in ids }
            }
        }
    }

    /**
     * Clear all items from the collection.
     */
    fun clear() {
        scope.launch {
            mutex.withLock {
                seenIds.clear()
                pendingItems.clear()
                pendingNewBuffer.clear()
                _pendingNewCount.value = 0
                _items.value = emptyList()
                batchJob?.cancel()
                batchJob = null
            }
        }
    }

    /**
     * Get current item count.
     */
    val size: Int
        get() = _items.value.size

    /**
     * Check if collection is empty.
     */
    val isEmpty: Boolean
        get() = _items.value.isEmpty()

    /**
     * Schedules a batched update if not already scheduled.
     * Cancels existing batch job and starts a new one.
     */
    private fun scheduleBatchUpdate() {
        batchJob?.cancel()
        batchJob =
            scope.launch {
                delay(batchDelayMs)
                applyBatchUpdate()
            }
    }

    /**
     * Applies pending items to the collection.
     * Uses O(n) merge of two pre-sorted lists when comparator is provided,
     * avoiding the previous O(n log n) full re-sort on every batch.
     */
    private suspend fun applyBatchUpdate() {
        mutex.withLock {
            if (pendingItems.isEmpty()) return

            // seenIds already updated in addItem/addItems

            val result =
                if (sortComparator != null) {
                    // Sort only the new pending items (small list), then merge with already-sorted existing items
                    val sortedPending = pendingItems.sortedWith(sortComparator)
                    mergeSorted(_items.value, sortedPending, sortComparator)
                } else {
                    // Reverse so newest (pending) items come first
                    (pendingItems.reversed() + _items.value).distinctBy { getId(it) }
                }

            // Trim to maxSize and update seenIds
            val trimmed =
                if (result.size > maxSize) {
                    val kept = result.take(maxSize)
                    val removed = result.drop(maxSize)
                    removed.forEach { seenIds.remove(getId(it)) }
                    kept
                } else {
                    result
                }

            _items.value = trimmed
            pendingItems.clear()
        }
    }

    /**
     * O(n) merge of two sorted lists with deduplication by ID.
     * Both input lists must already be sorted according to [comparator].
     */
    private fun mergeSorted(
        a: List<T>,
        b: List<T>,
        comparator: Comparator<T>,
    ): List<T> {
        val result = ArrayList<T>(a.size + b.size)
        val seenMerge = HashSet<String>(a.size + b.size)
        var i = 0
        var j = 0
        while (i < a.size && j < b.size) {
            val cmp = comparator.compare(a[i], b[j])
            val next =
                if (cmp <= 0) {
                    a[i++]
                } else {
                    b[j++]
                }
            val id = getId(next)
            if (seenMerge.add(id)) {
                result.add(next)
            }
        }
        while (i < a.size) {
            val id = getId(a[i])
            if (seenMerge.add(id)) result.add(a[i])
            i++
        }
        while (j < b.size) {
            val id = getId(b[j])
            if (seenMerge.add(id)) result.add(b[j])
            j++
        }
        return result
    }

    /**
     * Force flush pending items immediately without waiting for batch delay.
     */
    suspend fun flush() {
        batchJob?.cancel()
        applyBatchUpdate()
    }
}
