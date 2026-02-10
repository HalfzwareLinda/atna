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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vitorpamplona.amethyst.commons.ui.components.EmptyState
import com.vitorpamplona.amethyst.commons.ui.components.LoadingState
import com.vitorpamplona.amethyst.commons.ui.thread.drawReplyLevel
import com.vitorpamplona.amethyst.desktop.account.AccountState
import com.vitorpamplona.amethyst.desktop.cache.DesktopLocalCache
import com.vitorpamplona.amethyst.desktop.network.DesktopRelayConnectionManager
import com.vitorpamplona.amethyst.desktop.subscriptions.DesktopRelaySubscriptionsCoordinator
import com.vitorpamplona.amethyst.desktop.subscriptions.FeedMode
import com.vitorpamplona.amethyst.desktop.subscriptions.FeedTab
import com.vitorpamplona.amethyst.desktop.subscriptions.FilterBuilders
import com.vitorpamplona.amethyst.desktop.subscriptions.SubscriptionConfig
import com.vitorpamplona.amethyst.desktop.subscriptions.createContactListSubscription
import com.vitorpamplona.amethyst.desktop.subscriptions.createEngagementSubscription
import com.vitorpamplona.amethyst.desktop.subscriptions.createOutboxFollowingGenericFeedSubscription
import com.vitorpamplona.amethyst.desktop.subscriptions.rememberRoutedSubscription
import com.vitorpamplona.amethyst.desktop.subscriptions.rememberSubscription
import com.vitorpamplona.amethyst.desktop.ui.feed.NewNotesPill
import com.vitorpamplona.amethyst.desktop.ui.note.EmbeddedNoteCard
import com.vitorpamplona.amethyst.desktop.ui.note.NoteCard
import com.vitorpamplona.quartz.nip01Core.core.Event
import com.vitorpamplona.quartz.nip01Core.metadata.MetadataEvent
import com.vitorpamplona.quartz.nip02FollowList.ContactListEvent
import com.vitorpamplona.quartz.nip18Reposts.RepostEvent
import com.vitorpamplona.quartz.nip19Bech32.Nip19Parser
import com.vitorpamplona.quartz.nip23LongContent.LongTextNoteEvent
import com.vitorpamplona.quartz.nip25Reactions.ReactionEvent
import com.vitorpamplona.quartz.nip51Lists.bookmarkList.BookmarkListEvent
import com.vitorpamplona.quartz.nip53LiveActivities.streaming.LiveActivitiesEvent
import com.vitorpamplona.quartz.nip53LiveActivities.streaming.tags.StatusTag
import com.vitorpamplona.quartz.nip57Zaps.LnZapEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Note card with action buttons.
 */
