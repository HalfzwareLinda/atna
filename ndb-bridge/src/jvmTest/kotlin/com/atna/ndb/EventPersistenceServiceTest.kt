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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EventPersistenceServiceTest {
    private fun createScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Test
    fun testServiceCanBeConstructed() {
        val service = EventPersistenceService(createScope())
        assertNotNull(service)
        assertFalse(service.isRunning)
    }

    @Test
    fun testServiceStartAndStop() {
        val dbPath = System.getProperty("java.io.tmpdir") + "/atna-test-persistence-" + System.currentTimeMillis()
        val service = EventPersistenceService(createScope())
        try {
            service.start(dbPath)
            assertTrue(service.isRunning)
            service.stop()
            assertFalse(service.isRunning)
        } finally {
            java.io.File(dbPath).deleteRecursively()
        }
    }

    @Test
    fun testDoubleStartIsIdempotent() {
        val dbPath = System.getProperty("java.io.tmpdir") + "/atna-test-persistence-" + System.currentTimeMillis()
        val service = EventPersistenceService(createScope())
        try {
            service.start(dbPath)
            service.start(dbPath) // Should not throw
            assertTrue(service.isRunning)
        } finally {
            service.stop()
            java.io.File(dbPath).deleteRecursively()
        }
    }

    @Test
    fun testStopWithoutStartIsIdempotent() {
        val service = EventPersistenceService(createScope())
        service.stop() // Should not throw
        assertFalse(service.isRunning)
    }

    @Test
    fun testPersistEventBeforeStartDoesNotThrow() {
        val service = EventPersistenceService(createScope())
        val event =
            com.vitorpamplona.quartz.nip01Core.core.Event(
                id = "a".repeat(64),
                pubKey = "b".repeat(64),
                createdAt = 1700000000L,
                kind = 1,
                tags = emptyArray(),
                content = "test",
                sig = "c".repeat(64),
            )
        service.persistEvent(event) // Should silently drop
    }

    @Test
    fun testLoadEventsBeforeStartReturnsEmpty() =
        runTest {
            val service = EventPersistenceService(createScope())
            val events =
                service.loadEvents(
                    com.vitorpamplona.quartz.nip01Core.relay.filters
                        .Filter(kinds = listOf(1)),
                )
            assertTrue(events.isEmpty())
        }

    @Test
    fun testEmptyLoadReturnsEmpty() =
        runTest {
            val dbPath = System.getProperty("java.io.tmpdir") + "/atna-test-persistence-" + System.currentTimeMillis()
            val service = EventPersistenceService(createScope())
            try {
                service.start(dbPath)
                val events =
                    service.loadEvents(
                        com.vitorpamplona.quartz.nip01Core.relay.filters
                            .Filter(kinds = listOf(1)),
                    )
                assertTrue(events.isEmpty())
            } finally {
                service.stop()
                java.io.File(dbPath).deleteRecursively()
            }
        }

    @Test
    fun testEventCountBeforeStartReturnsZero() =
        runTest {
            val service = EventPersistenceService(createScope())
            val count =
                service.eventCount(
                    com.vitorpamplona.quartz.nip01Core.relay.filters
                        .Filter(kinds = listOf(1)),
                )
            assertTrue(count == 0L)
        }

    @Test
    fun testDirectoryIsCreatedOnStart() {
        val dbPath = System.getProperty("java.io.tmpdir") + "/atna-test-persistence-dir-" + System.currentTimeMillis()
        val service = EventPersistenceService(createScope())
        try {
            service.start(dbPath)
            assertTrue(java.io.File(dbPath).exists())
            assertTrue(java.io.File(dbPath).isDirectory)
        } finally {
            service.stop()
            java.io.File(dbPath).deleteRecursively()
        }
    }
}
