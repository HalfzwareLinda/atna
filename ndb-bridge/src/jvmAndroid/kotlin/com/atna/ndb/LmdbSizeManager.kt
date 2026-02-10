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
import com.vitorpamplona.quartz.utils.Log
import com.vitorpamplona.quartz.utils.TimeUtils
import java.io.File

/**
 * Manages LMDB database size with tiered enforcement.
 *
 * Retention periods inspired by Damus/Notedeck analysis: both apps store
 * everything and never prune (32 GiB / 1 TiB headroom). ATNA keeps kind-based
 * filtering with generous retention, appropriate for a 4 GB desktop target.
 *
 * Three tiers:
 * - Under 85%: normal time-based prune (14d / 60d / 180d)
 * - 85%-100%: moderate prune (7d / 30d / 90d)
 * - Over 100%: aggressive prune (3d / 14d / 30d + profiles + metadata)
 */
class LmdbSizeManager(
    private val dbPath: String,
    private val maxSizeMB: Int,
    private val store: LmdbEventStore,
) {
    fun getDbSizeMB(): Long {
        val dataFile = File(dbPath, "data.mdb")
        return if (dataFile.exists()) dataFile.length() / (1024 * 1024) else 0
    }

    fun isNearLimit(): Boolean = getDbSizeMB() > maxSizeMB * 85 / 100

    fun isOverLimit(): Boolean = getDbSizeMB() > maxSizeMB

    /**
     * Enforces size limits with tiered pruning strategy.
     *
     * Always runs the normal time-based prune first. If the DB is near or
     * over the limit, progressively more aggressive pruning is applied.
     */
    suspend fun enforceSizeLimit() {
        val sizeBefore = getDbSizeMB()

        // Always run normal prune
        normalPrune()

        if (sizeBefore <= maxSizeMB * 85 / 100) return

        if (sizeBefore <= maxSizeMB) {
            // Tier 2: near limit — halved retention
            Log.d(TAG, "LMDB near limit (${sizeBefore}MB / ${maxSizeMB}MB), moderate prune")
            moderatePrune()
            return
        }

        // Tier 3: over limit — aggressive
        Log.w(TAG, "LMDB over limit (${sizeBefore}MB / ${maxSizeMB}MB), aggressive prune")
        aggressivePrune()

        val sizeAfter = getDbSizeMB()
        Log.d(TAG, "LMDB after aggressive prune: ${sizeAfter}MB / ${maxSizeMB}MB")
    }

    /**
     * Normal prune: generous retention for a desktop app.
     * - Engagement (reactions, zaps, reposts, highlights, live chat): 14 days
     * - Content (notes, media, channels, Marmot, live, status): 60 days
     * - Private/long-lived (DMs, gift wraps, long-form, trust): 180 days
     */
    internal suspend fun normalPrune() {
        val now = TimeUtils.now()
        val d = EventPersistenceService.DAY_SECS
        store.delete(Filter(kinds = EventPersistenceService.ENGAGEMENT_KINDS.toList(), until = now - 14 * d))
        store.delete(Filter(kinds = EventPersistenceService.CONTENT_KINDS.toList(), until = now - 60 * d))
        store.delete(Filter(kinds = EventPersistenceService.PRIVATE_KINDS.toList(), until = now - 180 * d))
    }

    /**
     * Moderate prune: halved retention when approaching size limit.
     * - Engagement: 7 days
     * - Content: 30 days
     * - Private: 90 days
     */
    private suspend fun moderatePrune() {
        val now = TimeUtils.now()
        val d = EventPersistenceService.DAY_SECS
        store.delete(Filter(kinds = EventPersistenceService.ENGAGEMENT_KINDS.toList(), until = now - 7 * d))
        store.delete(Filter(kinds = EventPersistenceService.CONTENT_KINDS.toList(), until = now - 30 * d))
        store.delete(Filter(kinds = EventPersistenceService.PRIVATE_KINDS.toList(), until = now - 90 * d))
    }

    /**
     * Aggressive prune: tightest retention + prune normally-permanent kinds.
     * - Engagement: 3 days
     * - Content: 14 days
     * - Private: 30 days
     * - Profiles: 30 days
     * - Metadata/lists: 365 days
     */
    private suspend fun aggressivePrune() {
        val now = TimeUtils.now()
        val d = EventPersistenceService.DAY_SECS
        store.delete(Filter(kinds = EventPersistenceService.ENGAGEMENT_KINDS.toList(), until = now - 3 * d))
        store.delete(Filter(kinds = EventPersistenceService.CONTENT_KINDS.toList(), until = now - 14 * d))
        store.delete(Filter(kinds = EventPersistenceService.PRIVATE_KINDS.toList(), until = now - 30 * d))
        // Prune profiles under extreme space pressure
        store.delete(Filter(kinds = listOf(0), until = now - 30 * d))
        // Prune stale metadata/lists (normally never deleted)
        store.delete(
            Filter(
                kinds = EventPersistenceService.METADATA_KINDS.toList(),
                until = now - 365 * d,
            ),
        )
    }

    companion object {
        private const val TAG = "LmdbSizeManager"
    }
}
