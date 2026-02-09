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
package com.vitorpamplona.quartz.marmotMls

import com.vitorpamplona.quartz.nip01Core.core.isRegular
import com.vitorpamplona.quartz.nip01Core.core.isReplaceable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarmotEventKindTest {
    @Test
    fun keyPackageKindIs443() {
        assertEquals(443, MarmotKeyPackageEvent.KIND)
    }

    @Test
    fun welcomeKindIs444() {
        assertEquals(444, MarmotWelcomeEvent.KIND)
    }

    @Test
    fun groupKindIs445() {
        assertEquals(445, MarmotGroupEvent.KIND)
    }

    @Test
    fun keyPackageRelayListKindIs10051() {
        assertEquals(10051, MarmotKeyPackageRelayListEvent.KIND)
    }

    @Test
    fun regularKindsAreRegular() {
        assertTrue(MarmotKeyPackageEvent.KIND.isRegular())
        assertTrue(MarmotWelcomeEvent.KIND.isRegular())
        assertTrue(MarmotGroupEvent.KIND.isRegular())
    }

    @Test
    fun relayListKindIsReplaceable() {
        assertTrue(MarmotKeyPackageRelayListEvent.KIND.isReplaceable())
    }

    @Test
    fun regularKindsAreNotReplaceable() {
        assertFalse(MarmotKeyPackageEvent.KIND.isReplaceable())
        assertFalse(MarmotWelcomeEvent.KIND.isReplaceable())
        assertFalse(MarmotGroupEvent.KIND.isReplaceable())
    }

    @Test
    fun allMarmotEventsHaveEncodedContent() {
        val keyPkg =
            MarmotKeyPackageEvent(
                id = "abc",
                pubKey = "pubkey",
                createdAt = 1000L,
                tags = emptyArray(),
                content = "encrypted",
                sig = "sig",
            )
        assertTrue(keyPkg.isContentEncoded())

        val welcome =
            MarmotWelcomeEvent(
                id = "abc",
                pubKey = "pubkey",
                createdAt = 1000L,
                tags = emptyArray(),
                content = "encrypted",
                sig = "sig",
            )
        assertTrue(welcome.isContentEncoded())

        val group =
            MarmotGroupEvent(
                id = "abc",
                pubKey = "pubkey",
                createdAt = 1000L,
                tags = emptyArray(),
                content = "encrypted",
                sig = "sig",
            )
        assertTrue(group.isContentEncoded())
    }

    @Test
    fun relayListParsesRelayTags() {
        val event =
            MarmotKeyPackageRelayListEvent(
                id = "abc",
                pubKey = "pubkey",
                createdAt = 1000L,
                tags =
                    arrayOf(
                        arrayOf("relay", "wss://relay1.example.com"),
                        arrayOf("relay", "wss://relay2.example.com"),
                        arrayOf("other", "value"),
                    ),
                content = "",
                sig = "sig",
            )
        val relays = event.relays()
        assertEquals(2, relays.size)
        assertEquals("wss://relay1.example.com", relays[0])
        assertEquals("wss://relay2.example.com", relays[1])
    }

    @Test
    fun relayListReturnsEmptyForNoRelays() {
        val event =
            MarmotKeyPackageRelayListEvent(
                id = "abc",
                pubKey = "pubkey",
                createdAt = 1000L,
                tags = emptyArray(),
                content = "",
                sig = "sig",
            )
        assertTrue(event.relays().isEmpty())
    }
}
