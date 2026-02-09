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
import com.vitorpamplona.quartz.marmotMls.MarmotWelcomeEvent
import com.vitorpamplona.quartz.nip01Core.core.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Routes incoming Marmot Nostr events to the MarmotManager for MLS processing,
 * and exposes reactive state for the UI layer.
 */
class MarmotEventRouter(
    private val manager: MarmotManager,
    private val scope: CoroutineScope,
    private val ourPubkey: String = "",
) {
    private val _groups = MutableStateFlow<List<MarmotGroup>>(emptyList())
    val groups: StateFlow<List<MarmotGroup>> = _groups.asStateFlow()

    private val _invites = MutableStateFlow<List<MarmotInvite>>(emptyList())
    val invites: StateFlow<List<MarmotInvite>> = _invites.asStateFlow()

    private val _messagesPerGroup = MutableStateFlow<Map<String, List<MarmotMessage>>>(emptyMap())
    val messagesPerGroup: StateFlow<Map<String, List<MarmotMessage>>> = _messagesPerGroup.asStateFlow()

    fun onMarmotEvent(event: Event) {
        if (!manager.isInitialized) return

        scope.launch {
            try {
                when (event) {
                    is MarmotGroupEvent -> handleGroupEvent(event)
                    is MarmotWelcomeEvent -> handleWelcomeEvent(event)
                    else -> {}
                }
            } catch (e: Exception) {
                System.err.println("MarmotEventRouter: error processing event ${event.kind}: ${e.message}")
            }
        }
    }

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
        val current = _messagesPerGroup.value.toMutableMap()
        val groupMessages = current.getOrDefault(message.groupId, emptyList()).toMutableList()
        groupMessages.add(message)
        current[message.groupId] = groupMessages
        _messagesPerGroup.value = current
    }

    fun refreshGroups() {
        scope.launch {
            try {
                _groups.value = manager.getGroups()
            } catch (e: Exception) {
                System.err.println("MarmotEventRouter: error refreshing groups: ${e.message}")
            }
        }
    }

    fun refreshInvites() {
        scope.launch {
            try {
                _invites.value = manager.getPendingInvites()
            } catch (e: Exception) {
                System.err.println("MarmotEventRouter: error refreshing invites: ${e.message}")
            }
        }
    }

    fun refreshMessagesForGroup(groupId: String) {
        scope.launch {
            try {
                val messages = manager.getMessages(groupId)
                val current = _messagesPerGroup.value.toMutableMap()
                current[groupId] = messages
                _messagesPerGroup.value = current
            } catch (e: Exception) {
                System.err.println("MarmotEventRouter: error refreshing messages: ${e.message}")
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
}
