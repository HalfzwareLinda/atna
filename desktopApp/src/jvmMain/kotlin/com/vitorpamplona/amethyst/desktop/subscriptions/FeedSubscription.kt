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
package com.vitorpamplona.amethyst.desktop.subscriptions

import com.vitorpamplona.amethyst.desktop.network.DesktopOutboxResolver
import com.vitorpamplona.quartz.nip01Core.core.Event
import com.vitorpamplona.quartz.nip01Core.relay.filters.Filter
import com.vitorpamplona.quartz.nip01Core.relay.normalizer.NormalizedRelayUrl

/**
 * Feed mode for feed subscriptions.
 */
enum class FeedMode {
    GLOBAL,
    FOLLOWING,
}

/**
 * Feed tab types for the desktop home screen sidebar.
 * Each tab filters for specific Nostr event kinds.
 */
enum class FeedTab(
    val label: String,
    val kinds: List<Int>,
) {
    NOTES("Notes", listOf(1, 6)),
    REPLIES("Replies", listOf(1)),
    MEDIA("Media", listOf(20, 21, 22, 34235, 34236, 1063)),
    ARTICLES("Articles", listOf(30023)),
    LIVE("Live", listOf(30311)),
}

/**
 * Creates a subscription config for global feed (all text notes).
 */
fun createGlobalFeedSubscription(
    relays: Set<NormalizedRelayUrl>,
    limit: Int = 50,
    onEvent: (Event, Boolean, NormalizedRelayUrl, List<Filter>?) -> Unit,
    onEose: (NormalizedRelayUrl, List<Filter>?) -> Unit = { _, _ -> },
): SubscriptionConfig =
    SubscriptionConfig(
        subId = generateSubId("global-feed"),
        filters = listOf(FilterBuilders.textNotesGlobal(limit = limit)),
        relays = relays,
        onEvent = onEvent,
        onEose = onEose,
    )

/**
 * Creates a subscription config for following feed (text notes from followed users).
 */
fun createFollowingFeedSubscription(
    relays: Set<NormalizedRelayUrl>,
    followedUsers: List<String>,
    limit: Int = 50,
    onEvent: (Event, Boolean, NormalizedRelayUrl, List<Filter>?) -> Unit,
    onEose: (NormalizedRelayUrl, List<Filter>?) -> Unit = { _, _ -> },
): SubscriptionConfig? {
    if (followedUsers.isEmpty()) return null

    return SubscriptionConfig(
        subId = generateSubId("following-feed"),
        filters = listOf(FilterBuilders.textNotesFromAuthors(followedUsers, limit = limit)),
        relays = relays,
        onEvent = onEvent,
        onEose = onEose,
    )
}

/**
 * Creates a subscription config for contact list (kind 3).
 */
fun createContactListSubscription(
    relays: Set<NormalizedRelayUrl>,
    pubKeyHex: String,
    onEvent: (Event, Boolean, NormalizedRelayUrl, List<Filter>?) -> Unit,
    onEose: (NormalizedRelayUrl, List<Filter>?) -> Unit = { _, _ -> },
): SubscriptionConfig =
    SubscriptionConfig(
        subId = generateSubId("contacts-${pubKeyHex.take(8)}"),
        filters = listOf(FilterBuilders.contactList(pubKeyHex)),
        relays = relays,
        onEvent = onEvent,
        onEose = onEose,
    )

/**
 * Creates a subscription config for fetching a specific note by ID.
 */
fun createNoteSubscription(
    relays: Set<NormalizedRelayUrl>,
    noteId: String,
    onEvent: (Event, Boolean, NormalizedRelayUrl, List<Filter>?) -> Unit,
    onEose: (NormalizedRelayUrl, List<Filter>?) -> Unit = { _, _ -> },
): SubscriptionConfig =
    SubscriptionConfig(
        subId = generateSubId("note-${noteId.take(8)}"),
        filters = listOf(FilterBuilders.byIds(listOf(noteId))),
        relays = relays,
        onEvent = onEvent,
        onEose = onEose,
    )

/**
 * Creates a subscription config for fetching all replies to a note (thread).
 *
 * @param noteId The root note ID to fetch replies for
 * @param limit Maximum number of reply events to request
 */
fun createThreadRepliesSubscription(
    relays: Set<NormalizedRelayUrl>,
    noteId: String,
    limit: Int = 200,
    onEvent: (Event, Boolean, NormalizedRelayUrl, List<Filter>?) -> Unit,
    onEose: (NormalizedRelayUrl, List<Filter>?) -> Unit = { _, _ -> },
): SubscriptionConfig =
    SubscriptionConfig(
        subId = generateSubId("thread-${noteId.take(8)}"),
        filters =
            listOf(
                FilterBuilders.byETags(
                    eventIds = listOf(noteId),
                    kinds = listOf(1), // TextNoteEvent
                    limit = limit,
                ),
            ),
        relays = relays,
        onEvent = onEvent,
        onEose = onEose,
    )

