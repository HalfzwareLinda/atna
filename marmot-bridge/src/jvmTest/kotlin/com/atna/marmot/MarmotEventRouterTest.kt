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
package com.atna.marmot

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarmotEventRouterTest {
    private fun createScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Test
    fun testRouterStartsWithEmptyState() {
        val manager = StubMarmotManager()
        val router = MarmotEventRouter(manager, createScope())
        assertTrue(router.groups.value.isEmpty())
        assertTrue(router.invites.value.isEmpty())
        assertTrue(router.messagesPerGroup.value.isEmpty())
    }

    @Test
    fun testRefreshGroupsWithStubReturnsEmpty() =
        runTest {
            val manager = StubMarmotManager()
            val router = MarmotEventRouter(manager, createScope())
            router.refreshGroups()
            // StubMarmotManager returns empty lists
            assertTrue(router.groups.value.isEmpty())
        }

    @Test
    fun testRefreshInvitesWithStubReturnsEmpty() =
        runTest {
            val manager = StubMarmotManager()
            val router = MarmotEventRouter(manager, createScope())
            router.refreshInvites()
            assertTrue(router.invites.value.isEmpty())
        }

    @Test
    fun testOnMarmotEventIgnoredWhenNotInitialized() {
        val manager = StubMarmotManager()
        val router = MarmotEventRouter(manager, createScope())
        // StubMarmotManager.isInitialized is always false
        val event =
            com.vitorpamplona.quartz.marmotMls.MarmotGroupEvent(
                id = "a".repeat(64),
                pubKey = "b".repeat(64),
                createdAt = 1700000000L,
                tags = emptyArray(),
                content = "encrypted",
                sig = "c".repeat(64),
            )
        // Should not throw — event is silently ignored
        router.onMarmotEvent(event)
    }

    @Test
    fun testStubMarmotManagerIsNotInitialized() {
        val manager = StubMarmotManager()
        assertEquals(false, manager.isInitialized)
        // initialize logs but doesn't change state
        manager.initialize("/tmp/test")
        assertEquals(false, manager.isInitialized)
    }

    @Test
    fun testStubMarmotManagerGetGroupsReturnsEmpty() =
        runTest {
            val manager = StubMarmotManager()
            assertTrue(manager.getGroups().isEmpty())
            assertTrue(manager.getMessages("any").isEmpty())
            assertTrue(manager.getPendingInvites().isEmpty())
            assertEquals(null, manager.getGroup("any"))
            assertTrue(manager.getMembers("any").isEmpty())
        }
}
