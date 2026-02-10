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

import com.vitorpamplona.quartz.nip01Core.relay.client.EmptyNostrClient
import com.vitorpamplona.quartz.nip01Core.relay.normalizer.RelayUrlNormalizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.test.Test
import kotlin.test.assertTrue

class DesktopAuthCoordinatorTest {
    @Test
    fun testAuthCoordinatorCanBeCreated() {
        val client = EmptyNostrClient
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        val coordinator =
            DesktopAuthCoordinator(
                client = client,
                scope = scope,
                getSigners = { emptyList() },
            )

        // Should not throw
        coordinator.destroy()
    }

    @Test
    fun testHasFinishedAuthReturnsTrueForUnknownRelay() {
        val client = EmptyNostrClient
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        val coordinator =
            DesktopAuthCoordinator(
                client = client,
                scope = scope,
                getSigners = { emptyList() },
            )

        val relay = RelayUrlNormalizer.normalizeOrNull("wss://relay.damus.io")!!

        // Unknown relay should return true (no pending auth)
        assertTrue(
            coordinator.hasFinishedAuth(relay),
            "Unknown relay should have no pending auth",
        )

        coordinator.destroy()
    }
}
