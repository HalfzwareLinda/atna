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

import com.vitorpamplona.amethyst.commons.model.AddressableNote
import com.vitorpamplona.amethyst.commons.model.Channel
import com.vitorpamplona.amethyst.commons.model.Note
import com.vitorpamplona.amethyst.commons.model.User
import com.vitorpamplona.amethyst.commons.model.cache.ICacheEventStream
import com.vitorpamplona.amethyst.commons.model.cache.ICacheProvider
import com.vitorpamplona.amethyst.commons.services.nwc.NwcPaymentTracker
import com.vitorpamplona.quartz.nip01Core.core.Address
import com.vitorpamplona.quartz.nip01Core.core.Event
import com.vitorpamplona.quartz.nip01Core.core.HexKey
import com.vitorpamplona.quartz.nip01Core.hints.HintIndexer
import com.vitorpamplona.quartz.nip01Core.metadata.MetadataEvent
import com.vitorpamplona.quartz.nip01Core.relay.normalizer.NormalizedRelayUrl
import com.vitorpamplona.quartz.nip19Bech32.decodePublicKeyAsHexOrNull
import com.vitorpamplona.quartz.nip47WalletConnect.LnZapPaymentRequestEvent
import com.vitorpamplona.quartz.nip47WalletConnect.LnZapPaymentResponseEvent
import com.vitorpamplona.quartz.nip65RelayList.AdvertisedRelayListEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Desktop implementation of ICacheProvider.
 *
 * Provides in-memory caching of Users and Notes for the desktop application.
 * Uses [LruConcurrentCache] for bounded, access-ordered eviction of users,
 * notes, and addressable notes to prevent unbounded memory growth.
 */
class DesktopLocalCache : ICacheProvider {
    private val users = LruConcurrentCache<HexKey, User>(MAX_USERS)
    private val notes = LruConcurrentCache<HexKey, Note>(MAX_NOTES)
    private val addressableNotes = LruConcurrentCache<String, AddressableNote>(MAX_ADDRESSABLE)
    private val deletedEvents = ConcurrentHashMap.newKeySet<HexKey>()

    private val eventStream = DesktopCacheEventStream()

    private val _metadataVersion = MutableStateFlow(0)
    val metadataVersion: StateFlow<Int> = _metadataVersion.asStateFlow()

    val paymentTracker = NwcPaymentTracker()

    val relayHints = HintIndexer()

    // ----- User operations -----

    override fun getUserIfExists(pubkey: HexKey): User? = users.get(pubkey)

    override fun getOrCreateUser(pubkey: HexKey): User =
        users.getOrPut(pubkey) {
            // Create placeholder notes for relay lists
            val nip65Note = getOrCreateNote("nip65:$pubkey")
            val dmNote = getOrCreateNote("dm:$pubkey")
            User(pubkey, nip65Note, dmNote)
        }

    override fun countUsers(predicate: (String, User) -> Boolean): Int = users.count { key, user -> predicate(key, user) }

    override fun findUsersStartingWith(
        prefix: String,
        limit: Int,
    ): List<User> {
        if (prefix.isBlank()) return emptyList()

        // Check if it's a valid pubkey/npub first
        val pubkeyHex = decodePublicKeyAsHexOrNull(prefix)
        if (pubkeyHex != null) {
            val user = getUserIfExists(pubkeyHex)
            if (user != null) return listOf(user)
        }

        // Search by name/displayName/nip05/lud16
        return users
            .values()
            .filter { user ->
                user.metadataOrNull()?.anyNameStartsWith(prefix) == true ||
                    user.pubkeyHex.startsWith(prefix, ignoreCase = true) ||
                    user.pubkeyNpub().startsWith(prefix, ignoreCase = true)
            }.sortedWith(
                compareBy(
                    { !it.toBestDisplayName().startsWith(prefix, ignoreCase = true) },
                    { it.toBestDisplayName().lowercase() },
                    { it.pubkeyHex },
                ),
            ).take(limit)
    }

    /**
     * Updates user metadata from a MetadataEvent.
     * Called when receiving kind 0 events from relays.
     */
    fun consumeMetadata(event: MetadataEvent) {
        val user = getOrCreateUser(event.pubKey)

        if (user.metadata().shouldUpdateWith(event)) {
            val newUserMetadata = event.contactMetaData()
            if (newUserMetadata != null) {
                user.updateUserInfo(newUserMetadata, event)
                _metadataVersion.value++
            }
        }
    }

    /**
     * Updates a user's NIP-65 relay list (kind 10002).
     * Stores the event on the user's nip65RelayListNote so that
     * [User.authorRelayList] and [User.writeRelaysNorm] work correctly
     * for outbox model routing.
     */
    fun consumeRelayList(event: AdvertisedRelayListEvent) {
        val user = getOrCreateUser(event.pubKey)
        val note = user.nip65RelayListNote
        val existing = note.event as? AdvertisedRelayListEvent
        if (existing == null || existing.createdAt < event.createdAt) {
            note.loadEvent(event, user, emptyList())
        }
    }

    /**
     * Records relay hints for an incoming event.
     * Populates the [HintIndexer] bloom filters so the outbox model
     * can discover relays for users without explicit relay lists.
     */
    fun recordRelayHint(
        event: Event,
        relay: NormalizedRelayUrl,
    ) {
        relayHints.addEvent(event.id, relay)
        relayHints.addKey(event.pubKey, relay)
    }

