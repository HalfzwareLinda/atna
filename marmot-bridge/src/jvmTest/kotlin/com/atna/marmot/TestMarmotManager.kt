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
 * Test-only stub implementation of MarmotManager for unit tests.
 * Returns empty results for read operations and throws for write operations.
 */
class TestMarmotManager : MarmotManager {
    override val isInitialized: Boolean get() = false

    override fun initialize(dbPath: String) {}

    override fun close() {}

    override suspend fun createKeyPackage(
        publicKey: String,
        relays: List<String>,
    ): String = throw UnsupportedOperationException("Test stub")

    override suspend fun createGroup(
        creatorKey: String,
        memberKeyPackages: List<String>,
        name: String,
        description: String,
        relays: List<String>,
        admins: List<String>,
    ): MarmotCreateGroupResult = throw UnsupportedOperationException("Test stub")

    override suspend fun getGroups(): List<MarmotGroup> = emptyList()

    override suspend fun getGroup(groupId: String): MarmotGroup? = null

    override suspend fun getMembers(groupId: String): List<String> = emptyList()

    override suspend fun addMembers(
        groupId: String,
        keyPackages: List<String>,
    ): List<String> = throw UnsupportedOperationException("Test stub")

    override suspend fun removeMembers(
        groupId: String,
        memberKeys: List<String>,
    ) = throw UnsupportedOperationException("Test stub")

    override suspend fun sendMessage(
        groupId: String,
        senderKey: String,
        content: String,
        kind: Int,
        tags: List<List<String>>,
    ): String = throw UnsupportedOperationException("Test stub")

    override suspend fun processIncomingMessage(eventJson: String): MarmotProcessResult = throw UnsupportedOperationException("Test stub")

    override suspend fun getMessages(groupId: String): List<MarmotMessage> = emptyList()

    override suspend fun getPendingInvites(): List<MarmotInvite> = emptyList()

    override suspend fun processWelcome(
        eventJson: String,
        ourPubkey: String,
    ): MarmotInvite = throw UnsupportedOperationException("Test stub")

    override suspend fun acceptInvite(welcomeId: String) = throw UnsupportedOperationException("Test stub")

    override suspend fun declineInvite(welcomeId: String) = throw UnsupportedOperationException("Test stub")
}
