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
 * Represents a Marmot encrypted group.
 *
 * @property id The MLS group ID
 * @property nostrGroupId The Nostr-level group identifier
 * @property name Group name
 * @property description Group description
 * @property adminPubkeys Public keys of group admins
 * @property lastMessageAt Timestamp of last message (seconds since epoch), null if no messages
 * @property state Group state (e.g. "active")
 */
data class MarmotGroup(
    val id: String,
    val nostrGroupId: String = "",
    val name: String,
    val description: String,
    val adminPubkeys: List<String> = emptyList(),
    val lastMessageAt: Long? = null,
    val state: String = "active",
) {
    @Deprecated("Use adminPubkeys.size or getMembers() instead", ReplaceWith("0"))
    val memberCount: Int get() = 0
}

/**
 * Represents a message in a Marmot group.
 *
 * @property id Message ID
 * @property groupId The MLS group ID this message belongs to
 * @property nostrGroupId The Nostr-level group identifier
 * @property eventId The Nostr event ID for this message
 * @property senderKey Public key of the sender
 * @property content Decrypted message content (event JSON)
 * @property timestamp Message timestamp (seconds since epoch)
 * @property kind Nostr event kind
 * @property state Message state
 */
data class MarmotMessage(
    val id: String,
    val groupId: String,
    val nostrGroupId: String = "",
    val eventId: String = "",
    val senderKey: String,
    val content: String,
    val timestamp: Long,
    val kind: Int = 1,
    val state: String = "received",
)

/**
 * Represents a pending invitation to a Marmot group.
 *
 * @property welcomeId The welcome ID (used for accept/decline)
 * @property groupId The MLS group ID
 * @property nostrGroupId The Nostr-level group identifier
 * @property groupName Name of the group being invited to
 * @property groupDescription Description of the group
 * @property inviterKey Public key of the user who sent the invite
 * @property memberCount Number of members in the group
 * @property relays Relay URLs for the group
 * @property state Invite state
 */
data class MarmotInvite(
    val welcomeId: String,
    val groupId: String,
    val nostrGroupId: String = "",
    val groupName: String,
    val groupDescription: String = "",
    val inviterKey: String,
    val memberCount: Int = 0,
    val relays: List<String> = emptyList(),
    val state: String = "pending",
) {
    @Deprecated("Use welcomeId instead", ReplaceWith("welcomeId"))
    val welcomeJson: String get() = welcomeId
}