    fun stats(): CacheStats =
        CacheStats(
            users = users.size(),
            notes = notes.size(),
            addressableNotes = addressableNotes.size(),
            deletedEvents = deletedEvents.size,
        )

    // ----- NWC Payment operations -----

    /**
     * Consumes a NIP-47 payment request event.
     * Registers the request with the tracker and links it to the zapped note.
     *
     * @param event The payment request event
     * @param zappedNote The note being zapped (if this payment is for a zap)
     * @param relay The relay this event came from
     * @param onResponse Callback invoked when wallet responds
     * @return true if event was processed, false if already seen
     */
    fun consume(
        event: LnZapPaymentRequestEvent,
        zappedNote: Note?,
        relay: NormalizedRelayUrl?,
        onResponse: suspend (LnZapPaymentResponseEvent) -> Unit,
    ): Boolean {
        val note = getOrCreateNote(event.id)
        val author = getOrCreateUser(event.pubKey)

        // Already processed this event
        if (note.event != null) return false

        note.loadEvent(event, author, emptyList())
        relay?.let { note.addRelay(it) }

        zappedNote?.addZapPayment(note, null)
        paymentTracker.registerRequest(event.id, zappedNote, onResponse)

        return true
    }

    /**
     * Consumes a NIP-47 payment response event.
     * Matches to pending request, links notes, and invokes callback.
     *
     * @param event The payment response event
     * @param relay The relay this event came from
     * @return true if event was processed, false if no matching request
     */
    fun consume(
        event: LnZapPaymentResponseEvent,
        relay: NormalizedRelayUrl?,
    ): Boolean {
        val requestId = event.requestId()
        val pending = paymentTracker.onResponseReceived(requestId) ?: return false

        val requestNote = requestId?.let { getNoteIfExists(it) }
        val note = getOrCreateNote(event.id)
        val author = getOrCreateUser(event.pubKey)

        // Already processed this event
        if (note.event != null) return false

        note.loadEvent(event, author, emptyList())
        relay?.let { note.addRelay(it) }

        // Link response to zapped note via request
        requestNote?.let { req -> pending.zappedNote?.addZapPayment(req, note) }

        // Invoke callback on IO dispatcher
        GlobalScope.launch(Dispatchers.IO) {
            pending.onResponse(event)
        }

        return true
    }

    // ----- Note operations -----

    override fun getNoteIfExists(hexKey: HexKey): Note? = notes.get(hexKey)

    override fun checkGetOrCreateNote(hexKey: HexKey): Note = getOrCreateNote(hexKey)

    fun getOrCreateNote(hexKey: HexKey): Note =
        notes.getOrPut(hexKey) {
            Note(hexKey)
        }

    override fun getOrCreateAddressableNote(key: Address): AddressableNote =
        addressableNotes.getOrPut(key.toValue()) {
            AddressableNote(key)
        }

    // ----- Channel operations -----

    override fun getAnyChannel(note: Note): Channel? {
        // Desktop doesn't support channels yet
        return null
    }

    // ----- Deletion tracking -----

    override fun hasBeenDeleted(event: Any): Boolean =
        when (event) {
            is Note -> deletedEvents.contains(event.idHex)
            is Event -> deletedEvents.contains(event.id)
            else -> false
        }

    fun markAsDeleted(eventId: HexKey) {
        deletedEvents.add(eventId)
        trimDeletedEvents()
    }

    private fun trimDeletedEvents() {
        if (deletedEvents.size <= MAX_DELETED) return
        val iter = deletedEvents.iterator()
        val toRemove = deletedEvents.size - MAX_DELETED
        repeat(toRemove) {
            if (iter.hasNext()) {
                iter.next()
                iter.remove()
            }
        }
    }

    // ----- Own event consumption -----

    override fun justConsumeMyOwnEvent(event: Event): Boolean {
        // Desktop doesn't track own events separately
        return false
    }

    // ----- Event stream -----

    override fun getEventStream(): ICacheEventStream = eventStream

    /**
     * Emits a new note bundle to observers.
     */
    suspend fun emitNewNotes(notes: Set<Note>) {
        eventStream.emitNewNotes(notes)
    }

    /**
     * Emits deleted notes to observers.
     */
    suspend fun emitDeletedNotes(notes: Set<Note>) {
        eventStream.emitDeletedNotes(notes)
    }

    // ----- Stats -----

    fun userCount(): Int = users.size()

    fun noteCount(): Int = notes.size()

    fun clear() {
        users.clear()
        notes.clear()
        addressableNotes.clear()
        deletedEvents.clear()
    }

    companion object {
        const val MAX_USERS = 20_000
        const val MAX_NOTES = 50_000
        const val MAX_ADDRESSABLE = 10_000
        const val MAX_DELETED = 10_000
    }
}

/**
 * Desktop implementation of ICacheEventStream.
 */
class DesktopCacheEventStream : ICacheEventStream {
    private val _newEventBundles = MutableSharedFlow<Set<Note>>(replay = 0)
    private val _deletedEventBundles = MutableSharedFlow<Set<Note>>(replay = 0)

    override val newEventBundles: SharedFlow<Set<Note>> = _newEventBundles
    override val deletedEventBundles: SharedFlow<Set<Note>> = _deletedEventBundles

    suspend fun emitNewNotes(notes: Set<Note>) {
        _newEventBundles.emit(notes)
    }

    suspend fun emitDeletedNotes(notes: Set<Note>) {
        _deletedEventBundles.emit(notes)
    }
}
