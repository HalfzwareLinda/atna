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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.composemediaplayer.VideoPlayerState
import io.github.kdroidfilter.composemediaplayer.VideoPlayerSurface
import io.github.kdroidfilter.composemediaplayer.rememberVideoPlayerState
import java.io.File
import java.net.URI

/**
 * Safe accessors for [VideoPlayerState] properties that may throw
 * [IllegalStateException] if the underlying GStreamer native object
 * has been disposed on another thread (race between AWT-EventQueue
 * recomposition and DisposableEffect cleanup).
 */
private inline val VideoPlayerState.safeAspectRatio: Float
    get() = runCatching { aspectRatio }.getOrDefault(0f)

private inline val VideoPlayerState.safeIsPlaying: Boolean
    get() = runCatching { isPlaying }.getOrDefault(false)

private inline val VideoPlayerState.safeSliderPos: Float
    get() = runCatching { sliderPos }.getOrDefault(0f)

private inline val VideoPlayerState.safePositionText: String
    get() = runCatching { positionText }.getOrDefault("0:00")

private inline val VideoPlayerState.safeDurationText: String
    get() = runCatching { durationText }.getOrDefault("0:00")

private inline val VideoPlayerState.safeError: Any?
    get() = runCatching { error }.getOrNull()

private inline val VideoPlayerState.safeVolume: Float
    get() = runCatching { volume }.getOrDefault(0f)

/**
 * Launches video URLs in an external player (mpv, vlc) or falls back to browser.
 * Used as emergency fallback when GStreamer inline playback fails.
 */
object DesktopVideoLauncher {
    private val playerCommand: List<String>? by lazy { findPlayer() }

    private fun findPlayer(): List<String>? {
        val candidates =
            listOf(
                listOf("mpv", "--force-window=yes"),
                listOf("vlc"),
                listOf("xdg-open"),
            )
        for (candidate in candidates) {
            val binary = candidate[0]
            val found =
                System
                    .getenv("PATH")
                    ?.split(File.pathSeparator)
                    ?.any { File(it, binary).canExecute() } ?: false
            if (found) return candidate
        }
        return null
    }

    fun play(url: String): Boolean {
        val cmd = playerCommand ?: return false
        return runCatching {
            ProcessBuilder(cmd + url)
                .redirectErrorStream(true)
                .start()
            true
        }.getOrDefault(false)
    }
}

/**
 * Inline video player using ComposeMediaPlayer (GStreamer-based).
 * Videos play directly within the note tile in the feed.
 * Falls back to external player card if GStreamer playback fails.
 */
@Composable
fun VideoPreviewCard(
    url: String,
    modifier: Modifier = Modifier,
) {
    val playerState = rememberVideoPlayerState()
    var hasStarted by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }
    var isDisposed by remember { mutableStateOf(false) }
    // Track whether the surface should be rendered. Set to false before dispose
    // to stop GStreamer from painting frames while native objects are torn down.
    var showSurface by remember { mutableStateOf(true) }

    // Keep volume synced with mute state (guard against disposed native object)
    if (!isDisposed && isMuted && playerState.safeVolume != 0f) {
        runCatching { playerState.volume = 0f }
    }

    DisposableEffect(Unit) {
        onDispose {
            // 1. Hide the surface first — prevents VideoPlayerSurface from making
            //    further GStreamer native calls on the AWT-EventQueue paint thread.
            showSurface = false
            // 2. Pause playback to stop GStreamer's pipeline before disposing.
            runCatching { playerState.pause() }
            // 3. Mark disposed — the library's rememberVideoPlayerState handles the
            //    actual native dispose via its own DisposableEffect. Calling dispose()
            //    here causes a double-dispose crash (IllegalStateException: Native
            //    object has been disposed) when the library's cleanup runs second.
            isDisposed = true
        }
    }

    // If disposed, show nothing (composable is leaving the tree)
    if (isDisposed) return

    // If there's an error, fall back to external player
    if (playerState.safeError != null) {
        ExternalVideoCard(url = url, modifier = modifier)
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .let { m ->
                        val ratio = playerState.safeAspectRatio
                        if (ratio > 0f && hasStarted) {
                            m.height((400 / ratio).coerceAtMost(400f).dp)
                        } else {
                            m.height(200.dp)
                        }
                    }.background(Color(0xFF1A1A2E))
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (hasStarted && showSurface) {
                VideoPlayerSurface(
                    playerState = playerState,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }

            // Play button overlay (shown when not playing)
            if (!playerState.safeIsPlaying) {
                Box(
                    modifier =
                        Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.8f))
                            .clickable {
                                if (isDisposed) return@clickable
                                runCatching {
                                    if (!hasStarted) {
                                        playerState.openUri(url)
                                        playerState.volume = 0f
                                        hasStarted = true
                                    } else {
                                        playerState.play()
                                    }
                                }
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play video",
                        tint = Color(0xFF1A1A2E),
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }

        // Controls (shown after playback starts)
        if (hasStarted) {
            LinearProgressIndicator(
                progress = { playerState.safeSliderPos },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                // Play/Pause
                IconButton(
                    onClick = {
                        if (isDisposed) return@IconButton
                        runCatching {
                            if (playerState.isPlaying) {
                                playerState.pause()
                            } else {
                                playerState.play()
                            }
                        }
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = if (playerState.safeIsPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playerState.safeIsPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(Modifier.width(4.dp))

                // Mute/unmute
                IconButton(
                    onClick = {
                        if (isDisposed) return@IconButton
                        isMuted = !isMuted
                        runCatching { playerState.volume = if (isMuted) 0f else 1f }
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Time position
                Text(
                    text = playerState.safePositionText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = " / ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = playerState.safeDurationText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.weight(1f))

                Text(
                    text = runCatching { URI(url).host }.getOrDefault(""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Fallback card that opens video in an external player when GStreamer is not available.
 */
@Composable
private fun ExternalVideoCard(
    url: String,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable {
                    if (!DesktopVideoLauncher.play(url)) {
                        runCatching { uriHandler.openUri(url) }
                    }
                },
        shape = RoundedCornerShape(8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFF1A1A2E)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play video",
                    tint = Color(0xFF1A1A2E),
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Video",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = runCatching { URI(url).host }.getOrDefault(url),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
