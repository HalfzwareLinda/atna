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

/**
 * State machine for the "new Marmot chat" flow. Shared between Android and Desktop.
 *
 * Flow: Idle -> SearchingRelayList -> LookingUpKeyPackage -> CreatingGroup -> GroupCreated
 * Alt:  SearchingRelayList -> NoKeyPackage -> (NIP-17 invite on Android) -> InviteSent
 * Error: Any state -> Error
 */
sealed class MarmotNewChatState {
    data object Idle : MarmotNewChatState()

    data object Initializing : MarmotNewChatState()

    data class SearchingRelayList(
        val pubkey: String,
    ) : MarmotNewChatState()

    data class LookingUpKeyPackage(
        val pubkey: String,
        val relays: List<String>,
    ) : MarmotNewChatState()

    data class CreatingGroup(
        val pubkey: String,
    ) : MarmotNewChatState()

    data class GroupCreated(
        val groupId: String,
        val groupName: String,
    ) : MarmotNewChatState()

    data class NoKeyPackage(
        val pubkey: String,
    ) : MarmotNewChatState()

    data class Error(
        val message: String,
    ) : MarmotNewChatState()

    data class InviteSent(
        val pubkey: String,
    ) : MarmotNewChatState()

    val isLoading: Boolean
        get() =
            this is Initializing ||
                this is SearchingRelayList ||
                this is LookingUpKeyPackage ||
                this is CreatingGroup
}
