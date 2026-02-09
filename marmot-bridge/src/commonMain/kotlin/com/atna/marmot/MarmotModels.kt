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
 * @property name Group name
 * @property description Group description
 * @property memberCount Number of members in the group
 * @property lastMessageAt Timestamp of last message (milliseconds since epoch), null if no messages
 */
data class MarmotGroup(
    val id: String,
    val name: String,
    val description: String,
    val memberCount: Int,
    val lastMessageAt: Long?,
)

/**
 * Represents a message in a Marmot group.
 *
 * @property id Message ID
 * @property groupId The MLS group ID this message belongs to
 * @property senderKey Public key of the sender
 * @property content Decrypted message content
 * @property timestamp Message timestamp (milliseconds since epoch)
 */
data class MarmotMessage(
    val id: String,
    val groupId: String,
    val senderKey: String,
    val content: String,
    val timestamp: Long,
)

/**
 * Represents a pending invitation to a Marmot group.
 *
 * @property groupId The MLS group ID
 * @property groupName Name of the group being invited to
 * @property inviterKey Public key of the user who sent the invite
 * @property welcomeJson Raw welcome message JSON from the protocol
 */
data class MarmotInvite(
    val groupId: String,
    val groupName: String,
    val inviterKey: String,
    val welcomeJson: String,
)
