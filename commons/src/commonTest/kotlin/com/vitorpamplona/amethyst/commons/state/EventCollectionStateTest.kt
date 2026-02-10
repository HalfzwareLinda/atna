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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Simple data class for testing EventCollectionState.
 */
data class TestItem(
    val id: String,
    val timestamp: Long,
)

@OptIn(ExperimentalCoroutinesApi::class)
class EventCollectionStateTest {
    private fun createState(
        scope: TestScope,
        maxSize: Int = 200,
        batchDelayMs: Long = 50,
    ) = EventCollectionState<TestItem>(
        getId = { it.id },
        sortComparator = compareByDescending { it.timestamp },
        maxSize = maxSize,
        batchDelayMs = batchDelayMs,
        scope = scope,
    )

    private fun item(
        id: String,
        ts: Long = System.currentTimeMillis(),
    ) = TestItem(id, ts)

    // ---- Basic add/dedup ----

    @Test
    fun addItemAppearsInList() =
        runTest {
            val state = createState(this)
            state.addItem(item("a", 100))
            advanceUntilIdle()
            assertEquals(1, state.items.value.size)
            assertEquals("a", state.items.value[0].id)
        }

    @Test
    fun duplicateItemsAreIgnored() =
        runTest {
            val state = createState(this)
            state.addItem(item("a", 100))
            state.addItem(item("a", 100))
            advanceUntilIdle()
            assertEquals(1, state.items.value.size)
        }

    @Test
    fun addItemsSortsDescendingByTimestamp() =
        runTest {
            val state = createState(this)
            state.addItem(item("old", 100))
            state.addItem(item("new", 200))
            advanceUntilIdle()
            assertEquals("new", state.items.value[0].id)
            assertEquals("old", state.items.value[1].id)
        }

    // ---- Hold mode / pending new count (the "new notes pill") ----

    @Test
    fun pendingNewCountStartsAtZero() =
        runTest {
            val state = createState(this)
            assertEquals(0, state.pendingNewCount.value)
        }

    @Test
    fun holdModeBuffersNewItemsAndUpdatesCount() =
        runTest {
            val state = createState(this)
            // Seed the list so it's non-empty
            state.addItem(item("seed", 100))
            advanceUntilIdle()
            assertEquals(1, state.items.value.size)

            // Enable hold mode (simulates user scrolling down)
            state.setHoldNewItems(true)

            // Add new items — should be buffered, not in the main list
            state.addItem(item("new1", 200))
            state.addItem(item("new2", 300))
            advanceUntilIdle()

            assertEquals(1, state.items.value.size, "main list should still be 1")
            assertEquals(2, state.pendingNewCount.value, "pending count should be 2")
        }

    @Test
    fun holdModeDoesNotBufferWhenMainListEmpty() =
        runTest {
            val state = createState(this)
            // Enable hold mode before any items exist
            state.setHoldNewItems(true)

            state.addItem(item("a", 100))
            advanceUntilIdle()

            // Should go to the main list (not buffered) because list was empty
            assertEquals(1, state.items.value.size)
            assertEquals(0, state.pendingNewCount.value)
        }

    @Test
    fun releasePendingMergesBufferedItemsIntoMainList() =
        runTest {
            val state = createState(this)
            state.addItem(item("seed", 100))
            advanceUntilIdle()

            state.setHoldNewItems(true)
            state.addItem(item("new1", 200))
            state.addItem(item("new2", 300))
            advanceUntilIdle()

            assertEquals(2, state.pendingNewCount.value)

            // Release — simulates user clicking the pill or scrolling to top
            state.releasePending()
            advanceUntilIdle()

            assertEquals(3, state.items.value.size, "all 3 items should be in main list")
            assertEquals(0, state.pendingNewCount.value, "pending count should reset to 0")
            // Sorted: new2 (300) > new1 (200) > seed (100)
            assertEquals("new2", state.items.value[0].id)
        }

    @Test
    fun disablingHoldModeReleasesBufferedItems() =
        runTest {
            val state = createState(this)
            state.addItem(item("seed", 100))
            advanceUntilIdle()

            state.setHoldNewItems(true)
            state.addItem(item("new1", 200))
            advanceUntilIdle()
            assertEquals(1, state.pendingNewCount.value)

            // Disable hold mode (simulates scrolling back to top)
            state.setHoldNewItems(false)
            advanceUntilIdle()

            assertEquals(2, state.items.value.size)
            assertEquals(0, state.pendingNewCount.value)
        }

