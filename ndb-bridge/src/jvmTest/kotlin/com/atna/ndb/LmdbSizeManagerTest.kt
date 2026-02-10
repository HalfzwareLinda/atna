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

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LmdbSizeManagerTest {
    @Test
    fun testGetDbSizeMBOnEmptyDir() {
        val tmpDir = System.getProperty("java.io.tmpdir") + "/atna-test-size-" + System.currentTimeMillis()
        java.io.File(tmpDir).mkdirs()
        try {
            val lmdb = LmdbEventStore(tmpDir)
            lmdb.open()
            val manager = LmdbSizeManager(tmpDir, maxSizeMB = 512, store = lmdb)
            // LMDB creates data.mdb on open, so size should be >= 0
            assertTrue(manager.getDbSizeMB() >= 0)
            lmdb.close()
        } finally {
            java.io.File(tmpDir).deleteRecursively()
        }
    }

    @Test
    fun testIsNearLimitAndIsOverLimit() {
        val tmpDir = System.getProperty("java.io.tmpdir") + "/atna-test-limits-" + System.currentTimeMillis()
        java.io.File(tmpDir).mkdirs()
        try {
            val lmdb = LmdbEventStore(tmpDir)
            lmdb.open()
            // With maxSizeMB=512 and a tiny fresh DB, should not be near or over
            val manager = LmdbSizeManager(tmpDir, maxSizeMB = 512, store = lmdb)
            assertFalse(manager.isNearLimit())
            assertFalse(manager.isOverLimit())
            lmdb.close()
        } finally {
            java.io.File(tmpDir).deleteRecursively()
        }
    }

    @Test
    fun testIsOverLimitWithTinyMax() {
        val tmpDir = System.getProperty("java.io.tmpdir") + "/atna-test-tiny-" + System.currentTimeMillis()
        java.io.File(tmpDir).mkdirs()
        try {
            val lmdb = LmdbEventStore(tmpDir)
            lmdb.open()
            // With maxSizeMB=0 (technically invalid but testing boundary),
            // any DB should be "over limit". Use maxSizeMB=1 since we require positive.
            // A fresh LMDB data.mdb is typically 8KB-32KB, so 0MB threshold won't trigger.
            // But if we set maxSizeMB to very small, isNearLimit should depend on file size.
            val manager = LmdbSizeManager(tmpDir, maxSizeMB = 1, store = lmdb)
            // Fresh DB is < 1MB so should not be over limit
            assertFalse(manager.isOverLimit())
            lmdb.close()
        } finally {
            java.io.File(tmpDir).deleteRecursively()
        }
    }

    @Test
    fun testEnforceSizeLimitDoesNotThrowOnEmptyDb() =
        runTest {
            val tmpDir = System.getProperty("java.io.tmpdir") + "/atna-test-enforce-" + System.currentTimeMillis()
            java.io.File(tmpDir).mkdirs()
            try {
                val lmdb = LmdbEventStore(tmpDir)
                lmdb.open()
                val manager = LmdbSizeManager(tmpDir, maxSizeMB = 512, store = lmdb)
                // Should not throw on empty DB
                manager.enforceSizeLimit()
                lmdb.close()
            } finally {
                java.io.File(tmpDir).deleteRecursively()
            }
        }

    @Test
    fun testNormalPruneDoesNotThrowOnEmptyDb() =
        runTest {
            val tmpDir = System.getProperty("java.io.tmpdir") + "/atna-test-prune-" + System.currentTimeMillis()
            java.io.File(tmpDir).mkdirs()
            try {
                val lmdb = LmdbEventStore(tmpDir)
                lmdb.open()
                val manager = LmdbSizeManager(tmpDir, maxSizeMB = 512, store = lmdb)
                manager.normalPrune()
                lmdb.close()
            } finally {
                java.io.File(tmpDir).deleteRecursively()
            }
        }

    @Test
    fun testGetDbSizeMBOnNonExistentDir() {
        val fakePath = "/tmp/nonexistent-atna-test-" + System.currentTimeMillis()
        val lmdb = LmdbEventStore(fakePath)
        // Don't open — just test size manager on non-existent dir
        val manager = LmdbSizeManager(fakePath, maxSizeMB = 512, store = lmdb)
        assertEquals(0, manager.getDbSizeMB())
    }
}
