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
package com.atna.marmot

import com.vitorpamplona.quartz.marmotMls.MarmotGroupEvent
import com.vitorpamplona.quartz.marmotMls.MarmotKeyPackageRelayListEvent
import com.vitorpamplona.quartz.marmotMls.MarmotWelcomeEvent
import com.vitorpamplona.quartz.nip01Core.core.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Routes incoming Marmot Nostr events to the MarmotManager for MLS processing,
 * and exposes reactive state for the UI layer.
 */
class MarmotEventRouter(
    private val manager: MarmotManager,
    private val scope: CoroutineScope,
) {
    val isInitialized: Boolean get() = manager.isInitialized

    /** If initialization failed, contains the error message. Null if init succeeded or hasn't been attempted. */
    @Volatile
    var initError: String? = null
        private set

    /** Record that initialization failed with the given error message. */
    fun setInitFailed(error: String) {
        initError = error
    }

    /** Clear any previous initialization error (e.g. before re-initializing after re-login). */
    fun clearInitError() {
        initError = null
    }

    /** The current user's public key (hex). Set after login via [setCurrentUserPubkey]. */
    @Volatile
    private var ourPubkey: String = ""

    /** The current user's public key (hex). Used by UI to distinguish own messages. */
    val currentUserPubkey: String get() = ourPubkey

    /** Sets the current user's public key. Must be called after login for welcome events and own-message detection to work. */
    fun setCurrentUserPubkey(pubkey: String) {
        ourPubkey = pubkey
    }

    private val _groups = MutableStateFlow<List<MarmotGroup>>(emptyList())
    val groups: StateFlow<List<MarmotGroup>> = _groups.asStateFlow()

    private val _invites = MutableStateFlow<List<MarmotInvite>>(emptyList())
    val invites: StateFlow<List<MarmotInvite>> = _invites.asStateFlow()

    private val _messagesPerGroup = MutableStateFlow<Map<String, List<MarmotMessage>>>(emptyMap())
    val messagesPerGroup: StateFlow<Map<String, List<MarmotMessage>>> = _messagesPerGroup.asStateFlow()

    /** Emits user-facing error messages. Uses Channel so each error is delivered to exactly one collector (no duplicates during nav transitions). */
    private val _errors = Channel<String>(BUFFERED)
    val errors: Flow<String> = _errors.receiveAsFlow()

    /** Cached per-group message flows to avoid creating new collectors on each call. */
    private val groupMessageFlows = HashMap<String, StateFlow<List<MarmotMessage>>>()
    private val groupFlowsMutex = Mutex()

    /** Returns a cached flow of messages for a specific group, avoiding full-map recompositions. */
    fun messagesForGroup(groupId: String): StateFlow<List<MarmotMessage>> {
        // Fast path: already cached (common case, no lock needed)
        groupMessageFlows[groupId]?.let { return it }
        // Slow path: create new flow under lock. Benign race: at worst a duplicate flow is created.
        val flow =
            _messagesPerGroup
                .map { it[groupId] ?: emptyList() }
                .stateIn(scope, SharingStarted.Eagerly, _messagesPerGroup.value[groupId] ?: emptyList())
        // Use putIfAbsent logic: if another thread inserted while we created, use theirs
        return groupMessageFlows.getOrPut(groupId) { flow }
    }

    /** Tracks Marmot key package relay lists per user pubkey. */
    private val userMarmotRelaysMap = HashMap<String, List<String>>()
    private val relaysMutex = Mutex()
    private val _userMarmotRelays = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val userMarmotRelays: StateFlow<Map<String, List<String>>> = _userMarmotRelays.asStateFlow()

    /**
     * Clears all Marmot data and reinitializes the manager.
     * Used by the "Clear local database" settings option.
     */
    fun clearAndReinitialize(dbPath: String) {
        manager.clearData(dbPath)
        _groups.value = emptyList()
        _invites.value = emptyList()
        _messagesPerGroup.value = emptyMap()
        groupMessageFlows.clear()
        refreshGroups()
        refreshInvites()
    }

    fun onMarmotEvent(event: Event) {
        scope.launch {
            try {
                when (event) {
                    is MarmotKeyPackageRelayListEvent -> handleKeyPackageRelayList(event)
                    is MarmotGroupEvent -> {
                        if (!manager.isInitialized) return@launch
                        handleGroupEvent(event)
                    }
                    is MarmotWelcomeEvent -> {
                        if (!manager.isInitialized) return@launch
                        handleWelcomeEvent(event)
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _errors.trySend("Error processing Marmot event: ${e.message}")
            }
        }
    }

    private suspend fun handleKeyPackageRelayList(event: MarmotKeyPackageRelayListEvent) {
        val relays = event.relays().map { it.url }
        if (relays.isNotEmpty()) {
            relaysMutex.withLock {
                userMarmotRelaysMap[event.pubKey] = relays
                _userMarmotRelays.value = userMarmotRelaysMap.toMap()
            }
        }
    }

    /** Returns the Marmot relay list for a given user, or empty if unknown. */
    fun getMarmotRelaysForUser(pubkey: String): List<String> = _userMarmotRelays.value[pubkey] ?: emptyList()

    private suspend fun handleGroupEvent(event: MarmotGroupEvent) {
        val result = manager.processIncomingMessage(event.toJson())
        when (result) {
            is MarmotProcessResult.Message -> {
                addMessageToGroup(result.message)
                refreshGroups()
            }
            is MarmotProcessResult.Commit -> refreshGroups()
            is MarmotProcessResult.Proposal -> refreshGroups()
            is MarmotProcessResult.Unprocessable -> {}
        }
    }

    private suspend fun handleWelcomeEvent(event: MarmotWelcomeEvent) {
        if (ourPubkey.isNotEmpty()) {
            manager.processWelcome(event.toJson(), ourPubkey)
        }
        refreshInvites()
    }

    private fun addMessageToGroup(message: MarmotMessage) {
        val current = _messagesPerGroup.value
        val groupMessages = current[message.groupId] ?: emptyList()
        // Insert sorted descending by timestamp (newest first) to avoid re-sorting in UI
        val updated =
            buildList(groupMessages.size + 1) {
                var inserted = false
                for (m in groupMessages) {
                    if (!inserted && message.timestamp >= m.timestamp) {
                        add(message)
                        inserted = true
                    }
                    add(m)
                }
                if (!inserted) add(message)
            }
        _messagesPerGroup.value = current + (message.groupId to updated)
    }

    fun refreshGroups() {
        scope.launch {
            try {
                _groups.value = manager.getGroups()
            } catch (e: Exception) {
                _errors.trySend("Failed to refresh groups: ${e.message}")
            }
        }
    }

    fun refreshInvites() {
        scope.launch {
            try {
                _invites.value = manager.getPendingInvites()
            } catch (e: Exception) {
                _errors.trySend("Failed to refresh invites: ${e.message}")
            }
        }
    }

    fun refreshMessagesForGroup(groupId: String) {
        scope.launch {
            try {
                val messages = manager.getMessages(groupId).sortedByDescending { it.timestamp }
                _messagesPerGroup.value = _messagesPerGroup.value + (groupId to messages)
            } catch (e: Exception) {
                _errors.trySend("Failed to refresh messages: ${e.message}")
            }
        }
    }

    suspend fun acceptInvite(welcomeId: String) {
        manager.acceptInvite(welcomeId)
        refreshInvites()
        refreshGroups()
    }

    suspend fun declineInvite(welcomeId: String) {
        manager.declineInvite(welcomeId)
        refreshInvites()
    }

    suspend fun sendMessage(
        groupId: String,
        senderKey: String,
        content: String,
    ): String = manager.sendMessage(groupId, senderKey, content)

    suspend fun createGroup(
        creatorKey: String,
        memberKeyPackages: List<String>,
        name: String,
        description: String,
        relays: List<String>,
        admins: List<String>,
    ): MarmotCreateGroupResult =
        manager.createGroup(
            creatorKey = creatorKey,
            memberKeyPackages = memberKeyPackages,
            name = name,
            description = description,
            relays = relays,
            admins = admins,
        )
}