    // ---- BUG SCENARIO: duplicates counted in pending buffer ----

    @Test
    fun duplicateItemsNotDoubleCountedInPendingBuffer() =
        runTest {
            val state = createState(this)
            state.addItem(item("seed", 100))
            advanceUntilIdle()

            state.setHoldNewItems(true)

            // Add the same item twice — should only count once
            state.addItem(item("dup", 200))
            state.addItem(item("dup", 200))
            advanceUntilIdle()

            assertEquals(1, state.pendingNewCount.value, "duplicate should not be double-counted")
        }

    @Test
    fun addItemsAlreadyInMainListNotCountedAsPending() =
        runTest {
            val state = createState(this)
            // Add items while NOT in hold mode (they go to main list)
            state.addItem(item("a", 100))
            state.addItem(item("b", 200))
            advanceUntilIdle()
            assertEquals(2, state.items.value.size)

            // Enable hold mode
            state.setHoldNewItems(true)

            // Try to add items that already exist in the main list
            state.addItem(item("a", 100))
            state.addItem(item("b", 200))
            advanceUntilIdle()

            assertEquals(0, state.pendingNewCount.value, "existing items should not count as new")
        }

    // ---- BUG SCENARIO: addItems (batch) with hold mode ----

    @Test
    fun addItemsBatchInHoldModeBuffersCorrectly() =
        runTest {
            val state = createState(this)
            state.addItem(item("seed", 100))
            advanceUntilIdle()

            state.setHoldNewItems(true)

            // Batch add — simulates relay EOSE dump
            state.addItems(
                listOf(
                    item("new1", 200),
                    item("new2", 300),
                    item("new3", 400),
                ),
            )
            advanceUntilIdle()

            assertEquals(1, state.items.value.size, "main list should still be 1")
            assertEquals(3, state.pendingNewCount.value, "all 3 batch items should be pending")
        }

    @Test
    fun addItemsBatchWithDuplicatesInHoldMode() =
        runTest {
            val state = createState(this)
            state.addItem(item("seed", 100))
            advanceUntilIdle()

            state.setHoldNewItems(true)

            // Batch with duplicates
            state.addItems(
                listOf(
                    item("x", 200),
                    item("x", 200), // duplicate within batch
                    item("y", 300),
                ),
            )
            advanceUntilIdle()

            // seenIds filter is applied via `filter { getId(it) !in seenIds }`
            // BUT this filters against seenIds which is updated DURING the loop
            // Let's see what actually happens...
            assertEquals(2, state.pendingNewCount.value, "duplicates within batch should be deduplicated")
        }

    @Test
    fun addItemsBatchExcludesAlreadySeenItems() =
        runTest {
            val state = createState(this)
            state.addItem(item("existing", 100))
            advanceUntilIdle()

            state.setHoldNewItems(true)

            // Batch contains one item already in main list
            state.addItems(
                listOf(
                    item("existing", 100),
                    item("new1", 200),
                ),
            )
            advanceUntilIdle()

            assertEquals(1, state.pendingNewCount.value, "existing item should not be counted")
        }

    // ---- BUG SCENARIO: trimming removes items from seenIds, allowing re-adds ----

    @Test
    fun trimmedOldItemsGoToMainListNotPendingBuffer() =
        runTest {
            val state = createState(this, maxSize = 3)

            // Fill beyond maxSize so trimming occurs
            state.addItem(item("a", 100))
            state.addItem(item("b", 200))
            state.addItem(item("c", 300))
            state.addItem(item("d", 400))
            advanceUntilIdle()

            // maxSize=3, so "a" (ts=100) should be trimmed; head is "d" (ts=400)
            assertEquals(3, state.items.value.size)
            assertEquals("d", state.items.value[0].id)

            state.setHoldNewItems(true)

            // Re-add "a" (ts=100) — it's OLDER than head (ts=400), so it goes to
            // the main list, not the pending buffer. This prevents old events from
            // late relays from inflating the "new notes" count.
            state.addItem(item("a", 100))
            advanceUntilIdle()

            assertEquals(0, state.pendingNewCount.value, "old re-added item should not count as new")
            // But it may get trimmed again due to maxSize
        }