@Composable
fun FeedNoteCard(
    event: Event,
    relayManager: DesktopRelayConnectionManager,
    localCache: DesktopLocalCache,
    account: AccountState.LoggedIn?,
    nwcConnection: com.vitorpamplona.quartz.nip47WalletConnect.Nip47WalletConnect.Nip47URINorm? = null,
    onReply: () -> Unit,
    onQuote: () -> Unit,
    onZapFeedback: (ZapFeedback) -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToThread: (String) -> Unit = {},
    onNavigateToHashtag: (String) -> Unit = {},
    zapReceipts: List<ZapReceipt> = emptyList(),
    reactionCount: Int = 0,
    replyCount: Int = 0,
    repostCount: Int = 0,
    bookmarkList: BookmarkListEvent? = null,
    isBookmarked: Boolean = false,
    onBookmarkChanged: (BookmarkListEvent) -> Unit = {},
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
    inlineReplies: List<Event> = emptyList(),
    totalReplyCount: Int = 0,
    showParentNote: Boolean = false,
) {
    val zapAmountSats = zapReceipts.sumOf { it.amountSats }

    // Observe metadata version so profile names/pictures update when metadata arrives
    @Suppress("UNUSED_VARIABLE")
    val metadataVersion by localCache.metadataVersion.collectAsState()
    val noteData = remember(event.id, metadataVersion) { event.toNoteDisplayData(localCache) }

    Column {
        // Show parent note preview for replies (Replies tab)
        if (showParentNote) {
            val parentId = remember(event.id) { findReplyToId(event) }
            if (parentId != null) {
                // Find relay hint from the e-tag
                val relayHint =
                    remember(event.id) {
                        event.tags
                            .filter { it.size >= 2 && it[0] == "e" }
                            .find { it[1] == parentId }
                            ?.getOrNull(2)
                            ?.takeIf { it.isNotEmpty() }
                    }
                EmbeddedNoteCard(
                    eventIdHex = parentId,
                    relayHint = relayHint,
                    localCache = localCache,
                    relayManager = relayManager,
                    onNoteClick = onNavigateToThread,
                    onMentionClick = onNavigateToProfile,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }

        // Main note content with overflow menu positioned top-right
        Box {
            Column(
                modifier =
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onToggleExpand() },
                    ),
            ) {
                NoteCard(
                    note = noteData,
                    onAuthorClick = onNavigateToProfile,
                    localCache = localCache,
                    relayManager = relayManager,
                    onMentionClick = onNavigateToProfile,
                    onNoteClick = onNavigateToThread,
                    onHashtagClick = onNavigateToHashtag,
                )

                // Action buttons (only if logged in)
                if (account != null) {
                    NoteActionsRow(
                        event = event,
                        relayManager = relayManager,
                        localCache = localCache,
                        account = account,
                        nwcConnection = nwcConnection,
                        onReplyClick = onReply,
                        onQuoteClick = onQuote,
                        onZapFeedback = onZapFeedback,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        zapCount = zapReceipts.size,
                        zapAmountSats = zapAmountSats,
                        zapReceipts = zapReceipts,
                        reactionCount = reactionCount,
                        replyCount = replyCount,
                        repostCount = repostCount,
                        bookmarkList = bookmarkList,
                        isBookmarked = isBookmarked,
                        onBookmarkChanged = onBookmarkChanged,
                    )
                }
            }

            // Overflow menu (three dots) in top-right corner
            if (account != null) {
                NoteOverflowMenu(
                    event = event,
                    relayManager = relayManager,
                    account = account,
                    isBookmarked = isBookmarked,
                    bookmarkList = bookmarkList,
                    onBookmarkChanged = onBookmarkChanged,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 4.dp),
                )
            }
        }

        // Inline replies (shown when expanded)
        if (isExpanded) {
            if (inlineReplies.isNotEmpty()) {
                inlineReplies.forEach { replyEvent ->
                    val replyNoteData = remember(replyEvent.id, metadataVersion) { replyEvent.toNoteDisplayData(localCache) }
                    Column(
                        modifier =
                            Modifier
                                .drawReplyLevel(
                                    level = 1,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    selected = MaterialTheme.colorScheme.outlineVariant,
                                ).clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onNavigateToThread(replyEvent.id) },
                                ),
                    ) {
                        NoteCard(
                            note = replyNoteData,
                            onAuthorClick = onNavigateToProfile,
                            localCache = localCache,
                            relayManager = relayManager,
                            onMentionClick = onNavigateToProfile,
                            onNoteClick = onNavigateToThread,
                            onHashtagClick = onNavigateToHashtag,
                        )
                    }
                }
            } else if (totalReplyCount > 0) {
                Text(
                    text = "Loading replies...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
                )
            }

            // "View full thread" link
            Text(
                text =
                    if (totalReplyCount > inlineReplies.size) {
                        "View all $totalReplyCount replies"
                    } else {
                        "View full thread"
                    },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier
                        .padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onNavigateToThread(event.id) },
                        ),
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    }
}

