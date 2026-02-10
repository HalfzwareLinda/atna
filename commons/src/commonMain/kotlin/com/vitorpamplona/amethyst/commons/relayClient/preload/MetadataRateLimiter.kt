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
package com.vitorpamplona.amethyst.commons.relayClient.preload

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Global rate limiter for metadata requests to prevent thundering herd on fast scroll.
 * Batches pubkeys and processes at a controlled rate.
 *
 * @param maxRequestsPerSecond Maximum requests to process per second (default 20)
 * @param scope CoroutineScope for processing
 */
class MetadataRateLimiter(
    private val maxRequestsPerSecond: Int = 20,
    private val scope: CoroutineScope,
) {
    private val queue = Channel<String>(Channel.BUFFERED)
    private val processed = mutableSetOf<String>()

    /**
     * Enqueue a pubkey for metadata fetching.
     * Duplicates within the same batch are automatically filtered.
     */
    fun enqueue(pubkey: String) {
        if (pubkey !in processed) {
            queue.trySend(pubkey)
        }
    }

    /**
     * Enqueue multiple pubkeys for metadata fetching.
     */
    fun enqueueAll(pubkeys: Collection<String>) {
        pubkeys.forEach { enqueue(it) }
    }

    /**
     * Start processing the queue with rate limiting.
     * @param onRequest Callback invoked for each pubkey to process (legacy single-pubkey)
     */
    fun start(onRequest: suspend (String) -> Unit) {
        startBatched { batch -> batch.forEach { onRequest(it) } }
    }

    /**
     * Start processing the queue with rate limiting and batched callbacks.
     * The callback receives a batch of up to [maxRequestsPerSecond] pubkeys at once,
     * allowing a single subscription with multiple authors instead of N subscriptions.
     *
     * @param onBatch Callback invoked with a batch of pubkeys to process together
     */
    fun startBatched(onBatch: suspend (List<String>) -> Unit) {
        scope.launch {
            while (true) {
                // Wait for first item
                val first = queue.receiveCatching().getOrNull() ?: break
                val batch = mutableListOf<String>()
                if (first !in processed) {
                    batch.add(first)
                    processed.add(first)
                }

                // Collect more items for up to 200ms or until batch is full
                val deadline = System.currentTimeMillis() + 200
                while (batch.size < maxRequestsPerSecond) {
                    val remaining = deadline - System.currentTimeMillis()
                    if (remaining <= 0) break
                    val next = queue.tryReceive().getOrNull() ?: break
                    if (next !in processed) {
                        batch.add(next)
                        processed.add(next)
                    }
                }

                if (batch.isNotEmpty()) {
                    onBatch(batch)
                }
                if (batch.size >= maxRequestsPerSecond) {
                    delay(1000) // Rate limit when processing full batches
                }
            }
        }
    }

    /**
     * Clear the processed set to allow re-fetching.
     * Call this when switching accounts or clearing cache.
     */
    fun reset() {
        processed.clear()
    }

    /**
     * Check if a pubkey has already been processed.
     */
    fun isProcessed(pubkey: String): Boolean = pubkey in processed
}