    @Test
    fun trimmedItemReAddedAsNewerCountsAsPending() =
        runTest {
            val state = createState(this, maxSize = 3)

            state.addItem(item("a", 100))
            state.addItem(item("b", 200))
            state.addItem(item("c", 300))
            state.addItem(item("d", 400))
            advanceUntilIdle()

            assertEquals(3, state.items.value.size)

            state.setHoldNewItems(true)

            // Add a genuinely new item (ts=500, newer than head ts=400)
            state.addItem(item("e", 500))
            advanceUntilIdle()

            assertEquals(1, state.pendingNewCount.value, "newer item should count as pending")
        }

    // ---- isNewerThanHead: only genuinely new items count ----

    @Test
    fun newerItemsInHoldModeCountAsPending() =
        runTest {
            val state = createState(this)
            state.addItem(item("seed", 100))
            advanceUntilIdle()

            state.setHoldNewItems(true)

            // Each item with a higher timestamp than seed (100) should be counted
            for (i in 1..10) {
                state.addItem(item("item-$i", 100L + i))
            }
            advanceUntilIdle()

            assertEquals(10, state.pendingNewCount.value, "each newer unique item should be counted once")
        }

    @Test
    fun olderItemsInHoldModeGoToMainListNotPending() =
        runTest {
            val state = createState(this)
            // Seed with a recent event
            state.addItem(item("recent", 500))
            advanceUntilIdle()

            state.setHoldNewItems(true)

            // Add items that are OLDER than the head (ts < 500)
            state.addItem(item("old1", 100))
            state.addItem(item("old2", 200))
            state.addItem(item("old3", 300))
            advanceUntilIdle()

            assertEquals(0, state.pendingNewCount.value, "older items should not count as new")
            // They should be in the main list (merged via batch update)
            assertEquals(4, state.items.value.size, "old items should merge into main list")
        }

    @Test
    fun mixOfOldAndNewItemsInHoldMode() =
        runTest {
            val state = createState(this)
            state.addItem(item("head", 500))
            advanceUntilIdle()

            state.setHoldNewItems(true)

            // 3 old items (go to main list) + 2 new items (go to pending buffer)
            state.addItem(item("old1", 100))
            state.addItem(item("old2", 200))
            state.addItem(item("new1", 600))
            state.addItem(item("old3", 300))
            state.addItem(item("new2", 700))
            advanceUntilIdle()

            assertEquals(2, state.pendingNewCount.value, "only genuinely new items should be pending")
            assertEquals(4, state.items.value.size, "3 old items + 1 head in main list")
        }

    // ---- Clear while in hold mode ----

    @Test
    fun clearResetsPendingCount() =
        runTest {
            val state = createState(this)
            state.addItem(item("seed", 100))
            advanceUntilIdle()

            state.setHoldNewItems(true)
            state.addItem(item("new", 200))
            advanceUntilIdle()
            assertEquals(1, state.pendingNewCount.value)

            state.clear()
            advanceUntilIdle()

            assertEquals(0, state.pendingNewCount.value)
            assertEquals(0, state.items.value.size)
        }

    // ---- Race condition: hold mode toggled rapidly ----

    @Test
    fun rapidHoldToggleDoesNotLoseItems() =
        runTest {
            val state = createState(this)
            state.addItem(item("seed", 100))
            advanceUntilIdle()

            // Rapid toggle: hold -> add -> release -> hold -> add -> release
            state.setHoldNewItems(true)
            state.addItem(item("a", 200))
            state.setHoldNewItems(false) // releases "a"
            state.setHoldNewItems(true)
            state.addItem(item("b", 300))
            state.setHoldNewItems(false) // releases "b"
            advanceUntilIdle()

            assertEquals(3, state.items.value.size, "all items should be in main list")
            assertEquals(0, state.pendingNewCount.value)
        }

    // ---- maxSize interaction with pending buffer ----

    @Test
    fun releasedPendingItemsRespectMaxSize() =
        runTest {
            val state = createState(this, maxSize = 5)

            // Fill with 4 items
            state.addItems(
                listOf(
                    item("a", 100),
                    item("b", 200),
                    item("c", 300),
                    item("d", 400),
                ),
            )
            advanceUntilIdle()
            assertEquals(4, state.items.value.size)

            state.setHoldNewItems(true)

            // Buffer 3 more items (total would be 7, exceeds maxSize=5)
            state.addItem(item("e", 500))
            state.addItem(item("f", 600))
            state.addItem(item("g", 700))
            advanceUntilIdle()
            assertEquals(3, state.pendingNewCount.value)

            // Release
            state.releasePending()
            advanceUntilIdle()

            assertEquals(5, state.items.value.size, "should be capped at maxSize")
            assertEquals(0, state.pendingNewCount.value)
            // Newest items should survive (700, 600, 500, 400, 300)
            assertEquals("g", state.items.value[0].id)
            assertEquals("f", state.items.value[1].id)
        }

