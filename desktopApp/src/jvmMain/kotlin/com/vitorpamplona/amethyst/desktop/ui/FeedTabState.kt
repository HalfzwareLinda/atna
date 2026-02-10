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
package com.vitorpamplona.amethyst.desktop.ui

import com.vitorpamplona.amethyst.commons.state.EventCollectionState
import com.vitorpamplona.amethyst.desktop.subscriptions.FeedTab
import com.vitorpamplona.quartz.nip01Core.core.Event
import kotlinx.coroutines.CoroutineScope

/**
 * Holds per-tab state for feed tabs.
 * Each tab gets its own event collection and engagement metrics
 * that persist across tab switches.
 */
class FeedTabState(
    val tab: FeedTab,
    scope: CoroutineScope,
) {
    val eventState =
        EventCollectionState<Event>(
            getId = { it.id },
            sortComparator = compareByDescending { it.createdAt },
            maxSize = if (tab == FeedTab.LIVE) 50 else 200,
            scope = scope,
        )

    val engagementMetrics = EngagementMetrics()

    var eoseReceivedCount: Int = 0

    fun clear() {
        eventState.clear()
        eoseReceivedCount = 0
    }
}
