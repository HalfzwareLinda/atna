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
package com.vitorpamplona.amethyst.desktop.ui

import com.vitorpamplona.amethyst.commons.model.User
import com.vitorpamplona.amethyst.commons.model.cache.ICacheProvider
import com.vitorpamplona.amethyst.commons.util.toShortDisplay
import com.vitorpamplona.amethyst.desktop.ui.note.NoteDisplayData
import com.vitorpamplona.quartz.nip01Core.core.Event
import com.vitorpamplona.quartz.nip01Core.core.hexToByteArrayOrNull
import com.vitorpamplona.quartz.nip19Bech32.toNpub

/**
 * Extension to convert Event to NoteDisplayData for the shared NoteCard.
 * Resolves profile display names from cached metadata when available.
 */
fun Event.toNoteDisplayData(cache: ICacheProvider? = null): NoteDisplayData {
    val user = (cache?.getUserIfExists(pubKey) as? User)
    val displayName =
        user?.toBestDisplayName() ?: run {
            try {
                pubKey.hexToByteArrayOrNull()?.toNpub()?.toShortDisplay(5) ?: pubKey.take(16) + "..."
            } catch (e: Exception) {
                pubKey.take(16) + "..."
            }
        }
    val pictureUrl = user?.profilePicture()
    val nip05 = user?.metadataOrNull()?.nip05()

    return NoteDisplayData(
        id = id,
        pubKeyHex = pubKey,
        pubKeyDisplay = displayName,
        profilePictureUrl = pictureUrl,
        nip05 = nip05,
        content = content,
        createdAt = createdAt,
        user = user,
        tags = tags,
    )
}

/**
 * Finds the event ID this event is replying to.
 * Uses NIP-10 markers (reply/root) or falls back to last e-tag.
 */
fun findReplyToId(event: Event): String? {
    val eTags = event.tags.filter { it.size >= 2 && it[0] == "e" }
    if (eTags.isEmpty()) return null

    // Check for NIP-10 marked tags first
    val replyTag = eTags.find { it.size >= 4 && it[3] == "reply" }
    if (replyTag != null) return replyTag[1]

    val rootTag = eTags.find { it.size >= 4 && it[3] == "root" }
    if (rootTag != null && eTags.size == 1) return rootTag[1]

    // Fall back to positional (last e-tag is the reply-to)
    return eTags.lastOrNull()?.get(1)
}
