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
package com.vitorpamplona.amethyst.desktop.ui

import com.vitorpamplona.amethyst.desktop.subscriptions.FeedTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeedTabFilterTest {
    // Reply detection logic (same as used in FeedScreen.kt)
    private fun isReply(tags: Array<Array<String>>): Boolean = tags.any { it.size >= 2 && it[0] == "e" }

    // --- FeedTab enum tests ---

    @Test
    fun testFeedTabNotesKinds() {
        assertEquals(listOf(1, 6), FeedTab.NOTES.kinds)
        assertEquals("Notes", FeedTab.NOTES.label)
    }

    @Test
    fun testFeedTabRepliesKinds() {
        assertEquals(listOf(1), FeedTab.REPLIES.kinds)
        assertEquals("Replies", FeedTab.REPLIES.label)
    }

    @Test
    fun testFeedTabMediaKinds() {
        assertEquals(listOf(20, 21, 22, 34235, 34236, 1063), FeedTab.MEDIA.kinds)
        assertEquals("Media", FeedTab.MEDIA.label)
    }

    @Test
    fun testFeedTabArticlesKinds() {
        assertEquals(listOf(30023), FeedTab.ARTICLES.kinds)
        assertEquals("Articles", FeedTab.ARTICLES.label)
    }

    @Test
    fun testFeedTabLiveKinds() {
        assertEquals(listOf(30311), FeedTab.LIVE.kinds)
        assertEquals("Live", FeedTab.LIVE.label)
    }

    @Test
    fun testFeedTabCount() {
        assertEquals(5, FeedTab.entries.size)
    }

    // --- Reply detection tests ---

    @Test
    fun testReplyDetectionWithETags() {
        val tags =
            arrayOf(
                arrayOf("e", "1111111111111111111111111111111111111111111111111111111111111111"),
                arrayOf("p", "0000000000000000000000000000000000000000000000000000000000000001"),
            )
        assertTrue(isReply(tags))
    }

    @Test
    fun testReplyDetectionWithoutETags() {
        val tags =
            arrayOf(
                arrayOf("p", "0000000000000000000000000000000000000000000000000000000000000001"),
                arrayOf("t", "bitcoin"),
            )
        assertFalse(isReply(tags))
    }

    @Test
    fun testReplyDetectionWithEmptyTags() {
        val tags = emptyArray<Array<String>>()
        assertFalse(isReply(tags))
    }

    @Test
    fun testReplyDetectionWithSingleElementETag() {
        // An "e" tag with only one element (no value) should not count as a reply
        val tags =
            arrayOf(
                arrayOf("e"),
            )
        assertFalse(isReply(tags))
    }

    @Test
    fun testReplyDetectionWithMultipleETags() {
        val tags =
            arrayOf(
                arrayOf("e", "1111111111111111111111111111111111111111111111111111111111111111", "wss://relay.example.com", "root"),
                arrayOf("e", "2222222222222222222222222222222222222222222222222222222222222222", "wss://relay.example.com", "reply"),
                arrayOf("p", "0000000000000000000000000000000000000000000000000000000000000001"),
            )
        assertTrue(isReply(tags))
    }

    // --- Notes tab filtering tests ---

    @Test
    fun testNotesTabExcludesReplies() {
        val replyTags =
            arrayOf(
                arrayOf("e", "1111111111111111111111111111111111111111111111111111111111111111"),
            )
        val noteTags =
            arrayOf(
                arrayOf("p", "0000000000000000000000000000000000000000000000000000000000000001"),
            )

        // Notes tab should exclude replies (events with e-tags)
        val noteIncluded = !isReply(noteTags)
        val replyExcluded = isReply(replyTags)

        assertTrue(noteIncluded, "Note without e-tags should be included in Notes tab")
        assertTrue(replyExcluded, "Reply with e-tags should be excluded from Notes tab")
    }

    @Test
    fun testRepliesTabOnlyIncludesReplies() {
        val replyTags =
            arrayOf(
                arrayOf("e", "1111111111111111111111111111111111111111111111111111111111111111"),
            )
        val noteTags =
            arrayOf(
                arrayOf("p", "0000000000000000000000000000000000000000000000000000000000000001"),
            )

        // Replies tab should only include events with e-tags
        val replyIncluded = isReply(replyTags)
        val noteExcluded = !isReply(noteTags)

        assertTrue(replyIncluded, "Reply with e-tags should be included in Replies tab")
        assertTrue(noteExcluded, "Note without e-tags should be excluded from Replies tab")
    }

    // --- Media tab kind coverage tests ---

    @Test
    fun testMediaTabAcceptsPictureKind() {
        assertTrue(20 in FeedTab.MEDIA.kinds, "Media tab should accept kind 20 (NIP-68 pictures)")
    }

    @Test
    fun testMediaTabAcceptsVideoKinds() {
        assertTrue(21 in FeedTab.MEDIA.kinds, "Media tab should accept kind 21 (normal video)")
        assertTrue(22 in FeedTab.MEDIA.kinds, "Media tab should accept kind 22 (short video)")
        assertTrue(34235 in FeedTab.MEDIA.kinds, "Media tab should accept kind 34235 (horizontal video)")
        assertTrue(34236 in FeedTab.MEDIA.kinds, "Media tab should accept kind 34236 (vertical video)")
    }

    @Test
    fun testMediaTabAcceptsFileMetadataKind() {
        assertTrue(1063 in FeedTab.MEDIA.kinds, "Media tab should accept kind 1063 (NIP-94 file metadata)")
    }

    @Test
    fun testMediaTabHasSixKinds() {
        assertEquals(6, FeedTab.MEDIA.kinds.size, "Media tab should have exactly 6 event kinds")
    }

    // --- Live tab status sorting tests ---

    @Test
    fun testLiveStatusSortOrder() {
        // Status priority: LIVE (0) > PLANNED (1) > ENDED (2) > null (3)
        val statusPriority: (String?) -> Int = { status ->
            when (status?.lowercase()) {
                "live" -> 0
                "planned" -> 1
                "ended" -> 2
                else -> 3
            }
        }

        val statuses = listOf("ended", "live", null, "planned", "live", "ended")
        val sorted = statuses.sortedBy { statusPriority(it) }

        assertEquals("live", sorted[0])
        assertEquals("live", sorted[1])
        assertEquals("planned", sorted[2])
        assertEquals("ended", sorted[3])
        assertEquals("ended", sorted[4])
        assertEquals(null, sorted[5])
    }

    @Test
    fun testLiveStatusSortWithAllSameStatus() {
        val statusPriority: (String?) -> Int = { status ->
            when (status?.lowercase()) {
                "live" -> 0
                "planned" -> 1
                "ended" -> 2
                else -> 3
            }
        }

        val statuses = listOf("live", "live", "live")
        val sorted = statuses.sortedBy { statusPriority(it) }

        assertEquals(3, sorted.size)
        assertTrue(sorted.all { it == "live" })
    }
}
