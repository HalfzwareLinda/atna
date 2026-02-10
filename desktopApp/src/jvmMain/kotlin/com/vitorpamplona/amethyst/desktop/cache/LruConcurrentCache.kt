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

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Thread-safe LRU cache backed by [ConcurrentHashMap] for storage and
 * [ConcurrentLinkedDeque] for approximate access ordering.
 *
 * When the cache exceeds [maxSize], the least-recently-accessed entries
 * are evicted. Access ordering is approximate under high concurrency
 * (the deque may briefly contain duplicates), but the map is always
 * authoritative for membership.
 *
 * @param maxSize Maximum number of entries before eviction starts
 * @param onEvict Optional callback invoked when an entry is evicted
 */
class LruConcurrentCache<K : Any, V : Any>(
    private val maxSize: Int,
    private val onEvict: ((K, V) -> Unit)? = null,
) {
    private val map = ConcurrentHashMap<K, V>(maxSize / 2, 0.75f, 4)
    private val accessOrder = ConcurrentLinkedDeque<K>()

    fun get(key: K): V? {
        val value = map[key] ?: return null
        touchKey(key)
        return value
    }

    fun getOrPut(
        key: K,
        factory: (K) -> V,
    ): V {
        val existing = map[key]
        if (existing != null) {
            touchKey(key)
            return existing
        }
        val newValue = factory(key)
        val race = map.putIfAbsent(key, newValue)
        if (race != null) {
            touchKey(key)
            return race
        }
        accessOrder.addLast(key)
        evictIfNeeded()
        return newValue
    }

    fun remove(key: K): V? {
        accessOrder.remove(key)
        return map.remove(key)
    }

    fun size(): Int = map.size

    fun values(): Collection<V> = map.values

    fun clear() {
        map.clear()
        accessOrder.clear()
    }

    fun forEach(action: (K, V) -> Unit) {
        map.forEach { (k, v) -> action(k, v) }
    }

    fun count(predicate: (K, V) -> Boolean): Int = map.count { (k, v) -> predicate(k, v) }

    fun containsKey(key: K): Boolean = map.containsKey(key)

    private fun touchKey(key: K) {
        accessOrder.remove(key)
        accessOrder.addLast(key)
    }

    private fun evictIfNeeded() {
        while (map.size > maxSize) {
            val oldest = accessOrder.pollFirst() ?: break
            val removed = map.remove(oldest)
            if (removed != null) {
                onEvict?.invoke(oldest, removed)
            }
        }
    }
}
