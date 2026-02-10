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
package com.vitorpamplona.amethyst.ui.screen.loggedIn.chats.marmotDM

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.atna.marmot.MarmotNewChatState
import com.atna.marmot.normalizeKeyPackageEventJson
import com.atna.marmot.queryRelayForEvent
import com.vitorpamplona.amethyst.Amethyst
import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.model.User
import com.vitorpamplona.quartz.marmotMls.MarmotKeyPackageEvent
import com.vitorpamplona.quartz.marmotMls.MarmotKeyPackageRelayListEvent
import com.vitorpamplona.quartz.nip01Core.core.Event
import com.vitorpamplona.quartz.nip01Core.relay.filters.Filter
import com.vitorpamplona.quartz.nip01Core.relay.normalizer.RelayUrlNormalizer
import com.vitorpamplona.quartz.nip01Core.tags.people.PTag
import com.vitorpamplona.quartz.nip17Dm.messages.ChatMessageEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class MarmotNewChatViewModel(
    val account: Account,
) : ViewModel() {
    var searchValue by mutableStateOf("")
    val searchValueFlow = MutableStateFlow("")
    private val invalidations = MutableStateFlow(0)

    val searchResultsUsers: StateFlow<List<User>> =
        combine(
            searchValueFlow.debounce(100),
            invalidations.debounce(100),
        ) { term, _ ->
            LocalCache.findUsersStartingWith(term, account)
        }.flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, WhileSubscribed(5000), emptyList())

    private val _chatState = MutableStateFlow<MarmotNewChatState>(MarmotNewChatState.Idle)
    val chatState: StateFlow<MarmotNewChatState> = _chatState.asStateFlow()

    fun updateSearchValue(newValue: String) {
        searchValue = newValue
        searchValueFlow.tryEmit(newValue)
    }

    fun clear() = updateSearchValue("")

    fun resetState() {
        _chatState.value = MarmotNewChatState.Idle
    }

    fun startMarmotChat(targetPubkey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val router = Amethyst.instance.marmotRouter

                // Check if Marmot init already failed
                router.initError?.let { error ->
                    _chatState.value = MarmotNewChatState.Error(error)
                    return@launch
                }

                // Wait for Marmot to finish initializing (up to 10s)
                if (!router.isInitialized) {
                    _chatState.value = MarmotNewChatState.Initializing
                    repeat(100) {
                        if (router.isInitialized) return@repeat
                        // Check for init failure during polling
                        router.initError?.let { error ->
                            _chatState.value = MarmotNewChatState.Error(error)
                            return@launch
                        }
                        kotlinx.coroutines.delay(100)
                    }
                    if (!router.isInitialized) {
                        _chatState.value =
                            MarmotNewChatState.Error(
                                router.initError ?: "Marmot is still initializing. Please try again in a moment.",
                            )
                        return@launch
                    }
                }

                _chatState.value = MarmotNewChatState.SearchingRelayList(targetPubkey)

                val client = Amethyst.instance.client

                // Step 1: Look for cached relay list
                var marmotRelays = router.getMarmotRelaysForUser(targetPubkey)

                // Step 2: If not cached, query relays for kind 10051
                if (marmotRelays.isEmpty()) {
                    val relayListEvent =
                        queryRelayForEvent(
                            client = client,
                            filter =
                                Filter(
                                    authors = listOf(targetPubkey),
                                    kinds = listOf(MarmotKeyPackageRelayListEvent.KIND),
                                    limit = 1,
                                ),
                            relayUrls = client.connectedRelaysFlow().value.toList(),
                        )

                    if (relayListEvent is MarmotKeyPackageRelayListEvent) {
                        marmotRelays = relayListEvent.relays().map { it.url }
                    }
                }

                if (marmotRelays.isEmpty()) {
                    _chatState.value = MarmotNewChatState.NoKeyPackage(targetPubkey)
                    return@launch
                }

                // Step 3: Query marmot relays for kind 443 key package
                _chatState.value = MarmotNewChatState.LookingUpKeyPackage(targetPubkey, marmotRelays)

                val normalizedRelays = marmotRelays.mapNotNull { RelayUrlNormalizer.normalizeOrNull(it) }
                val keyPackageEvent =
                    queryRelayForEvent(
                        client = client,
                        filter =
                            Filter(
                                authors = listOf(targetPubkey),
                                kinds = listOf(MarmotKeyPackageEvent.KIND),
                                limit = 1,
                            ),
                        relayUrls = normalizedRelays,
                    )

                if (keyPackageEvent == null) {
                    _chatState.value = MarmotNewChatState.NoKeyPackage(targetPubkey)
                    return@launch
                }

                // Step 4: Create group
                _chatState.value = MarmotNewChatState.CreatingGroup(targetPubkey)

                val targetUser = LocalCache.getUserIfExists(targetPubkey)
                val displayName = targetUser?.toBestDisplayName() ?: targetPubkey.take(8)

                val result =
                    router.createGroup(
                        creatorKey = account.signer.pubKey,
                        memberKeyPackages = listOf(normalizeKeyPackageEventJson(keyPackageEvent)),
                        name = "Chat with $displayName",
                        description = "",
                        relays = marmotRelays,
                        admins = listOf(account.signer.pubKey),
                    )

                // Broadcast welcome events
                val normalizedMarmotRelays = marmotRelays.mapNotNull { RelayUrlNormalizer.normalizeOrNull(it) }.toSet()
                result.welcomeEventJsons.forEach { json ->
                    val event = Event.fromJson(json)
                    client.send(event, normalizedMarmotRelays)
                }

                router.refreshGroups()
                _chatState.value = MarmotNewChatState.GroupCreated(result.group.id, result.group.name)
            } catch (e: Exception) {
                _chatState.value = MarmotNewChatState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun sendNip17Invitation(targetPubkey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val template =
                    ChatMessageEvent.build(
                        msg = MARMOT_INVITE_TEXT,
                        to = listOf(PTag(targetPubkey)),
                    )
                account.sendNip17PrivateMessage(template)
                _chatState.value = MarmotNewChatState.InviteSent(targetPubkey)
            } catch (e: Exception) {
                _chatState.value = MarmotNewChatState.Error(e.message ?: "Failed to send invitation")
            }
        }
    }

    class Factory(
        val account: Account,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MarmotNewChatViewModel(account) as T
    }

    companion object {
        private const val MARMOT_INVITE_TEXT =
            "Hey! I'd like to chat with you using Marmot encrypted messaging. " +
                "Marmot uses the MLS protocol for end-to-end encrypted group chats on Nostr. " +
                "To get started, install an app that supports Marmot and publish your key package."
    }
}
