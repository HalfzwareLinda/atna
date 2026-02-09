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

import com.vitorpamplona.quartz.nip01Core.relay.filters.Filter
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LmdbEventStoreTest {
    @Test
    fun testStoreCanBeConstructed() {
        val store = LmdbEventStore("/tmp/test-lmdb-ndb")
        assertNotNull(store)
    }

    @Test
    fun testQueryWithoutOpenThrows() =
        runTest {
            val store = LmdbEventStore("/tmp/test-lmdb-ndb")
            assertFailsWith<IllegalStateException> {
                store.query(Filter())
            }
        }

    @Test
    fun testInsertWithoutOpenThrows() =
        runTest {
            val store = LmdbEventStore("/tmp/test-lmdb-ndb")
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
            assertFailsWith<Exception> {
                store.insert(event)
            }
        }

    @Test
    fun testCountWithoutOpenThrows() =
        runTest {
            val store = LmdbEventStore("/tmp/test-lmdb-ndb")
            assertFailsWith<IllegalStateException> {
                store.count(Filter())
            }
        }

    @Test
    fun testCloseWithoutOpenDoesNotThrow() {
        val store = LmdbEventStore("/tmp/test-lmdb-ndb")
        store.close()
    }

    @Test
    fun testFilterConversionEmptyFilter() {
        val filter = Filter()
        val rustFilter = filter.toRustFilter()
        assertNotNull(rustFilter)
        assertTrue(rustFilter.isEmpty())
    }

    @Test
    fun testFilterConversionWithKinds() {
        val filter = Filter(kinds = listOf(1, 7, 30023))
        val rustFilter = filter.toRustFilter()
        assertNotNull(rustFilter)
    }

    @Test
    fun testFilterConversionWithLimit() {
        val filter = Filter(limit = 100)
        val rustFilter = filter.toRustFilter()
        assertNotNull(rustFilter)
    }

    @Test
    fun testFilterConversionWithTimestamps() {
        val filter = Filter(since = 1700000000L, until = 1700100000L)
        val rustFilter = filter.toRustFilter()
        assertNotNull(rustFilter)
    }

    @Test
    fun testFilterConversionWithSearch() {
        val filter = Filter(search = "hello world")
        val rustFilter = filter.toRustFilter()
        assertNotNull(rustFilter)
    }

    @Test
    fun testFilterConversionWithIds() {
        val validId = "a".repeat(64)
        val filter = Filter(ids = listOf(validId))
        val rustFilter = filter.toRustFilter()
        assertNotNull(rustFilter)
    }

    @Test
    fun testFilterConversionWithAuthors() {
        val validPubkey = "b".repeat(64)
        val filter = Filter(authors = listOf(validPubkey))
        val rustFilter = filter.toRustFilter()
        assertNotNull(rustFilter)
    }

    @Test
    fun testOpenAndCloseWithRealDb() {
        val dbPath = System.getProperty("java.io.tmpdir") + "/atna-test-lmdb-" + System.currentTimeMillis()
        val store = LmdbEventStore(dbPath)
        try {
            store.open()
            store.close()
        } finally {
            java.io.File(dbPath).deleteRecursively()
        }
    }

    @Test
    fun testEmptyQueryReturnsEmptyList() =
        runTest {
            val dbPath = System.getProperty("java.io.tmpdir") + "/atna-test-lmdb-" + System.currentTimeMillis()
            val store = LmdbEventStore(dbPath)
            try {
                store.open()
                val results = store.query(Filter(kinds = listOf(1)))
                assertEquals(0, results.size)
            } finally {
                store.close()
                java.io.File(dbPath).deleteRecursively()
            }
        }

    @Test
    fun testEmptyCountReturnsZero() =
        runTest {
            val dbPath = System.getProperty("java.io.tmpdir") + "/atna-test-lmdb-" + System.currentTimeMillis()
            val store = LmdbEventStore(dbPath)
            try {
                store.open()
                val count = store.count(Filter(kinds = listOf(1)))
                assertEquals(0L, count)
            } finally {
                store.close()
                java.io.File(dbPath).deleteRecursively()
            }
        }

    @Test
    fun testWipeOnEmptyDb() =
        runTest {
            val dbPath = System.getProperty("java.io.tmpdir") + "/atna-test-lmdb-" + System.currentTimeMillis()
            val store = LmdbEventStore(dbPath)
            try {
                store.open()
                store.wipe()
            } finally {
                store.close()
                java.io.File(dbPath).deleteRecursively()
            }
        }
}