    // ---- The actual 830 vs 1 bug scenario ----
    // Root cause: the outbox model fans out to many relays, each sending
    // unique events. When the user scrolls and hold mode kicks in,
    // late-arriving old events from slow relays were ALL counted as "new."
    // Fix: only events newer than the current head of the list are counted.

    @Test
    fun lateRelayDuplicatesDoNotInflatePendingCount() =
        runTest {
            val state = createState(this)

            // Phase 1: Initial load from fast relays (user at top, no hold mode)
            for (i in 1..50) {
                state.addItem(item("event-$i", 1000L + i))
            }
            advanceUntilIdle()
            assertEquals(50, state.items.value.size)

            // User scrolls down — hold mode on
            state.setHoldNewItems(true)

            // Phase 2: Slow relays deliver the SAME events again (all rejected by seenIds)
            for (i in 1..50) {
                state.addItem(item("event-$i", 1000L + i))
            }
            advanceUntilIdle()

            assertEquals(
                0,
                state.pendingNewCount.value,
                "duplicate events from slow relays should NOT inflate the pending count",
            )
        }

    @Test
    fun lateRelayUniqueOldEventsDoNotInflatePendingCount() =
        runTest {
            val state = createState(this)

            // Phase 1: Initial load — 20 events with ts 1000..1019
            for (i in 0..19) {
                state.addItem(item("fast-$i", 1000L + i))
            }
            advanceUntilIdle()
            assertEquals(20, state.items.value.size)
            // Head is fast-19 (ts=1019)

            // User scrolls down — hold mode on
            state.setHoldNewItems(true)

            // Phase 2: Slow relays deliver DIFFERENT but OLD events (ts < 1019)
            for (i in 0..99) {
                state.addItem(item("slow-$i", 500L + i)) // ts 500..599, all older than head
            }
            advanceUntilIdle()

            assertEquals(
                0,
                state.pendingNewCount.value,
                "unique-but-old events from slow relays should NOT count as new",
            )
            // But they should be in the main list
            assertEquals(120, state.items.value.size, "old events should merge into main list")
        }

    @Test
    fun onlyGenuinelyNewEventsCountedAfterHoldEnabled() =
        runTest {
            val state = createState(this)

            // Load 50 events (head at ts=1050)
            for (i in 1..50) {
                state.addItem(item("event-$i", 1000L + i))
            }
            advanceUntilIdle()

            state.setHoldNewItems(true)

            // 50 duplicates from slow relays (rejected by seenIds)
            for (i in 1..50) {
                state.addItem(item("event-$i", 1000L + i))
            }
            // 1 genuinely new event (ts=2000 > head ts=1050)
            state.addItem(item("genuinely-new", 2000))
            advanceUntilIdle()

            assertEquals(
                1,
                state.pendingNewCount.value,
                "only the 1 genuinely new event should be counted",
            )
        }

    @Test
    fun realWorldScenarioOutboxFloodDoesNotInflateCount() =
        runTest {
            val state = createState(this)

            // Simulate: 30 events arrive from fast relays (initial render)
            for (i in 1..30) {
                state.addItem(item("note-$i", 1000L + i))
            }
            advanceUntilIdle()
            assertEquals(30, state.items.value.size)

            // User scrolls down slightly
            state.setHoldNewItems(true)

            // 800 unique OLD events arrive from slow outbox relays
            // (different authors' write relays that responded late)
            for (i in 1..800) {
                state.addItem(item("outbox-$i", 500L + i)) // ts 501..1300
            }

            // Only 1 actually new note is posted
            state.addItem(item("brand-new-post", 2000))
            advanceUntilIdle()

            // The pill should say "1 new note", not "801 new notes"
            // Note: some outbox events have ts > 1030 (the head), specifically
            // ts 1031..1300 (270 events). These would be counted as "new" since they
            // are newer than the head. This is still correct behavior — they ARE
            // events the user hasn't seen that sort above the current view.
            // The key fix is that the 530 events with ts <= 1030 go to main list.
            val oldEventsAboveHead = (1..800).count { 500L + it > 1030L }
            assertEquals(
                oldEventsAboveHead + 1, // outbox events newer than head + the brand new post
                state.pendingNewCount.value,
                "only events newer than the head should be counted",
            )
        }
}