@Composable
fun FeedScreen(
    tab: FeedTab = FeedTab.NOTES,
    relayManager: DesktopRelayConnectionManager,
    localCache: DesktopLocalCache,
    account: AccountState.LoggedIn? = null,
    nwcConnection: com.vitorpamplona.quartz.nip47WalletConnect.Nip47WalletConnect.Nip47URINorm? = null,
    subscriptionsCoordinator: DesktopRelaySubscriptionsCoordinator? = null,
    outboxResolver: com.vitorpamplona.amethyst.desktop.network.DesktopOutboxResolver? = null,
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToThread: (String) -> Unit = {},
    onNavigateToHashtag: (String) -> Unit = {},
    onZapFeedback: (ZapFeedback) -> Unit = {},
) {
    val connectedRelays by relayManager.connectedRelays.collectAsState()
    val relayStatuses by relayManager.relayStatuses.collectAsState()

    val scope = rememberCoroutineScope()

    // Per-tab state holders that persist across tab switches
    val tabStates =
        remember {
            FeedTab.entries.associateWith { FeedTabState(it, scope) }
        }
    val activeTabState = tabStates[tab]!!
    val events by activeTabState.eventState.items.collectAsState()
    val pendingNewCount by activeTabState.eventState.pendingNewCount.collectAsState()
    val engagementMetrics = activeTabState.engagementMetrics
    val engagementVersion by engagementMetrics.version.collectAsState()
    val initialLoadComplete = activeTabState.eoseReceivedCount > 0

    var replyToEvent by remember { mutableStateOf<Event?>(null) }
    var quoteEvent by remember { mutableStateOf<Event?>(null) }
    var expandedNoteId by remember { mutableStateOf<String?>(null) }
    val feedMode = FeedMode.FOLLOWING
    var followedUsers by remember { mutableStateOf<Set<String>>(emptySet()) }

    var bookmarkList by remember { mutableStateOf<BookmarkListEvent?>(null) }
    var bookmarkedEventIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Reset expanded note when switching tabs
    LaunchedEffect(tab) {
        expandedNoteId = null
    }

    // ---- Shared subscriptions (same across all tabs) ----

    // Load followed users for Following feed mode
    rememberSubscription(relayStatuses, account, feedMode, relayManager = relayManager) {
        val configuredRelays = relayStatuses.keys
        if (configuredRelays.isNotEmpty() && account != null && feedMode == FeedMode.FOLLOWING) {
            createContactListSubscription(
                relays = configuredRelays,
                pubKeyHex = account.pubKeyHex,
                onEvent = { event, _, _, _ ->
                    if (event is ContactListEvent) {
                        val users = event.verifiedFollowKeySet()
                        scope.launch(Dispatchers.Main) { followedUsers = users }
                    }
                },
            )
        } else {
            null
        }
    }

    // Timeout: if no contact list arrives within 30s, proceed with empty set
    var followedUsersTimedOut by remember { mutableStateOf(false) }
    LaunchedEffect(feedMode, connectedRelays.size) {
        if (feedMode == FeedMode.FOLLOWING) {
            followedUsersTimedOut = false
            delay(30_000)
            if (followedUsers.isEmpty()) {
                followedUsersTimedOut = true
            }
        }
    }

    // Load NIP-65 relay lists for followed users (bootstraps outbox model)
    LaunchedEffect(followedUsers) {
        if (followedUsers.isNotEmpty()) {
            subscriptionsCoordinator?.loadRelayLists(followedUsers.toList().take(500))
        }
    }

    // Load user's bookmark list
    rememberSubscription(relayStatuses, account, relayManager = relayManager) {
        val configuredRelays = relayStatuses.keys
        if (configuredRelays.isNotEmpty() && account != null) {
            SubscriptionConfig(
                subId = "bookmarks-${account.pubKeyHex.take(8)}",
                filters =
                    listOf(
                        FilterBuilders.byAuthors(
                            authors = listOf(account.pubKeyHex),
                            kinds = listOf(BookmarkListEvent.KIND),
                            limit = 1,
                        ),
                    ),
                relays = configuredRelays,
                onEvent = { event, _, _, _ ->
                    if (event is BookmarkListEvent) {
                        val pubIds =
                            event
                                .publicBookmarks()
                                .filterIsInstance<com.vitorpamplona.quartz.nip51Lists.bookmarkList.tags.EventBookmark>()
                                .map { it.eventId }
                                .toSet()
                        scope.launch(Dispatchers.Main) {
                            bookmarkList = event
                            bookmarkedEventIds = pubIds
                        }
                    }
                },
                onEose = { _, _ -> },
            )
        } else {
            null
        }
    }

    // ---- Active tab subscriptions (change when tab changes) ----

    // Helper: client-side filter for events based on active tab
    val addEventToTab: (Event) -> Unit =
        remember(tab) {
            { event: Event ->
                if (event is MetadataEvent) {
                    localCache.consumeMetadata(event)
                }
                when (tab) {
                    FeedTab.NOTES -> {
                        // Exclude replies (events with e-tags are replies)
                        val isReply = event.tags.any { it.size >= 2 && it[0] == "e" }
                        if (!isReply) {
                            activeTabState.eventState.addItem(event)
                        }
                    }
                    FeedTab.REPLIES -> {
                        // Only include replies (events with e-tags)
                        val isReply = event.tags.any { it.size >= 2 && it[0] == "e" }
                        if (isReply) {
                            activeTabState.eventState.addItem(event)
                        }
                    }
                    else -> {
                        activeTabState.eventState.addItem(event)
                    }
                }
            }
        }

    val onEoseForTab: (com.vitorpamplona.quartz.nip01Core.relay.normalizer.NormalizedRelayUrl, List<com.vitorpamplona.quartz.nip01Core.relay.filters.Filter>?) -> Unit =
        remember(tab) {
            { _, _ ->
                scope.launch(Dispatchers.Main) { activeTabState.eoseReceivedCount++ }
            }
        }

    // Outbox-routed subscription for FOLLOWING mode
    rememberRoutedSubscription(relayStatuses, tab, followedUsers, outboxResolver, relayManager = relayManager) {
        if (followedUsers.isEmpty() || outboxResolver == null) {
            return@rememberRoutedSubscription null
        }
        createOutboxFollowingGenericFeedSubscription(
            outboxResolver = outboxResolver,
            followedUsers = followedUsers.toList(),
            kinds = tab.kinds,
            fallbackRelays = relayStatuses.keys,
            onEvent = { event, _, _, _ -> addEventToTab(event) },
            onEose = onEoseForTab,
        )
    }

    // Broadcast fallback subscription (used when outbox not available)
    rememberSubscription(relayStatuses, tab, followedUsers, outboxResolver, relayManager = relayManager) {
        val configuredRelays = relayStatuses.keys
        if (configuredRelays.isEmpty()) return@rememberSubscription null
        // Skip broadcast when outbox resolver is active
        if (outboxResolver != null) return@rememberSubscription null
        if (followedUsers.isEmpty()) return@rememberSubscription null

        SubscriptionConfig(
            subId =
                com.vitorpamplona.amethyst.desktop.subscriptions
                    .generateSubId("feed-${tab.name.lowercase()}"),
            filters =
                listOf(
                    FilterBuilders.byAuthors(
                        authors = followedUsers.toList(),
                        kinds = tab.kinds,
                        limit = if (tab == FeedTab.LIVE) 30 else 50,
                    ),
                ),
            relays = configuredRelays,
            onEvent = { event, _, _, _ -> addEventToTab(event) },
            onEose = onEoseForTab,
        )
    }

    // Engagement subscriptions (only for Notes and Replies tabs)
    val showEngagement = tab == FeedTab.NOTES || tab == FeedTab.REPLIES
    val eventIds =
        remember(events, showEngagement) {
            if (showEngagement) events.take(50).map { it.id } else emptyList()
        }

    rememberSubscription(relayStatuses, eventIds, relayManager = relayManager) {
        val configuredRelays = relayStatuses.keys
        if (configuredRelays.isEmpty() || eventIds.isEmpty()) {
            return@rememberSubscription null
        }

        createEngagementSubscription(
            relays = configuredRelays,
            eventIds = eventIds,
            onEvent = { event, _, _, _ ->
                when (event) {
                    is LnZapEvent -> {
                        val receipt = event.toZapReceipt(localCache) ?: return@createEngagementSubscription
                        val targetEventId = event.zappedPost().firstOrNull() ?: return@createEngagementSubscription
                        engagementMetrics.addZap(targetEventId, receipt)
                    }
                    is ReactionEvent -> {
                        val targetEventId = event.originalPost().firstOrNull() ?: return@createEngagementSubscription
                        engagementMetrics.addReaction(targetEventId, event.id)
                    }
                    is RepostEvent -> {
                        val targetEventId = event.boostedEventId() ?: return@createEngagementSubscription
                        engagementMetrics.addRepost(targetEventId, event.id)
                    }
                    else -> {
                        val replyToId =
                            event.tags
                                .filter { it.size >= 2 && it[0] == "e" }
                                .lastOrNull()
                                ?.get(1) ?: return@createEngagementSubscription
                        if (replyToId in eventIds) {
                            engagementMetrics.addReply(replyToId, event.id, event)
                        }
                    }
                }
            },
        )
    }

    // Metadata loading for authors
    val authorPubkeys = remember(events) { events.map { it.pubKey }.distinct() }
    LaunchedEffect(authorPubkeys, subscriptionsCoordinator) {
        if (subscriptionsCoordinator != null && authorPubkeys.isNotEmpty()) {
            subscriptionsCoordinator.loadMetadataForPubkeys(authorPubkeys)
        }
    }

    // Metadata loading for mentioned pubkeys (NIP-27 nostr:npub/nprofile in content)
    val mentionedPubkeys =
        remember(events) {
            events
                .flatMap { event ->
                    Nip19Parser
                        .parseAll(event.content)
                        .mapNotNull { entity ->
                            when (entity) {
                                is com.vitorpamplona.quartz.nip19Bech32.entities.NPub -> entity.hex
                                is com.vitorpamplona.quartz.nip19Bech32.entities.NProfile -> entity.hex
                                else -> null
                            }
                        }
                }.distinct()
        }
    LaunchedEffect(mentionedPubkeys, subscriptionsCoordinator) {
        if (subscriptionsCoordinator != null && mentionedPubkeys.isNotEmpty()) {
            subscriptionsCoordinator.loadMetadataForPubkeys(mentionedPubkeys)
        }
    }

    // Metadata for zap senders (Notes/Replies only)
    if (showEngagement) {
        val zapSenderPubkeys =
            remember(engagementVersion) {
                engagementMetrics.allZapSenderPubkeys()
            }
        LaunchedEffect(zapSenderPubkeys, subscriptionsCoordinator) {
            if (subscriptionsCoordinator != null && zapSenderPubkeys.isNotEmpty()) {
                subscriptionsCoordinator.loadMetadataForPubkeys(zapSenderPubkeys)
            }
        }

        // Load metadata for inline reply authors when a note is expanded
        LaunchedEffect(expandedNoteId, engagementVersion) {
            if (expandedNoteId != null && subscriptionsCoordinator != null) {
                val replyAuthors =
                    engagementMetrics
                        .getReplyEvents(expandedNoteId!!, limit = 3)
                        .map { it.pubKey }
                        .distinct()
                if (replyAuthors.isNotEmpty()) {
                    subscriptionsCoordinator.loadMetadataForPubkeys(replyAuthors)
                }
            }
        }
    }

    // ---- UI ----

    Column(modifier = Modifier.fillMaxSize()) {
        if (connectedRelays.isEmpty()) {
            LoadingState("Connecting to relays...")
        } else if (feedMode == FeedMode.FOLLOWING && followedUsers.isEmpty() && !followedUsersTimedOut) {
            LoadingState("Loading followed users...")
        } else if (feedMode == FeedMode.FOLLOWING && followedUsers.isEmpty() && followedUsersTimedOut) {
            EmptyState(
                title = "No contact list found",
                description = "Could not load your followed users. Check your relay connections or try switching to Global feed.",
                onRefresh = { relayManager.connect() },
            )
        } else if (events.isEmpty() && !initialLoadComplete) {
            LoadingState("Loading ${tab.label.lowercase()}...")
        } else if (events.isEmpty() && initialLoadComplete) {
            EmptyState(
                title = "No ${tab.label.lowercase()} from followed users",
                description = "${tab.label} from people you follow will appear here",
                onRefresh = { relayManager.connect() },
            )
        } else {
            when (tab) {
                FeedTab.NOTES, FeedTab.REPLIES -> {
                    NotesFeedContent(
                        events = events,
                        pendingNewCount = pendingNewCount,
                        onReleasePending = { activeTabState.eventState.releasePending() },
                        onHoldStateChanged = { isAtTop ->
                            activeTabState.eventState.setHoldNewItems(!isAtTop)
                        },
                        engagementMetrics = engagementMetrics,
                        engagementVersion = engagementVersion,
                        expandedNoteId = expandedNoteId,
                        onToggleExpand = { noteId ->
                            expandedNoteId = if (expandedNoteId == noteId) null else noteId
                        },
                        relayManager = relayManager,
                        localCache = localCache,
                        account = account,
                        nwcConnection = nwcConnection,
                        bookmarkList = bookmarkList,
                        bookmarkedEventIds = bookmarkedEventIds,
                        onBookmarkChanged = { newList ->
                            bookmarkList = newList
                            val pubIds =
                                newList
                                    .publicBookmarks()
                                    .filterIsInstance<com.vitorpamplona.quartz.nip51Lists.bookmarkList.tags.EventBookmark>()
                                    .map { it.eventId }
                                    .toSet()
                            bookmarkedEventIds = pubIds
                        },
                        onReply = { replyToEvent = it },
                        onQuote = { quoteEvent = it },
                        onZapFeedback = onZapFeedback,
                        onNavigateToProfile = onNavigateToProfile,
                        onNavigateToThread = onNavigateToThread,
                        onNavigateToHashtag = onNavigateToHashtag,
                        showParentNote = tab == FeedTab.REPLIES,
                    )
                }
                FeedTab.MEDIA -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(events, key = { it.id }) { event ->
                            MediaCard(
                                event = event,
                                localCache = localCache,
                                onAuthorClick = onNavigateToProfile,
                                onClick = { onNavigateToThread(event.id) },
                            )
                        }
                    }
                }
                FeedTab.ARTICLES -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(events, key = { it.id }) { event ->
                            if (event is LongTextNoteEvent) {
                                LongFormCard(
                                    event = event,
                                    localCache = localCache,
                                    onAuthorClick = onNavigateToProfile,
                                    onClick = { onNavigateToThread(event.id) },
                                )
                            }
                        }
                    }
                }
                FeedTab.LIVE -> {
                    // Sort: LIVE first, PLANNED second, ENDED last
                    val sortedEvents =
                        remember(events) {
                            events.sortedWith(
                                compareBy<Event> { event ->
                                    if (event is LiveActivitiesEvent) {
                                        when (event.checkStatus(event.status())) {
                                            StatusTag.STATUS.LIVE -> 0
                                            StatusTag.STATUS.PLANNED -> 1
                                            StatusTag.STATUS.ENDED -> 2
                                            else -> 3
                                        }
                                    } else {
                                        3
                                    }
                                }.thenByDescending { it.createdAt },
                            )
                        }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(sortedEvents, key = { it.id }) { event ->
                            if (event is LiveActivitiesEvent) {
                                LiveActivityCard(
                                    event = event,
                                    localCache = localCache,
                                    onAuthorClick = onNavigateToProfile,
                                    onClick = {
                                        val url = event.streaming()
                                        if (url != null) {
                                            try {
                                                java.awt.Desktop
                                                    .getDesktop()
                                                    .browse(java.net.URI(url))
                                            } catch (_: Exception) {
                                                // Fallback: navigate to thread
                                                onNavigateToThread(event.id)
                                            }
                                        } else {
                                            onNavigateToThread(event.id)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Reply dialog
        if (replyToEvent != null && account != null) {
            ComposeNoteDialog(
                onDismiss = { replyToEvent = null },
                relayManager = relayManager,
                account = account,
                replyTo = replyToEvent,
            )
        }

        // Quote dialog
        if (quoteEvent != null && account != null) {
            ComposeNoteDialog(
                onDismiss = { quoteEvent = null },
                relayManager = relayManager,
                account = account,
                quotedEvent = quoteEvent,
            )
        }
    }
}

/**
 * Extracted LazyColumn for Notes and Replies feeds with engagement metrics.
 */
@Composable
private fun NotesFeedContent(
    events: List<Event>,
    pendingNewCount: Int,
    onReleasePending: () -> Unit,
    onHoldStateChanged: (Boolean) -> Unit,
    engagementMetrics: EngagementMetrics,
    engagementVersion: Int,
    expandedNoteId: String?,
    onToggleExpand: (String) -> Unit,
    relayManager: DesktopRelayConnectionManager,
    localCache: DesktopLocalCache,
    account: AccountState.LoggedIn?,
    nwcConnection: com.vitorpamplona.quartz.nip47WalletConnect.Nip47WalletConnect.Nip47URINorm?,
    bookmarkList: BookmarkListEvent?,
    bookmarkedEventIds: Set<String>,
    onBookmarkChanged: (BookmarkListEvent) -> Unit,
    onReply: (Event) -> Unit,
    onQuote: (Event) -> Unit,
    onZapFeedback: (ZapFeedback) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToThread: (String) -> Unit,
    onNavigateToHashtag: (String) -> Unit = {},
    showParentNote: Boolean = false,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val isAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 &&
                listState.firstVisibleItemScrollOffset < 50
        }
    }

    // Toggle hold mode based on scroll position
    LaunchedEffect(isAtTop) {
        onHoldStateChanged(isAtTop)
        if (isAtTop && pendingNewCount > 0) {
            onReleasePending()
        }
    }

    Box {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(events, key = { it.id }) { event ->
                val zapReceipts = remember(event.id, engagementVersion) { engagementMetrics.getZaps(event.id) }
                val reactionCount = remember(event.id, engagementVersion) { engagementMetrics.getReactionCount(event.id) }
                val replyCount = remember(event.id, engagementVersion) { engagementMetrics.getReplyCount(event.id) }
                val repostCount = remember(event.id, engagementVersion) { engagementMetrics.getRepostCount(event.id) }

                val isExpanded = expandedNoteId == event.id
                val inlineReplies =
                    remember(event.id, engagementVersion, isExpanded) {
                        if (isExpanded) engagementMetrics.getReplyEvents(event.id, limit = 3) else emptyList()
                    }

                FeedNoteCard(
                    event = event,
                    relayManager = relayManager,
                    localCache = localCache,
                    account = account,
                    nwcConnection = nwcConnection,
                    onReply = { onReply(event) },
                    onQuote = { onQuote(event) },
                    onZapFeedback = onZapFeedback,
                    onNavigateToProfile = onNavigateToProfile,
                    onNavigateToThread = onNavigateToThread,
                    onNavigateToHashtag = onNavigateToHashtag,
                    zapReceipts = zapReceipts,
                    reactionCount = reactionCount,
                    replyCount = replyCount,
                    repostCount = repostCount,
                    bookmarkList = bookmarkList,
                    isBookmarked = bookmarkedEventIds.contains(event.id),
                    onBookmarkChanged = onBookmarkChanged,
                    isExpanded = isExpanded,
                    onToggleExpand = { onToggleExpand(event.id) },
                    inlineReplies = inlineReplies,
                    totalReplyCount = replyCount,
                    showParentNote = showParentNote,
                )
            }
        }

        // "New notes" indicator pill
        if (pendingNewCount > 0 && !isAtTop) {
            NewNotesPill(
                count = pendingNewCount,
                onClick = {
                    onReleasePending()
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp),
            )
        }
    }
}

/**
 * Holds engagement metrics (zaps, reactions, replies, reposts) using ConcurrentHashMaps.
 * Mutations don't trigger recomposition individually — instead, a debounced version counter
 * emits at most once per 500ms to batch-notify the UI.
 */
class EngagementMetrics {
    private val zaps = java.util.concurrent.ConcurrentHashMap<String, MutableList<ZapReceipt>>()
    private val reactionIds = java.util.concurrent.ConcurrentHashMap<String, MutableSet<String>>()
    private val replyIds = java.util.concurrent.ConcurrentHashMap<String, MutableSet<String>>()
    private val replyEventsList = java.util.concurrent.ConcurrentHashMap<String, MutableList<Event>>()
    private val repostIds = java.util.concurrent.ConcurrentHashMap<String, MutableSet<String>>()

    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var debounceJob: Job? = null

    private fun bumpVersion() {
        debounceJob?.cancel()
        debounceJob =
            scope.launch {
                delay(500)
                _version.value++
            }
    }

    fun addZap(
        eventId: String,
        receipt: ZapReceipt,
    ) {
        val list = zaps.getOrPut(eventId) { mutableListOf() }
        synchronized(list) {
            if (list.none { it.createdAt == receipt.createdAt && it.senderPubKey == receipt.senderPubKey }) {
                list.add(receipt)
            }
        }
        bumpVersion()
    }

    fun addReaction(
        eventId: String,
        reactionId: String,
    ) {
        val set =
            reactionIds.getOrPut(eventId) {
                java.util.concurrent.ConcurrentHashMap
                    .newKeySet()
            }
        if (set.add(reactionId)) bumpVersion()
    }

    fun addReply(
        eventId: String,
        replyId: String,
        replyEvent: Event? = null,
    ) {
        val set =
            replyIds.getOrPut(eventId) {
                java.util.concurrent.ConcurrentHashMap
                    .newKeySet()
            }
        if (set.add(replyId)) {
            if (replyEvent != null) {
                val list = replyEventsList.getOrPut(eventId) { mutableListOf() }
                synchronized(list) {
                    if (list.none { it.id == replyId }) {
                        list.add(replyEvent)
                    }
                }
            }
            bumpVersion()
        }
    }

    fun addRepost(
        eventId: String,
        repostId: String,
    ) {
        val set =
            repostIds.getOrPut(eventId) {
                java.util.concurrent.ConcurrentHashMap
                    .newKeySet()
            }
        if (set.add(repostId)) bumpVersion()
    }

    fun getZaps(eventId: String): List<ZapReceipt> = zaps[eventId]?.toList() ?: emptyList()

    fun getReactionCount(eventId: String): Int = reactionIds[eventId]?.size ?: 0

    fun getReplyCount(eventId: String): Int = replyIds[eventId]?.size ?: 0

    fun getReplyEvents(
        eventId: String,
        limit: Int = 3,
    ): List<Event> {
        val list = replyEventsList[eventId] ?: return emptyList()
        synchronized(list) {
            return list.sortedBy { it.createdAt }.take(limit)
        }
    }

    fun getRepostCount(eventId: String): Int = repostIds[eventId]?.size ?: 0

    fun allZapSenderPubkeys(): List<String> =
        zaps.values
            .flatMap { it.toList() }
            .map { it.senderPubKey }
            .distinct()
}
