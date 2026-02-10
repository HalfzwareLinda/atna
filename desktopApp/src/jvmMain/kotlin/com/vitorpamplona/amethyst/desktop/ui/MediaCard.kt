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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vitorpamplona.amethyst.desktop.cache.DesktopLocalCache
import com.vitorpamplona.quartz.nip01Core.core.Event
import com.vitorpamplona.quartz.nip68Picture.PictureEvent
import com.vitorpamplona.quartz.nip71Video.VideoEvent
import com.vitorpamplona.quartz.nip94FileMetadata.FileHeaderEvent

/**
 * Card for displaying media events: pictures, videos, and file metadata.
 */
@Composable
fun MediaCard(
    event: Event,
    localCache: DesktopLocalCache,
    onAuthorClick: (String) -> Unit = {},
    onClick: () -> Unit = {},
) {
    when (event) {
        is PictureEvent -> PictureCard(event, localCache, onAuthorClick, onClick)
        is VideoEvent -> VideoCard(event, localCache, onAuthorClick, onClick)
        is FileHeaderEvent -> FileCard(event, localCache, onAuthorClick, onClick)
        else -> {}
    }
}

@Composable
private fun PictureCard(
    event: PictureEvent,
    localCache: DesktopLocalCache,
    onAuthorClick: (String) -> Unit,
    onClick: () -> Unit,
) {
    val author = localCache.getOrCreateUser(event.pubKey)
    val authorName = author.toBestDisplayName()
    val images = event.imetaTags()
    val firstImage = images.firstOrNull()

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        // Image preview
        if (firstImage != null) {
            val ratio = firstImage.dimension?.aspectRatio() ?: (16f / 9f)
            AsyncImage(
                model = firstImage.url,
                contentDescription = firstImage.alt ?: event.title() ?: "Picture",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio.coerceIn(0.5f, 3f))
                        .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.height(8.dp))
        }

        // Title
        event.title()?.let { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
        }

        // Description (content field)
        if (event.content.isNotBlank() && event.content != event.title()) {
            Text(
                text = event.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
        }

        // Image count badge + author
        MediaFooter(
            authorName = authorName,
            onAuthorClick = { onAuthorClick(event.pubKey) },
            badge =
                if (images.size > 1) {
                    "${images.size} images"
                } else {
                    null
                },
        )

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    }
}

@Composable
private fun VideoCard(
    event: VideoEvent,
    localCache: DesktopLocalCache,
    onAuthorClick: (String) -> Unit,
    onClick: () -> Unit,
) {
    val baseEvent = event as Event
    val author = localCache.getOrCreateUser(baseEvent.pubKey)
    val authorName = author.toBestDisplayName()
    val videoMeta = event.imetaTags().firstOrNull()
    val thumbnailUrl = videoMeta?.image?.firstOrNull()

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        // Thumbnail with play icon overlay
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = event.title() ?: "Video",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            // Play icon overlay
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = "Play video",
                modifier =
                    Modifier
                        .size(48.dp)
                        .align(Alignment.Center),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
            // Duration badge
            event.duration()?.let { seconds ->
                val minutes = seconds / 60
                val secs = seconds % 60
                Text(
                    text = "$minutes:${secs.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                RoundedCornerShape(4.dp),
                            ).padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // Title
        event.title()?.let { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
        }

        MediaFooter(
            authorName = authorName,
            onAuthorClick = { onAuthorClick(baseEvent.pubKey) },
        )

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    }
}

@Composable
private fun FileCard(
    event: FileHeaderEvent,
    localCache: DesktopLocalCache,
    onAuthorClick: (String) -> Unit,
    onClick: () -> Unit,
) {
    val author = localCache.getOrCreateUser(event.pubKey)
    val authorName = author.toBestDisplayName()
    val isImage = event.mimeType()?.startsWith("image/") == true
    val isVideo = event.mimeType()?.startsWith("video/") == true
    val previewUrl = event.thumb() ?: event.image() ?: if (isImage) event.url() else null

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        // Preview image or file icon
        if (previewUrl != null) {
            AsyncImage(
                model = previewUrl,
                contentDescription = event.summary() ?: "File",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.height(8.dp))
        } else {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector =
                        if (isVideo) {
                            Icons.Default.PlayCircle
                        } else if (isImage) {
                            Icons.Default.Image
                        } else {
                            Icons.Default.AttachFile
                        },
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column {
                    Text(
                        text = event.mimeType() ?: "File",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    event.size()?.let { size ->
                        Text(
                            text = formatFileSize(size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Summary/description
        event.summary()?.let { summary ->
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
        }

        MediaFooter(
            authorName = authorName,
            onAuthorClick = { onAuthorClick(event.pubKey) },
        )

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    }
}

@Composable
private fun MediaFooter(
    authorName: String,
    onAuthorClick: () -> Unit,
    badge: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = authorName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onAuthorClick),
        )
        badge?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatFileSize(bytes: Int): String =
    when {
        bytes >= 1_048_576 -> "${bytes / 1_048_576} MB"
        bytes >= 1024 -> "${bytes / 1024} KB"
        else -> "$bytes B"
    }
