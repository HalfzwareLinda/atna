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
 * Marmot protocol manager for encrypted group DMs.
 * Wraps mdk-kotlin to provide MLS-over-Nostr messaging.
 *
 * This is a common interface with platform-specific implementations
 * for Android and JVM that wrap the mdk-kotlin library.
 */
interface MarmotManager {
    val isInitialized: Boolean

    /**
     * Initialize the Marmot manager with a database path.
     *
     * @param dbPath Path to the database directory for storing MLS state
     */
    fun initialize(dbPath: String)

    /**
     * Close the Marmot manager and release resources.
     */
    fun close()

    /**
     * Create a key package for this user to be shared with others.
     * The key package is wrapped in a Nostr kind 443 event.
     *
     * @param publicKey The user's public key (hex)
     * @param relays List of relay URLs
     * @return KeyPackageResult with key package content and tags
     */
    suspend fun createKeyPackage(
        publicKey: String,
        relays: List<String>,
    ): String

    /**
     * Create a new encrypted group.
     *
     * @param creatorKey The creator's public key (hex)
     * @param memberKeyPackages List of key package event JSONs for initial members
     * @param name Group name
     * @param description Group description
     * @param relays List of relay URLs for the group
     * @param admins List of public keys (hex) for group admins
     * @return CreateGroupResult with the group and welcome event JSONs to publish
     */
    suspend fun createGroup(
        creatorKey: String,
        memberKeyPackages: List<String>,
        name: String,
        description: String,
        relays: List<String>,
        admins: List<String>,
    ): MarmotCreateGroupResult

    /**
     * Get all groups this user is a member of.
     */
    suspend fun getGroups(): List<MarmotGroup>

    /**
     * Get a specific group by ID.
     *
     * @param groupId The MLS group ID
     * @return The group, or null if not found
     */
    suspend fun getGroup(groupId: String): MarmotGroup?

    /**
     * Get member public keys for a group.
     *
     * @param groupId The MLS group ID
     * @return List of member public keys (hex)
     */
    suspend fun getMembers(groupId: String): List<String>

    /**
     * Add members to an existing group.
     *
     * @param groupId The MLS group ID
     * @param keyPackages List of key package event JSONs for new members
     * @return List of welcome event JSONs to publish
     */
    suspend fun addMembers(
        groupId: String,
        keyPackages: List<String>,
    ): List<String>

    /**
     * Remove members from a group.
     *
     * @param groupId The MLS group ID
     * @param memberKeys List of public keys (hex) to remove
     */
    suspend fun removeMembers(
        groupId: String,
        memberKeys: List<String>,
    )

    /**
     * Send a message to a group.
     *
     * @param groupId The MLS group ID
     * @param senderKey The sender's public key (hex)
     * @param content Message content (plaintext)
     * @param kind Nostr event kind (default 445 for group messages)
     * @param tags Additional tags for the event
     * @return The encrypted message event JSON to publish
     */
    suspend fun sendMessage(
        groupId: String,
        senderKey: String,
        content: String,
        kind: Int = 445,
        tags: List<List<String>> = emptyList(),
    ): String

    /**
     * Process an incoming encrypted message event.
     *
     * @param eventJson JSON string of the encrypted message event
     * @return Processing result (message, commit, proposal, or unprocessable)
     */
    suspend fun processIncomingMessage(eventJson: String): MarmotProcessResult

    /**
     * Get all messages for a group.
     *
     * @param groupId The MLS group ID
     * @return List of messages
     */
    suspend fun getMessages(groupId: String): List<MarmotMessage>

    /**
     * Get all pending group invitations (welcomes).
     */
    suspend fun getPendingInvites(): List<MarmotInvite>

    /**
     * Process a welcome event and return the invite details.
     *
     * @param eventJson The welcome event JSON
     * @param ourPubkey Our public key (hex)
     * @return The pending invite
     */
    suspend fun processWelcome(
        eventJson: String,
        ourPubkey: String,
    ): MarmotInvite

    /**
     * Accept a group invitation.
     *
     * @param welcomeId The welcome ID
     */
    suspend fun acceptInvite(welcomeId: String)

    /**
     * Decline a group invitation.
     *
     * @param welcomeId The welcome ID
     */
    suspend fun declineInvite(welcomeId: String)
}

/**
 * Result of creating a group.
 * @property group The created group
 * @property welcomeEventJsons Welcome event JSONs to publish (invite events for members)
 */
data class MarmotCreateGroupResult(
    val group: MarmotGroup,
    val welcomeEventJsons: List<String>,
)

/**
 * Result of processing an incoming message.
 */
sealed class MarmotProcessResult {
    data class Message(
        val message: MarmotMessage,
    ) : MarmotProcessResult()

    data class Commit(
        val mlsGroupId: String,
    ) : MarmotProcessResult()

    data class Proposal(
        val evolutionEventJson: String,
        val welcomeEventJsons: List<String>,
        val mlsGroupId: String,
    ) : MarmotProcessResult()

    data class Unprocessable(
        val mlsGroupId: String,
    ) : MarmotProcessResult()
}
