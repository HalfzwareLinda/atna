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
package com.vitorpamplona.amethyst.desktop.ui.note

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.vitorpamplona.amethyst.commons.model.ImmutableListOfLists
import com.vitorpamplona.amethyst.commons.model.User
import com.vitorpamplona.amethyst.commons.richtext.BechSegment
import com.vitorpamplona.amethyst.commons.richtext.HashTagSegment
import com.vitorpamplona.amethyst.commons.richtext.ImageGalleryParagraph
import com.vitorpamplona.amethyst.commons.richtext.ImageSegment
import com.vitorpamplona.amethyst.commons.richtext.LinkSegment
import com.vitorpamplona.amethyst.commons.richtext.RichTextParser
import com.vitorpamplona.amethyst.commons.richtext.RichTextViewerState
import com.vitorpamplona.amethyst.commons.richtext.VideoSegment
import com.vitorpamplona.amethyst.commons.ui.components.UserAvatar
import com.vitorpamplona.amethyst.commons.util.toTimeAgo
import com.vitorpamplona.amethyst.desktop.cache.DesktopLocalCache
import com.vitorpamplona.amethyst.desktop.network.RelayConnectionManager
import com.vitorpamplona.quartz.nip19Bech32.Nip19Parser
import com.vitorpamplona.quartz.nip19Bech32.entities.NEvent
import com.vitorpamplona.quartz.nip19Bech32.entities.NNote
import com.vitorpamplona.quartz.nip19Bech32.entities.NProfile
import com.vitorpamplona.quartz.nip19Bech32.entities.NPub

/**
 * Data class for displaying a note card.
 */
data class NoteDisplayData(
    val id: String,
    val pubKeyHex: String,
    val pubKeyDisplay: String,
    val profilePictureUrl: String? = null,
    val nip05: String? = null,
    val content: String,
    val createdAt: Long,
    val user: User? = null,
    val tags: Array<Array<String>>? = null,
)

/**
 * Reusable note card composable that displays a Nostr note.
 * Can be used by both Desktop and Android apps.
 */
