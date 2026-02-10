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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImage
import com.vitorpamplona.amethyst.commons.preview.UrlInfoItem
import com.vitorpamplona.amethyst.desktop.preview.DesktopUrlCachedPreviewer
import com.vitorpamplona.amethyst.desktop.preview.DesktopUrlPreviewState

@Composable
fun InlineImage(
    url: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    AsyncImage(
        model = url,
        contentDescription = "Embedded image",
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = true },
        contentScale = ContentScale.FillWidth,
    )

    if (expanded) {
        ExpandedImageOverlay(url = url, onDismiss = { expanded = false })
    }
}

private val GALLERY_SPACING = 4.dp

@Composable
private fun GalleryImage(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    onExpand: () -> Unit = {},
) {
    AsyncImage(
        model = url,
        contentDescription = "Embedded image",
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onExpand() },
        contentScale = contentScale,
    )
}

@Composable
fun DesktopImageGallery(
    imageUrls: List<String>,
    modifier: Modifier = Modifier,
) {
    if (imageUrls.isEmpty()) return

    var expandedIndex by remember { mutableStateOf(-1) }

    Column(modifier = modifier.padding(vertical = 4.dp)) {
        when (imageUrls.size) {
            1 -> SingleImageLayout(imageUrls[0], onExpand = { expandedIndex = 0 })
            2 -> TwoImageLayout(imageUrls, onExpand = { expandedIndex = it })
            3 -> ThreeImageLayout(imageUrls, onExpand = { expandedIndex = it })
            else -> FourPlusImageLayout(imageUrls.take(4), onExpand = { expandedIndex = it })
        }
    }

    if (expandedIndex >= 0) {
        ExpandedImageOverlay(
            imageUrls = imageUrls,
            initialIndex = expandedIndex,
            onDismiss = { expandedIndex = -1 },
        )
    }
}

@Composable
private fun SingleImageLayout(
    url: String,
    onExpand: () -> Unit,
) {
    GalleryImage(
        url = url,
        modifier =
            Modifier
                .fillMaxWidth(0.85f)
                .heightIn(max = 400.dp),
        contentScale = ContentScale.Fit,
        onExpand = onExpand,
    )
}

@Composable
private fun TwoImageLayout(
    urls: List<String>,
    onExpand: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GALLERY_SPACING),
    ) {
        urls.take(2).forEachIndexed { index, url ->
            GalleryImage(
                url = url,
                modifier =
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                onExpand = { onExpand(index) },
            )
        }
    }
}

@Composable
private fun ThreeImageLayout(
    urls: List<String>,
    onExpand: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GALLERY_SPACING),
    ) {
        urls.take(3).forEachIndexed { index, url ->
            GalleryImage(
                url = url,
                modifier =
                    Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                onExpand = { onExpand(index) },
            )
        }
    }
}

