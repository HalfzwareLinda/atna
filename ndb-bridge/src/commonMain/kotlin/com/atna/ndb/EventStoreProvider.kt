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

/**
 * Factory for creating event store instances.
 *
 * This module provides a pluggable backend architecture for Nostr event storage.
 * Currently delegates to Amethyst's built-in SQLite EventStore (in quartz module),
 * with nostrdb backend planned for future implementation.
 *
 * ## Available Backends
 *
 * - **SQLITE** (Default): Amethyst's built-in SQLite event store
 *   - Location: `quartz/src/androidMain/kotlin/.../EventStore.kt`
 *   - Pros: Production-tested, full-featured
 *   - Cons: Android-only (uses `android.content.Context`)
 *
 * - **NOSTRDB** (Planned): High-performance LMDB-based storage
 *   - Implementation: Via rust-nostr or direct LMDB bindings
 *   - Pros: Cross-platform, optimized for Nostr workloads
 *   - Status: Research phase - rust-nostr Kotlin bindings don't expose local DB ops
 *
 * ## Usage
 *
 * ```kotlin
 * // Configure preferred backend
 * EventStoreProvider.preferredBackend = EventStoreProvider.Backend.SQLITE
 *
 * // In the future, when nostrdb is available:
 * EventStoreProvider.preferredBackend = EventStoreProvider.Backend.NOSTRDB
 * ```
 *
 * ## Architecture
 *
 * This module acts as an abstraction layer that allows switching between storage
 * backends without modifying application code. The actual IEventStore implementation
 * remains in the quartz module's androidMain source set for now.
 */
object EventStoreProvider {
    /**
     * Available storage backend implementations.
     */
    enum class Backend {
        /**
         * Default SQLite-based storage using Amethyst's built-in EventStore.
         * Currently implemented in quartz/src/androidMain only.
         */
        SQLITE,

        /**
         * Planned nostrdb backend using LMDB for high-performance storage.
         * Future implementation will support cross-platform (Android + JVM Desktop).
         */
        NOSTRDB,
    }

    /**
     * The preferred backend to use for event storage.
     * Defaults to SQLITE (Amethyst's production implementation).
     *
     * Note: Changing this at runtime requires app restart to take effect.
     */
    var preferredBackend: Backend = Backend.SQLITE
}
