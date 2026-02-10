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

import com.vitorpamplona.quartz.utils.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Periodically monitors in-memory cache sizes and JVM heap usage.
 *
 * Logs cache stats every [MONITOR_INTERVAL_MS] and warns when JVM heap
 * usage exceeds 80%. The LRU caches handle eviction automatically;
 * this class provides observability.
 */
class DesktopMemoryManager(
    private val localCache: DesktopLocalCache,
    private val scope: CoroutineScope,
) {
    private var monitorJob: Job? = null

    fun start() {
        if (monitorJob != null) return
        monitorJob =
            scope.launch(Dispatchers.IO) {
                delay(INITIAL_DELAY_MS)
                while (true) {
                    try {
                        val stats = localCache.stats()
                        Log.d(
                            TAG,
                            "Cache: ${stats.users} users, ${stats.notes} notes, " +
                                "${stats.addressableNotes} addressable, ${stats.deletedEvents} deleted",
                        )

                        val runtime = Runtime.getRuntime()
                        val usedMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                        val maxMB = runtime.maxMemory() / (1024 * 1024)
                        if (usedMB > maxMB * 80 / 100) {
                            Log.w(TAG, "High heap usage: ${usedMB}MB / ${maxMB}MB (80% threshold)")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Monitor tick failed: ${e.message}", e)
                    }
                    delay(MONITOR_INTERVAL_MS)
                }
            }
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
    }

    companion object {
        private const val TAG = "DesktopMemoryManager"
        private const val MONITOR_INTERVAL_MS = 2 * 60 * 1000L // 2 minutes
        private const val INITIAL_DELAY_MS = 30_000L // 30 seconds after startup
    }
}

data class CacheStats(
    val users: Int,
    val notes: Int,
    val addressableNotes: Int,
    val deletedEvents: Int,
)
