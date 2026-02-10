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
package com.vitorpamplona.amethyst.desktop.cache

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LruConcurrentCacheTest {
    @Test
    fun testGetOrPutCreatesEntry() {
        val cache = LruConcurrentCache<String, Int>(10)
        val value = cache.getOrPut("a") { 1 }
        assertEquals(1, value)
        assertEquals(1, cache.size())
    }

    @Test
    fun testGetReturnsExistingEntry() {
        val cache = LruConcurrentCache<String, Int>(10)
        cache.getOrPut("a") { 1 }
        assertEquals(1, cache.get("a"))
    }

    @Test
    fun testGetReturnsNullForMissing() {
        val cache = LruConcurrentCache<String, Int>(10)
        assertNull(cache.get("missing"))
    }

    @Test
    fun testEvictsOldestWhenOverMax() {
        val cache = LruConcurrentCache<String, Int>(3)
        cache.getOrPut("a") { 1 }
        cache.getOrPut("b") { 2 }
        cache.getOrPut("c") { 3 }
        // Adding 4th should evict "a" (oldest)
        cache.getOrPut("d") { 4 }
        assertEquals(3, cache.size())
        assertNull(cache.get("a"), "oldest entry 'a' should be evicted")
        assertNotNull(cache.get("b"))
        assertNotNull(cache.get("c"))
        assertNotNull(cache.get("d"))
    }

    @Test
    fun testAccessTouchKeepsEntryAlive() {
        val cache = LruConcurrentCache<String, Int>(3)
        cache.getOrPut("a") { 1 }
        cache.getOrPut("b") { 2 }
        cache.getOrPut("c") { 3 }
        // Access "a" to move it to tail (most recent)
        cache.get("a")
        // Adding "d" should evict "b" (now the oldest)
        cache.getOrPut("d") { 4 }
        assertEquals(3, cache.size())
        assertNotNull(cache.get("a"), "'a' was accessed, should survive")
        assertNull(cache.get("b"), "'b' should be evicted as oldest")
    }

    @Test
    fun testRemoveEntry() {
        val cache = LruConcurrentCache<String, Int>(10)
        cache.getOrPut("a") { 1 }
        val removed = cache.remove("a")
        assertEquals(1, removed)
        assertEquals(0, cache.size())
        assertNull(cache.get("a"))
    }

    @Test
    fun testRemoveNonExistent() {
        val cache = LruConcurrentCache<String, Int>(10)
        assertNull(cache.remove("missing"))
    }

    @Test
    fun testClear() {
        val cache = LruConcurrentCache<String, Int>(10)
        cache.getOrPut("a") { 1 }
        cache.getOrPut("b") { 2 }
        cache.clear()
        assertEquals(0, cache.size())
        assertNull(cache.get("a"))
    }

    @Test
    fun testContainsKey() {
        val cache = LruConcurrentCache<String, Int>(10)
        cache.getOrPut("a") { 1 }
        assertTrue(cache.containsKey("a"))
        assertFalse(cache.containsKey("b"))
    }

    @Test
    fun testValues() {
        val cache = LruConcurrentCache<String, Int>(10)
        cache.getOrPut("a") { 1 }
        cache.getOrPut("b") { 2 }
        val values = cache.values().toSet()
        assertEquals(setOf(1, 2), values)
    }

    @Test
    fun testOnEvictCallback() {
        val evicted = mutableListOf<Pair<String, Int>>()
        val cache = LruConcurrentCache<String, Int>(2) { k, v -> evicted.add(k to v) }
        cache.getOrPut("a") { 1 }
        cache.getOrPut("b") { 2 }
        cache.getOrPut("c") { 3 }
        assertEquals(1, evicted.size)
        assertEquals("a" to 1, evicted[0])
    }

    @Test
    fun testGetOrPutDoesNotRecreatExisting() {
        val cache = LruConcurrentCache<String, Int>(10)
        cache.getOrPut("a") { 1 }
        val value = cache.getOrPut("a") { 999 }
        assertEquals(1, value, "should return existing value, not call factory again")
    }

    @Test
    fun testForEachVisitsAll() {
        val cache = LruConcurrentCache<String, Int>(10)
        cache.getOrPut("a") { 1 }
        cache.getOrPut("b") { 2 }
        val seen = mutableMapOf<String, Int>()
        cache.forEach { k, v -> seen[k] = v }
        assertEquals(mapOf("a" to 1, "b" to 2), seen)
    }

    @Test
    fun testCountWithPredicate() {
        val cache = LruConcurrentCache<String, Int>(10)
        cache.getOrPut("a") { 1 }
        cache.getOrPut("b") { 2 }
        cache.getOrPut("c") { 3 }
        assertEquals(2, cache.count { _, v -> v >= 2 })
    }

    @Test
    fun testMaxSizeOneWorks() {
        val cache = LruConcurrentCache<String, Int>(1)
        cache.getOrPut("a") { 1 }
        cache.getOrPut("b") { 2 }
        assertEquals(1, cache.size())
        assertNull(cache.get("a"))
        assertEquals(2, cache.get("b"))
    }

    @Test
    fun testMultipleEvictions() {
        val cache = LruConcurrentCache<Int, Int>(3)
        // Insert 10 items; only last 3 should survive
        for (i in 0 until 10) {
            cache.getOrPut(i) { i * 10 }
        }
        assertEquals(3, cache.size())
        // Entries 7, 8, 9 should survive (most recent)
        assertNotNull(cache.get(7))
        assertNotNull(cache.get(8))
        assertNotNull(cache.get(9))
        assertNull(cache.get(0))
        assertNull(cache.get(6))
    }
}