@Composable
fun NoteCard(
    note: NoteDisplayData,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onAuthorClick: ((String) -> Unit)? = null,
    localCache: DesktopLocalCache? = null,
    relayManager: RelayConnectionManager? = null,
    onMentionClick: ((String) -> Unit)? = null,
    onNoteClick: ((String) -> Unit)? = null,
    onHashtagClick: ((String) -> Unit)? = null,
) {
    val richTextParser = remember { RichTextParser() }

    val richTextState =
        remember(note.content, note.tags) {
            if (note.tags != null) {
                richTextParser.parseText(
                    note.content,
                    ImmutableListOfLists(note.tags),
                    null,
                )
            } else {
                null
            }
        }

    val urls =
        remember(note.content, richTextState) {
            richTextState?.urlSet ?: richTextParser.parseValidUrls(note.content)
        }

    // Observe user metadata reactively — updates when profile (kind 0) loads
    val userInfo by note.user
        ?.metadataOrNull()
        ?.flow
        ?.collectAsState()
        ?: remember { mutableStateOf(null) }

    val displayName = userInfo?.info?.bestName() ?: note.pubKeyDisplay
    val pictureUrl = userInfo?.info?.picture ?: note.profilePictureUrl
    val nip05Value = userInfo?.info?.nip05 ?: note.nip05

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .let { mod ->
                    if (onClick != null) {
                        mod.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClick,
                        )
                    } else {
                        mod
                    }
                }.padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Author with avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    if (onAuthorClick != null) {
                        Modifier.clickable { onAuthorClick(note.pubKeyHex) }
                    } else {
                        Modifier
                    },
            ) {
                UserAvatar(
                    userHex = note.pubKeyHex,
                    pictureUrl = pictureUrl,
                    size = 32.dp,
                    contentDescription = "Profile picture of $displayName",
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // NIP-05 identifier + timestamp in lighter color
            val nip05Text =
                nip05Value?.let { nip05 ->
                    val parts = nip05.split("@")
                    if (parts.size == 2 && parts[0] != "_") {
                        " @${parts[0]}@${parts[1]}"
                    } else if (parts.size == 2) {
                        " @${parts[1]}"
                    } else {
                        " @$nip05"
                    }
                } ?: ""
            val timeText = note.createdAt.toTimeAgo(withDot = true)

            Text(
                text = "$nip05Text $timeText",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(8.dp))

        if (richTextState != null) {
            RichMediaContent(
                state = richTextState,
                modifier = Modifier.fillMaxWidth(),
                localCache = localCache,
                relayManager = relayManager,
                onMentionClick = onMentionClick,
                onNoteClick = onNoteClick,
                onHashtagClick = onHashtagClick,
            )
        } else {
            RichTextContent(
                content = note.content,
                urls = urls,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Renders rich media content using parsed segments from RichTextParser.
 * Images are rendered inline, link previews are shown as cards below text.
 * NIP-27 mentions (nostr:npub..., nostr:note...) are rendered as clickable usernames/note refs.
 */
@Composable
fun RichMediaContent(
    state: RichTextViewerState,
    modifier: Modifier = Modifier,
    localCache: DesktopLocalCache? = null,
    relayManager: RelayConnectionManager? = null,
    onMentionClick: ((String) -> Unit)? = null,
    onNoteClick: ((String) -> Unit)? = null,
    onHashtagClick: ((String) -> Unit)? = null,
    quotesLeft: Int = 1,
) {
    val uriHandler = LocalUriHandler.current
    val linkUrls = mutableListOf<String>()
    val videoUrls = mutableListOf<String>()
    val embeddedNoteIds = mutableListOf<EmbeddedNoteRef>()
    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface
    val textStyle = MaterialTheme.typography.bodyMedium

    Column(modifier = modifier) {
        state.paragraphs.forEach { paragraph ->
            when (paragraph) {
                is ImageGalleryParagraph -> {
                    val galleryUrls =
                        paragraph.words
                            .filterIsInstance<ImageSegment>()
                            .map { it.segmentText }

                    if (galleryUrls.isNotEmpty()) {
                        DesktopImageGallery(
                            imageUrls = galleryUrls,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                else -> {
                    val imageUrls = mutableListOf<String>()
                    val hasClickableSegments =
                        paragraph.words.any { it is BechSegment || it is HashTagSegment }

                    if (hasClickableSegments) {
                        // Build AnnotatedString with clickable mentions and hashtags
                        val annotatedString =
                            buildAnnotatedString {
                                var needsSpace = false
                                paragraph.words.forEach { word ->
                                    when (word) {
                                        is ImageSegment -> imageUrls.add(word.segmentText)
                                        is VideoSegment -> videoUrls.add(word.segmentText)
                                        is LinkSegment -> linkUrls.add(word.segmentText)
                                        is HashTagSegment -> {
                                            if (needsSpace) append(" ")
                                            pushStringAnnotation("hashtag", word.hashtag)
                                            withStyle(SpanStyle(color = primaryColor)) {
                                                append("#${word.hashtag}")
                                            }
                                            pop()
                                            word.extras?.let {
                                                withStyle(SpanStyle(color = textColor)) {
                                                    append(it)
                                                }
                                            }
                                            needsSpace = true
                                        }
                                        is BechSegment -> {
                                            if (needsSpace) append(" ")
                                            val parsed = Nip19Parser.uriToRoute(word.segmentText)
                                            when (val entity = parsed?.entity) {
                                                is NPub -> {
                                                    val displayName =
                                                        localCache
                                                            ?.getOrCreateUser(entity.hex)
                                                            ?.toBestDisplayName()
                                                            ?: word.segmentText.take(16) + "..."
                                                    pushStringAnnotation("mention", entity.hex)
                                                    withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Medium)) {
                                                        append("@$displayName")
                                                    }
                                                    pop()
                                                }
                                                is NProfile -> {
                                                    val displayName =
                                                        localCache
                                                            ?.getOrCreateUser(entity.hex)
                                                            ?.toBestDisplayName()
                                                            ?: word.segmentText.take(16) + "..."
                                                    pushStringAnnotation("mention", entity.hex)
                                                    withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Medium)) {
                                                        append("@$displayName")
                                                    }
                                                    pop()
                                                }
                                                is NNote -> {
                                                    if (quotesLeft > 0) {
                                                        embeddedNoteIds.add(EmbeddedNoteRef(entity.hex, null))
                                                    } else {
                                                        val shortId =
                                                            if (word.segmentText.length > 16) {
                                                                word.segmentText.removePrefix("nostr:").take(8) + "..." +
                                                                    word.segmentText.takeLast(8)
                                                            } else {
                                                                word.segmentText
                                                            }
                                                        pushStringAnnotation("note", entity.hex)
                                                        withStyle(SpanStyle(color = primaryColor)) {
                                                            append(shortId)
                                                        }
                                                        pop()
                                                    }
                                                }
                                                is NEvent -> {
                                                    if (quotesLeft > 0) {
                                                        embeddedNoteIds.add(
                                                            EmbeddedNoteRef(
                                                                entity.hex,
                                                                entity.relay.firstOrNull()?.url,
                                                            ),
                                                        )
                                                    } else {
                                                        val shortId =
                                                            if (word.segmentText.length > 16) {
                                                                word.segmentText.removePrefix("nostr:").take(8) + "..." +
                                                                    word.segmentText.takeLast(8)
                                                            } else {
                                                                word.segmentText
                                                            }
                                                        pushStringAnnotation("note", entity.hex)
                                                        withStyle(SpanStyle(color = primaryColor)) {
                                                            append(shortId)
                                                        }
                                                        pop()
                                                    }
                                                }
                                                else -> append(word.segmentText)
                                            }
                                            needsSpace = true
                                        }
                                        else -> {
                                            if (needsSpace) append(" ")
                                            append(word.segmentText)
                                            needsSpace = true
                                        }
                                    }
                                }
                            }

                        if (annotatedString.isNotEmpty()) {
                            @Suppress("DEPRECATION")
                            ClickableText(
                                text = annotatedString,
                                style = textStyle.copy(color = textColor),
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { offset ->
                                    annotatedString
                                        .getStringAnnotations("mention", offset, offset)
                                        .firstOrNull()
                                        ?.let { onMentionClick?.invoke(it.item) }
                                    annotatedString
                                        .getStringAnnotations("note", offset, offset)
                                        .firstOrNull()
                                        ?.let { onNoteClick?.invoke(it.item) }
                                    annotatedString
                                        .getStringAnnotations("hashtag", offset, offset)
                                        .firstOrNull()
                                        ?.let { onHashtagClick?.invoke(it.item) }
                                },
                            )
                        }
                    } else {
                        // No clickable segments — use simple Text (no clickable overhead)
                        val textParts = StringBuilder()

                        paragraph.words.forEach { word ->
                            when (word) {
                                is ImageSegment -> imageUrls.add(word.segmentText)
                                is VideoSegment -> videoUrls.add(word.segmentText)
                                is LinkSegment -> linkUrls.add(word.segmentText)
                                else -> {
                                    if (textParts.isNotEmpty()) textParts.append(" ")
                                    textParts.append(word.segmentText)
                                }
                            }
                        }

                        val text = textParts.toString().trim()
                        if (text.isNotEmpty()) {
                            Text(
                                text = text,
                                style = textStyle,
                                color = textColor,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    // Render inline images from this paragraph using gallery layout
                    if (imageUrls.isNotEmpty()) {
                        DesktopImageGallery(
                            imageUrls = imageUrls,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        // Render embedded note cards for nevent/note1 references
        embeddedNoteIds.forEach { ref ->
            Spacer(Modifier.height(4.dp))
            EmbeddedNoteCard(
                eventIdHex = ref.eventIdHex,
                relayHint = ref.relayHint,
                localCache = localCache,
                relayManager = relayManager,
                onNoteClick = onNoteClick,
                onMentionClick = onMentionClick,
            )
        }

        // Render video preview cards for known video URLs
        videoUrls.forEach { url ->
            Spacer(Modifier.height(4.dp))
            VideoPreviewCard(
                url = url,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Render link preview cards for non-media URLs
        linkUrls.forEach { url ->
            if (!RichTextParser.isImageUrl(url)) {
                Spacer(Modifier.height(4.dp))
                LoadableUrlPreview(
                    url = url,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Reference to an embedded note (nevent/note1) to be rendered as a quoted card.
 */
private data class EmbeddedNoteRef(
    val eventIdHex: String,
    val relayHint: String?,
)

/**
 * Renders text content with highlighted URLs (fallback when tags are not available).
 */
@Composable
fun RichTextContent(
    content: String,
    urls: Set<String>,
    modifier: Modifier = Modifier,
    maxLines: Int = 10,
) {
    if (urls.isEmpty()) {
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
    } else {
        val annotatedText =
            buildAnnotatedString {
                var lastIndex = 0
                val sortedUrls = urls.sortedBy { content.indexOf(it) }

                for (url in sortedUrls) {
                    val startIndex = content.indexOf(url, lastIndex)
                    if (startIndex == -1) continue

                    // Add text before URL
                    if (startIndex > lastIndex) {
                        append(content.substring(lastIndex, startIndex))
                    }

                    // Add URL with styling
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                        ),
                    ) {
                        append(url)
                    }

                    lastIndex = startIndex + url.length
                }

                // Add remaining text
                if (lastIndex < content.length) {
                    append(content.substring(lastIndex))
                }
            }

        Text(
            text = annotatedText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
    }
}