/**
 * Creates a NIP-50 search subscription for user profiles.
 * Requires NIP-50 compatible relays (e.g., relay.nostr.band, nostr.wine).
 *
 * @param searchQuery Text to search for in user profiles
 * @param limit Maximum results to return
 */
fun createSearchPeopleSubscription(
    relays: Set<NormalizedRelayUrl>,
    searchQuery: String,
    limit: Int = 50,
    onEvent: (Event, Boolean, NormalizedRelayUrl, List<Filter>?) -> Unit,
    onEose: (NormalizedRelayUrl, List<Filter>?) -> Unit = { _, _ -> },
): SubscriptionConfig? {
    if (searchQuery.isBlank()) return null

    return SubscriptionConfig(
        subId = generateSubId("search-people-${searchQuery.take(8)}"),
        filters = listOf(FilterBuilders.searchPeople(searchQuery, limit)),
        relays = relays,
        onEvent = onEvent,
        onEose = onEose,
    )
}

/**
 * Creates a NIP-50 search subscription for text notes.
 * Requires NIP-50 compatible relays.
 *
 * @param searchQuery Text to search for in notes
 * @param limit Maximum results to return
 */
fun createSearchNotesSubscription(
    relays: Set<NormalizedRelayUrl>,
    searchQuery: String,
    limit: Int = 50,
    onEvent: (Event, Boolean, NormalizedRelayUrl, List<Filter>?) -> Unit,
    onEose: (NormalizedRelayUrl, List<Filter>?) -> Unit = { _, _ -> },
): SubscriptionConfig? {
    if (searchQuery.isBlank()) return null

    return SubscriptionConfig(
        subId = generateSubId("search-notes-${searchQuery.take(8)}"),
        filters = listOf(FilterBuilders.searchNotes(searchQuery, limit)),
        relays = relays,
        onEvent = onEvent,
        onEose = onEose,
    )
}

/**
 * Creates a subscription for zap receipts (kind 9735) for specific events.
 *
 * @param eventIds Event IDs to get zaps for
 * @param limit Maximum zaps per event
 */
fun createZapsSubscription(
    relays: Set<NormalizedRelayUrl>,
    eventIds: List<String>,
    limit: Int = 100,
    onEvent: (Event, Boolean, NormalizedRelayUrl, List<Filter>?) -> Unit,
    onEose: (NormalizedRelayUrl, List<Filter>?) -> Unit = { _, _ -> },
): SubscriptionConfig? {
    if (eventIds.isEmpty()) return null

    return SubscriptionConfig(
        subId = generateSubId("zaps-${eventIds.first().take(8)}"),
        filters = listOf(FilterBuilders.zapsForEvents(eventIds, limit)),
        relays = relays,
        onEvent = onEvent,
        onEose = onEose,
    )
}

/**
 * Creates a subscription for reactions (kind 7) for specific events.
 *
 * @param eventIds Event IDs to get reactions for
 * @param limit Maximum reactions per event
 */
fun createReactionsSubscription(
    relays: Set<NormalizedRelayUrl>,
    eventIds: List<String>,
    limit: Int = 100,
    onEvent: (Event, Boolean, NormalizedRelayUrl, List<Filter>?) -> Unit,
    onEose: (NormalizedRelayUrl, List<Filter>?) -> Unit = { _, _ -> },
): SubscriptionConfig? {
    if (eventIds.isEmpty()) return null

    return SubscriptionConfig(
        subId = generateSubId("reactions-${eventIds.first().take(8)}"),
        filters = listOf(FilterBuilders.reactionsForEvents(eventIds, limit)),
        relays = relays,
        onEvent = onEvent,
        onEose = onEose,
    )
}

/**
 * Creates a subscription for replies (kind 1) to specific events.
 *
 * @param eventIds Event IDs to get replies for
 * @param limit Maximum replies per event
 */
fun createRepliesSubscription(
    relays: Set<NormalizedRelayUrl>,
    eventIds: List<String>,
    limit: Int = 100,
    onEvent: (Event, Boolean, NormalizedRelayUrl, List<Filter>?) -> Unit,
    onEose: (NormalizedRelayUrl, List<Filter>?) -> Unit = { _, _ -> },
): SubscriptionConfig? {
    if (eventIds.isEmpty()) return null

    return SubscriptionConfig(
        subId = generateSubId("replies-${eventIds.first().take(8)}"),
        filters = listOf(FilterBuilders.repliesForEvents(eventIds, limit)),
        relays = relays,
        onEvent = onEvent,
        onEose = onEose,
    )
}

/**
 * Creates a subscription for reposts (kind 6) of specific events.
 *
 * @param eventIds Event IDs to get reposts for
 * @param limit Maximum reposts per event
 */
