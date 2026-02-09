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
 * Configuration for event store backends.
 *
 * This data class provides backend-agnostic configuration options that can be
 * interpreted differently by each storage implementation.
 *
 * ## Configuration Examples
 *
 * ### SQLite Backend (Default)
 * ```kotlin
 * EventStoreConfig(
 *     backend = EventStoreProvider.Backend.SQLITE,
 *     dbPath = "/data/data/com.atna/databases/events.db",  // Custom location
 *     maxSizeMB = 1024,  // 1GB limit
 *     enableFts = true   // Full-text search support
 * )
 * ```
 *
 * ### nostrdb Backend (Future)
 * ```kotlin
 * EventStoreConfig(
 *     backend = EventStoreProvider.Backend.NOSTRDB,
 *     dbPath = "/data/data/com.atna/nostrdb",  // LMDB directory
 *     maxSizeMB = 2048,  // 2GB LMDB map size
 *     enableFts = true   // Custom FTS index
 * )
 * ```
 *
 * ## Backend-Specific Behavior
 *
 * - **dbPath**: If null, backends use their default locations
 *   - SQLite: Android app's databases directory
 *   - nostrdb: Platform-specific cache/data directory
 *
 * - **maxSizeMB**: Interpreted differently per backend
 *   - SQLite: Maximum file size or auto-vacuum threshold
 *   - nostrdb: LMDB map size (preallocated virtual memory)
 *
 * - **enableFts**: Full-text search capability
 *   - SQLite: Uses FTS5 virtual tables
 *   - nostrdb: May use separate FTS index or Tantivy
 */
data class EventStoreConfig(
    /**
     * Which storage backend to use.
     * Defaults to SQLITE (production-tested Amethyst implementation).
     */
    val backend: EventStoreProvider.Backend = EventStoreProvider.Backend.SQLITE,
    /**
     * Custom database path for storage files.
     *
     * Platform conventions:
     * - Android: `/data/data/<package>/databases/events.db`
     * - Desktop JVM: `~/.local/share/atna/events.db` (Linux)
     * - Desktop JVM: `~/Library/Application Support/atna/events.db` (macOS)
     *
     * If null, the backend chooses a sensible platform-specific default.
     */
    val dbPath: String? = null,
    /**
     * Maximum database size in megabytes.
     *
     * Interpretation varies by backend:
     * - SQLite: Triggers cleanup/vacuum when exceeded
     * - nostrdb (LMDB): Sets the memory map size (virtual, not physical)
     *
     * Default: 512 MB (conservative for mobile devices)
     */
    val maxSizeMB: Int = 512,
    /**
     * Enable full-text search indexing for event content.
     *
     * When enabled:
     * - SQLite: Creates FTS5 virtual tables for text search
     * - nostrdb: May add separate FTS index (implementation-dependent)
     *
     * Trade-off: Faster searches but increased storage and write overhead.
     *
     * Default: true (search is a core Nostr feature)
     */
    val enableFts: Boolean = true,
) {
    init {
        require(maxSizeMB > 0) { "maxSizeMB must be positive, got: $maxSizeMB" }
    }

    companion object {
        /**
         * Default configuration using SQLite backend.
         * Suitable for most use cases.
         */
        val DEFAULT = EventStoreConfig()

        /**
         * Minimal configuration for testing or embedded scenarios.
         * Disables FTS and uses a small DB size.
         */
        val MINIMAL =
            EventStoreConfig(
                maxSizeMB = 64,
                enableFts = false,
            )

        /**
         * High-capacity configuration for desktop or relay use cases.
         * Enables larger storage and FTS.
         */
        val HIGH_CAPACITY =
            EventStoreConfig(
                maxSizeMB = 4096, // 4GB
                enableFts = true,
            )
    }
}
