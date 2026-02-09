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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * JVM implementation of MarmotManager wrapping mdk-kotlin.
 *
 * This implementation uses the mdk-kotlin library and JNA to provide
 * Marmot encrypted group messaging on desktop JVM platforms.
 *
 * Note: This implementation requires the mdk-kotlin library to be available.
 * If the library is not found at runtime, operations will throw exceptions.
 */
class MdkMarmotManager : MarmotManager {
    private var mdk: Any? = null

    override fun initialize(dbPath: String) {
        try {
            // Attempt to create MDK instance
            // mdk = newMdk(dbPath)
            throw NotImplementedError("MDK initialization not yet implemented - requires mdk-kotlin library")
        } catch (e: Exception) {
            throw IllegalStateException("Failed to initialize Marmot: ${e.message}", e)
        }
    }

    override fun close() {
        mdk = null
    }

    override suspend fun createKeyPackage(
        publicKey: String,
        relays: List<String>,
    ): String =
        withContext(Dispatchers.IO) {
            requireInitialized()
            try {
                // return mdk.createKeyPackageForEvent(publicKey, relays)
                throw NotImplementedError("createKeyPackage not yet implemented - requires mdk-kotlin library")
            } catch (e: Exception) {
                throw IllegalStateException("Failed to create key package: ${e.message}", e)
            }
        }

    override suspend fun createGroup(
        creatorKey: String,
        memberKeyPackages: List<String>,
        name: String,
        description: String,
        relays: List<String>,
        admins: List<String>,
    ): MarmotGroup =
        withContext(Dispatchers.IO) {
            requireInitialized()
            try {
                // val groupJson = mdk.createGroup(creatorKey, memberKeyPackages.toJson(), name, description, relays, admins)
                // return parseGroupJson(groupJson)
                throw NotImplementedError("createGroup not yet implemented - requires mdk-kotlin library")
            } catch (e: Exception) {
                throw IllegalStateException("Failed to create group: ${e.message}", e)
            }
        }

    override suspend fun getGroups(): List<MarmotGroup> =
        withContext(Dispatchers.IO) {
            requireInitialized()
            try {
                // val groupsJson = mdk.getGroups()
                // return parseGroupsJson(groupsJson)
                throw NotImplementedError("getGroups not yet implemented - requires mdk-kotlin library")
            } catch (e: Exception) {
                throw IllegalStateException("Failed to get groups: ${e.message}", e)
            }
        }

    override suspend fun getGroup(groupId: String): MarmotGroup? =
        withContext(Dispatchers.IO) {
            requireInitialized()
            try {
                // val groupJson = mdk.getGroup(groupId) ?: return@withContext null
                // return parseGroupJson(groupJson)
                throw NotImplementedError("getGroup not yet implemented - requires mdk-kotlin library")
            } catch (e: Exception) {
                throw IllegalStateException("Failed to get group: ${e.message}", e)
            }
        }

    override suspend fun addMembers(
        groupId: String,
        keyPackages: List<String>,
    ) = withContext(Dispatchers.IO) {
        requireInitialized()
        try {
            // mdk.addMembers(groupId, keyPackages.toJson())
            throw NotImplementedError("addMembers not yet implemented - requires mdk-kotlin library")
        } catch (e: Exception) {
            throw IllegalStateException("Failed to add members: ${e.message}", e)
        }
    }

    override suspend fun removeMembers(
        groupId: String,
        memberKeys: List<String>,
    ) = withContext(Dispatchers.IO) {
        requireInitialized()
        try {
            // mdk.removeMembers(groupId, memberKeys)
            throw NotImplementedError("removeMembers not yet implemented - requires mdk-kotlin library")
        } catch (e: Exception) {
            throw IllegalStateException("Failed to remove members: ${e.message}", e)
        }
    }

    override suspend fun sendMessage(
        groupId: String,
        senderKey: String,
        content: String,
    ): String =
        withContext(Dispatchers.IO) {
            requireInitialized()
            try {
                // return mdk.createMessage(groupId, senderKey, content)
                throw NotImplementedError("sendMessage not yet implemented - requires mdk-kotlin library")
            } catch (e: Exception) {
                throw IllegalStateException("Failed to send message: ${e.message}", e)
            }
        }

    override suspend fun processIncomingMessage(eventJson: String): MarmotMessage =
        withContext(Dispatchers.IO) {
            requireInitialized()
            try {
                // val messageJson = mdk.processMessage(eventJson)
                // return parseMessageJson(messageJson)
                throw NotImplementedError("processIncomingMessage not yet implemented - requires mdk-kotlin library")
            } catch (e: Exception) {
                throw IllegalStateException("Failed to process message: ${e.message}", e)
            }
        }

    override suspend fun getMessages(groupId: String): List<MarmotMessage> =
        withContext(Dispatchers.IO) {
            requireInitialized()
            try {
                // val messagesJson = mdk.getMessages(groupId)
                // return parseMessagesJson(messagesJson)
                throw NotImplementedError("getMessages not yet implemented - requires mdk-kotlin library")
            } catch (e: Exception) {
                throw IllegalStateException("Failed to get messages: ${e.message}", e)
            }
        }

    override suspend fun getPendingInvites(): List<MarmotInvite> =
        withContext(Dispatchers.IO) {
            requireInitialized()
            try {
                // val welcomesJson = mdk.getPendingWelcomes()
                // return parseInvitesJson(welcomesJson)
                throw NotImplementedError("getPendingInvites not yet implemented - requires mdk-kotlin library")
            } catch (e: Exception) {
                throw IllegalStateException("Failed to get pending invites: ${e.message}", e)
            }
        }

    override suspend fun acceptInvite(welcomeJson: String): MarmotGroup =
        withContext(Dispatchers.IO) {
            requireInitialized()
            try {
                // val groupJson = mdk.acceptWelcome(welcomeJson)
                // return parseGroupJson(groupJson)
                throw NotImplementedError("acceptInvite not yet implemented - requires mdk-kotlin library")
            } catch (e: Exception) {
                throw IllegalStateException("Failed to accept invite: ${e.message}", e)
            }
        }

    override suspend fun declineInvite(welcomeJson: String) =
        withContext(Dispatchers.IO) {
            requireInitialized()
            try {
                // mdk.declineWelcome(welcomeJson)
                throw NotImplementedError("declineInvite not yet implemented - requires mdk-kotlin library")
            } catch (e: Exception) {
                throw IllegalStateException("Failed to decline invite: ${e.message}", e)
            }
        }

    private fun requireInitialized() {
        if (mdk == null) {
            throw IllegalStateException("MarmotManager not initialized. Call initialize() first.")
        }
    }

    // TODO: Implement JSON parsing helpers when mdk-kotlin is available
    // private fun parseGroupJson(json: String): MarmotGroup { ... }
    // private fun parseGroupsJson(json: String): List<MarmotGroup> { ... }
    // private fun parseMessageJson(json: String): MarmotMessage { ... }
    // private fun parseMessagesJson(json: String): List<MarmotMessage> { ... }
    // private fun parseInvitesJson(json: String): List<MarmotInvite> { ... }
    // private fun List<String>.toJson(): String { ... }
}
