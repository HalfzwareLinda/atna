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
package com.vitorpamplona.amethyst.desktop

import com.vitorpamplona.amethyst.desktop.subscriptions.FeedTab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class DesktopScreenTest {
    @Test
    fun allSingletonScreensAreSingletons() {
        assertIs<DesktopScreen>(DesktopScreen.Feed())
        assertIs<DesktopScreen>(DesktopScreen.Reads)
        assertIs<DesktopScreen>(DesktopScreen.Search)
        assertIs<DesktopScreen>(DesktopScreen.Bookmarks)
        assertIs<DesktopScreen>(DesktopScreen.Messages)
        assertIs<DesktopScreen>(DesktopScreen.Notifications)
        assertIs<DesktopScreen>(DesktopScreen.MyProfile)
        assertIs<DesktopScreen>(DesktopScreen.Settings)
        assertIs<DesktopScreen>(DesktopScreen.BugReport)
    }

    @Test
    fun userProfileCarriesPubKeyHex() {
        val profile = DesktopScreen.UserProfile("abc123")
        assertEquals("abc123", profile.pubKeyHex)
    }

    @Test
    fun threadCarriesNoteId() {
        val thread = DesktopScreen.Thread("note1abc")
        assertEquals("note1abc", thread.noteId)
    }

    @Test
    fun dataClassScreensEqualityWorks() {
        val profileA = DesktopScreen.UserProfile("abc")
        val profileB = DesktopScreen.UserProfile("abc")
        val profileC = DesktopScreen.UserProfile("def")

        assertEquals(profileA, profileB)
        assertNotEquals(profileA, profileC)
    }

    @Test
    fun marmotNewChatScreenExists() {
        val screen: DesktopScreen = DesktopScreen.MarmotNewChat()
        assertIs<DesktopScreen>(screen)
    }

    @Test
    fun feedScreenDefaultsToNotesTab() {
        val feed = DesktopScreen.Feed()
        assertEquals(FeedTab.NOTES, feed.tab)
    }

    @Test
    fun feedScreenTabEquality() {
        val feedNotes = DesktopScreen.Feed(FeedTab.NOTES)
        val feedMedia = DesktopScreen.Feed(FeedTab.MEDIA)
        val feedNotes2 = DesktopScreen.Feed(FeedTab.NOTES)

        assertEquals(feedNotes, feedNotes2)
        assertNotEquals(feedNotes, feedMedia)
    }
}