fun createRepostsSubscription(
    relays: Set<NormalizedRelayUrl>,
    eventIds: List<String>,
    limit: Int = 100,
    onEvent: (Event, Boolean, NormalizedRelayUrl, List<Filter>?) -> Unit,
    onEose: (NormalizedRelayUrl, List<Filter>?) -> Unit = { _, _ -> },
): SubscriptionConfig? {
    if (eventIds.isEmpty()) return null

    return SubscriptionConfig(
        subId = generateSubId("reposts-${eventIds.first().take(8)}"),
        filters = listOf(FilterBuilders.repostsForEvents(eventIds, limit)),
        relays = relays,
        onEvent = onEvent,
        onEose = onEose,
    )
}

/**
 * Creates a subscription config for global long-form content (kind 30023, NIP-23).
 */
fun createLongFormFeedSubscription(
    relays: Set<NormalizedRelayUrl>,
    limit: Int = 30,
    onEvent: (Event, Boolean, NormalizedRelayUrl, List<Filter>?) -> Unit,
    onEose: (NormalizedRelayUrl, List<Filter>?) -> Unit = { _, _ -> },
): SubscriptionConfig =
    SubscriptionConfig(
        subId = generateSubId("longform-feed"),
        filters = listOf(FilterBuilders.longFormGlobal(limit = limit)),
        relays = relays,
        onEvent = onEvent,
        onEose = onEose,
    )

/**
 * Creates a subscription config for long-form content from followed users.
 */
fun createFollowingLongFormFeedSubscription(
    relays: Set<NormalizedRelayUrl>,
    followedUsers: List<String>,
    limit: Int = 30,
    onEvent: (Event, Boolean, NormalizedRelayUrl, List<Filter>?) -> Unit,
    onEose: (NormalizedRelayUrl, List<Filter>?) -> Unit = { _, _ -> },
): SubscriptionConfig? {
    if (followedUsers.isEmpty()) return null

    return SubscriptionConfig(
        subId = generateSubId("longform-following"),
        filters = listOf(FilterBuilders.longFormFromAuthors(followedUsers, limit = limit)),
        relays = relays,
        onEvent = onEvent,
        onEose = onEose,
    )
}

/**
 * Creates a single consolidated subscription for all engagement metrics:
 * zaps (9735), reactions (7), replies (1 with e-tag), and reposts (6).
 * Reduces 4 separate relay subscriptions to 1.
 *
 * @param eventIds Event IDs to get engagement for (capped to 50 by caller)
 * @param limit Maximum events per kind
 */
fun createEngagementSubscription(
    relays: Set<NormalizedRelayUrl>,
    eventIds: List<String>,
    limit: Int = 100,
    onEvent: (Event, Boolean, NormalizedRelayUrl, List<Filter>?) -> Unit,
    onEose: (NormalizedRelayUrl, List<Filter>?) -> Unit = { _, _ -> },
): SubscriptionConfig? {
    if (eventIds.isEmpty()) return null

    return SubscriptionConfig(
        subId = generateSubId("engagement-${eventIds.first().take(8)}"),
        filters =
            listOf(
                FilterBuilders.zapsForEvents(eventIds, limit),
                FilterBuilders.reactionsForEvents(eventIds, limit),
                FilterBuilders.repliesForEvents(eventIds, limit),
                FilterBuilders.repostsForEvents(eventIds, limit),
            ),
        relays = relays,
        onEvent = onEvent,
        onEose = onEose,
    )
}

/**
 * Creates a subscription config for global media feed (NIP-68 pictures, NIP-71 videos, NIP-94 file metadata).
 */
fun createMediaFeedSubscription(
    relays: Set<NormalizedRelayUrl>,
    limit: Int = 50,
    onEvent: (Event, Boolean, NormalizedRelayUrl, List<Filter>?) -> Unit,
    onEose: (NormalizedRelayUrl, List<Filter>?) -> Unit = { _, _ -> },
): SubscriptionConfig =
    SubscriptionConfig(
        subId = generateSubId("media-feed"),
        filters = listOf(FilterBuilders.mediaGlobal(limit = limit)),
        relays = relays,
        onEvent = onEvent,
        onEose = onEose,
    )

/**
 * Creates a subscription config for media feed from followed users.
 */
fun createFollowingMediaFeedSubscription(
    relays: Set<NormalizedRelayUrl>,
    followedUsers: List<String>,
    limit: Int = 50,
    onEvent: (Event, Boolean, NormalizedRelayUrl, List<Filter>?) -> Unit,
    onEose: (NormalizedRelayUrl, List<Filter>?) -> Unit = { _, _ -> },
): SubscriptionConfig? {
    if (followedUsers.isEmpty()) return null

    return SubscriptionConfig(
        subId = generateSubId("media-following"),
        filters = listOf(FilterBuilders.mediaFromAuthors(followedUsers, limit = limit)),
        relays = relays,
        onEvent = onEvent,
        onEose = onEose,
    )
}