@Composable
private fun FourPlusImageLayout(
    urls: List<String>,
    onExpand: (Int) -> Unit,
) {
    val gridUrls = urls.take(4)
    var globalIndex = 0
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GALLERY_SPACING),
    ) {
        gridUrls.chunked(2).forEach { rowUrls ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GALLERY_SPACING),
            ) {
                rowUrls.forEach { url ->
                    val idx = globalIndex++
                    GalleryImage(
                        url = url,
                        modifier =
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                        onExpand = { onExpand(idx) },
                    )
                }
                repeat(2 - rowUrls.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ExpandedImageOverlay(
    url: String,
    onDismiss: () -> Unit,
) {
    ExpandedImageOverlay(imageUrls = listOf(url), initialIndex = 0, onDismiss = onDismiss)
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun ExpandedImageOverlay(
    imageUrls: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    var currentIndex by remember(initialIndex) { mutableStateOf(initialIndex) }
    val focusRequester = remember { FocusRequester() }
    // Accumulate trackpad scroll delta to require a meaningful swipe
    var scrollCumulativeDelta by remember { mutableStateOf(0f) }
    val scrollThreshold = 10f

    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .focusRequester(focusRequester)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onDismiss() }
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyUp) {
                            when (keyEvent.key) {
                                Key.DirectionLeft -> {
                                    if (currentIndex > 0) currentIndex--
                                    true
                                }
                                Key.DirectionRight -> {
                                    if (currentIndex < imageUrls.size - 1) currentIndex++
                                    true
                                }
                                Key.Escape -> {
                                    onDismiss()
                                    true
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    }.onPointerEvent(PointerEventType.Scroll) { event ->
                        val scrollDelta =
                            event.changes
                                .firstOrNull()
                                ?.scrollDelta
                                ?.x ?: 0f
                        scrollCumulativeDelta += scrollDelta
                        if (scrollCumulativeDelta > scrollThreshold) {
                            if (currentIndex < imageUrls.size - 1) currentIndex++
                            scrollCumulativeDelta = 0f
                        } else if (scrollCumulativeDelta < -scrollThreshold) {
                            if (currentIndex > 0) currentIndex--
                            scrollCumulativeDelta = 0f
                        }
                    },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = imageUrls[currentIndex],
                contentDescription = "Expanded image ${currentIndex + 1} of ${imageUrls.size}",
                modifier =
                    Modifier
                        .fillMaxSize(0.9f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onDismiss() },
                contentScale = ContentScale.Fit,
            )

            // Left arrow
            if (currentIndex > 0) {
                IconButton(
                    onClick = { currentIndex-- },
                    modifier =
                        Modifier
                            .align(Alignment.CenterStart)
                            .padding(16.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f)),
                    colors =
                        IconButtonDefaults.iconButtonColors(
                            contentColor = Color.White,
                        ),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous image",
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            // Right arrow
            if (currentIndex < imageUrls.size - 1) {
                IconButton(
                    onClick = { currentIndex++ },
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .padding(16.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f)),
                    colors =
                        IconButtonDefaults.iconButtonColors(
                            contentColor = Color.White,
                        ),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next image",
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            // Counter
            if (imageUrls.size > 1) {
                Text(
                    "${currentIndex + 1} / ${imageUrls.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 24.dp),
                )
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
fun LinkPreviewCard(
    url: String,
    previewInfo: UrlInfoItem,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { runCatching { uriHandler.openUri(url) } },
        shape = RoundedCornerShape(8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp),
        ) {
            if (previewInfo.imageUrlFullPath.isNotEmpty()) {
                AsyncImage(
                    model = previewInfo.imageUrlFullPath,
                    contentDescription = previewInfo.title,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .width(120.dp)
                            .fillMaxHeight(),
                )
            }

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = previewInfo.verifiedUrl?.host ?: url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (previewInfo.title.isNotEmpty()) {
                    Text(
                        text = previewInfo.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (previewInfo.description.isNotEmpty()) {
                    Text(
                        text = previewInfo.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun LoadableUrlPreview(
    url: String,
    modifier: Modifier = Modifier,
) {
    @Suppress("ProduceStateDoesNotAssignValue")
    val state by
        produceState<DesktopUrlPreviewState>(
            initialValue = DesktopUrlCachedPreviewer.getCached(url) ?: DesktopUrlPreviewState.Loading,
            key1 = url,
        ) {
            if (value == DesktopUrlPreviewState.Loading) {
                DesktopUrlCachedPreviewer.previewInfo(url) { value = it }
            }
        }

    when (val s = state) {
        is DesktopUrlPreviewState.Loaded -> {
            if (s.previewInfo.mimeType.startsWith("image")) {
                InlineImage(url = url, modifier = modifier)
            } else if (s.previewInfo.mimeType.startsWith("video")) {
                VideoPreviewCard(url = url, modifier = modifier)
            } else {
                LinkPreviewCard(url = url, previewInfo = s.previewInfo, modifier = modifier)
            }
        }
        is DesktopUrlPreviewState.Loading -> {
            val uriHandler = LocalUriHandler.current
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = modifier.clickable { runCatching { uriHandler.openUri(url) } },
            )
        }
        else -> {
            val uriHandler = LocalUriHandler.current
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = modifier.clickable { runCatching { uriHandler.openUri(url) } },
            )
        }
    }
}