/**
 * Creates a subscription config for global live activities feed (NIP-53).
 */
fun createLiveFeedSubscription(
    relays: Set<NormalizedRelayUrl>,
    limit: Int = 50,
    onEvent: (Event, Boolean, NormalizedRelayUrl, List<Filter>?) -> Unit,
    onEose: (NormalizedRelayUrl, List<Filter>?) -> Unit = { _, _ -> },
): SubscriptionConfig =
    SubscriptionConfig(
        subId = generateSubId("live-feed"),
        filters = listOf(FilterBuilders.liveActivitiesGlobal(limit = limit)),
        relays = relays,
        onEvent = onEvent,
        onEose = onEose,
    )

/**
 * Creates a subscription config for live activities from followed users.
 */
fun createFollowingLiveFeedSubscription(
    relays: Set<NormalizedRelayUrl>,
    followedUsers: List<String>,
    limit: Int = 50,
    onEvent: (Event, Boolean, NormalizedRelayUrl, List<Filter>?) -> Unit,
    onEose: (NormalizedRelayUrl, List<Filter>?) -> Unit = { _, _ -> },
): SubscriptionConfig? {
    if (followedUsers.isEmpty()) return null

    return SubscriptionConfig(
        subId = generateSubId("live-following"),
        filters = listOf(FilterBuilders.liveActivitiesFromAuthors(followedUsers, limit = limit)),
        relays = relays,
        onEvent = onEvent,
        onEose = onEose,
    )
}

// ----- Outbox-aware subscription builders -----

/**
 * Creates an outbox-routed subscription for following feed.
 * Routes text note filters to each user's WRITE relays via the outbox model.
 */
fun createOutboxFollowingFeedSubscription(
    outboxResolver: DesktopOutboxResolver,
    followedUsers: List<String>,
    fallbackRelays: Set<NormalizedRelayUrl>,
    limit: Int = 50,
    onEvent: (Event, Boolean, NormalizedRelayUrl, List<Filter>?) -> Unit,
    onEose: (NormalizedRelayUrl, List<Filter>?) -> Unit = { _, _ -> },
): RoutedSubscriptionConfig? {
    if (followedUsers.isEmpty()) return null

    val filterMap = outboxResolver.resolveOutboxFilters(followedUsers, listOf(1), limit, fallbackRelays)
    if (filterMap.isEmpty()) return null

    return RoutedSubscriptionConfig(
        subId = generateSubId("outbox-following"),
        filterMap = filterMap,
        onEvent = onEvent,
        onEose = onEose,
    )
}

/**
 * Creates an outbox-routed subscription for long-form content from followed users.
 */
fun createOutboxFollowingLongFormFeedSubscription(
    outboxResolver: DesktopOutboxResolver,
    followedUsers: List<String>,
    fallbackRelays: Set<NormalizedRelayUrl>,
    limit: Int = 30,
    onEvent: (Event, Boolean, NormalizedRelayUrl, List<Filter>?) -> Unit,
    onEose: (NormalizedRelayUrl, List<Filter>?) -> Unit = { _, _ -> },
): RoutedSubscriptionConfig? {
    if (followedUsers.isEmpty()) return null

    val filterMap = outboxResolver.resolveOutboxFilters(followedUsers, listOf(30023), limit, fallbackRelays)
    if (filterMap.isEmpty()) return null

    return RoutedSubscriptionConfig(
        subId = generateSubId("outbox-longform"),
        filterMap = filterMap,
        onEvent = onEvent,
        onEose = onEose,
    )
}

/**
 * Creates a generic outbox-routed subscription for any event kinds from followed users.
 * Used by all feed tabs to route filters to each user's WRITE relays.
 */
fun createOutboxFollowingGenericFeedSubscription(
    outboxResolver: DesktopOutboxResolver,
    followedUsers: List<String>,
    kinds: List<Int>,
    fallbackRelays: Set<NormalizedRelayUrl>,
    limit: Int = 50,
    onEvent: (Event, Boolean, NormalizedRelayUrl, List<Filter>?) -> Unit,
    onEose: (NormalizedRelayUrl, List<Filter>?) -> Unit = { _, _ -> },
): RoutedSubscriptionConfig? {
    if (followedUsers.isEmpty()) return null

    val filterMap = outboxResolver.resolveOutboxFilters(followedUsers, kinds, limit, fallbackRelays)
    if (filterMap.isEmpty()) return null

    return RoutedSubscriptionConfig(
        subId = generateSubId("outbox-${kinds.joinToString("-")}"),
        filterMap = filterMap,
        onEvent = onEvent,
        onEose = onEose,
    )
}
