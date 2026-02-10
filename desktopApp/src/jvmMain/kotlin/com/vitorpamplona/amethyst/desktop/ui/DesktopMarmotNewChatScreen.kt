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

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.atna.marmot.MarmotEventRouter
import com.atna.marmot.MarmotNewChatState
import com.atna.marmot.normalizeKeyPackageEventJson
import com.atna.marmot.queryRelayForEvent
import com.vitorpamplona.amethyst.commons.model.User
import com.vitorpamplona.amethyst.commons.ui.components.UserAvatar
import com.vitorpamplona.amethyst.desktop.account.AccountState
import com.vitorpamplona.amethyst.desktop.cache.DesktopLocalCache
import com.vitorpamplona.amethyst.desktop.network.DesktopRelayConnectionManager
import com.vitorpamplona.quartz.marmotMls.MarmotKeyPackageEvent
import com.vitorpamplona.quartz.marmotMls.MarmotKeyPackageRelayListEvent
import com.vitorpamplona.quartz.nip01Core.core.Event
import com.vitorpamplona.quartz.nip01Core.core.hexToByteArrayOrNull
import com.vitorpamplona.quartz.nip01Core.relay.filters.Filter
import com.vitorpamplona.quartz.nip01Core.relay.normalizer.RelayUrlNormalizer
import com.vitorpamplona.quartz.nip19Bech32.toNpub
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DesktopMarmotNewChatScreen(
    prefillPubkey: String?,
    relayManager: DesktopRelayConnectionManager,
    localCache: DesktopLocalCache,
    account: AccountState.LoggedIn?,
    marmotRouter: MarmotEventRouter,
    onBack: () -> Unit,
    onNavigateToConversation: (groupId: String, groupName: String) -> Unit = { _, _ -> },
) {
    var searchText by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<User>>(emptyList()) }
    var chatState by remember { mutableStateOf<MarmotNewChatState>(MarmotNewChatState.Idle) }
    val scope = rememberCoroutineScope()

    // Auto-prefill and search when navigating from profile
    LaunchedEffect(prefillPubkey) {
        if (!prefillPubkey.isNullOrEmpty()) {
            val user = localCache.getUserIfExists(prefillPubkey)
            if (user != null) {
                searchText = user.toBestDisplayName()
                searchResults = listOf(user)
            } else {
                val npub = prefillPubkey.hexToByteArrayOrNull()?.toNpub() ?: prefillPubkey
                searchText = npub.take(20)
                searchResults = localCache.findUsersStartingWith(prefillPubkey)
            }
        }
    }

    // Auto-navigate to conversation when group is created
    LaunchedEffect(chatState) {
        val state = chatState
        if (state is MarmotNewChatState.GroupCreated) {
            onNavigateToConversation(state.groupId, state.groupName)
        }
    }

    // Debounced search
    LaunchedEffect(searchText) {
        if (searchText.isBlank()) {
            searchResults = emptyList()
            return@LaunchedEffect
        }
        delay(150)
        searchResults = localCache.findUsersStartingWith(searchText)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "New Marmot Chat",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }

        // Search bar
        OutlinedTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                chatState = MarmotNewChatState.Idle
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            placeholder = { Text("Search by name or npub\u2026") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
        )

        Spacer(Modifier.height(12.dp))

        // Progress indicator
        when (val state = chatState) {
            is MarmotNewChatState.Initializing -> {
                LookupProgressRow("Initializing Marmot\u2026")
            }
            is MarmotNewChatState.SearchingRelayList -> {
                LookupProgressRow("Looking up Marmot relay list\u2026")
            }
            is MarmotNewChatState.LookingUpKeyPackage -> {
                LookupProgressRow("Looking up key package\u2026")
            }
            is MarmotNewChatState.CreatingGroup -> {
                LookupProgressRow("Creating encrypted group\u2026")
            }
            is MarmotNewChatState.GroupCreated -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Group created! Opening conversation\u2026",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            is MarmotNewChatState.Error -> {
                SelectionContainer {
                    Text(
                        "Error: ${state.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            else -> {}
        }

        // No key package dialog
        if (chatState is MarmotNewChatState.NoKeyPackage) {
            val pubkey = (chatState as MarmotNewChatState.NoKeyPackage).pubkey
            val displayName = localCache.getUserIfExists(pubkey)?.toBestDisplayName() ?: pubkey.take(12)

            AlertDialog(
                onDismissRequest = { chatState = MarmotNewChatState.Idle },
                title = { Text("No Key Package Found") },
                text = {
                    Text(
                        "$displayName hasn't published Marmot key packages yet. " +
                            "NIP-17 DM invitations are not yet available on desktop.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = { chatState = MarmotNewChatState.Idle }) {
                        Text("OK")
                    }
                },
            )
        }

        // Search results
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Marmot watermark
            Column(
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .alpha(0.04f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                )
                Text(
                    text = "Marmot",
                    style = MaterialTheme.typography.displaySmall,
                )
            }

            if (searchResults.isNotEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(searchResults, key = { it.pubkeyHex }) { user ->
                        UserSearchResultCard(
                            user = user,
                            isLoading = chatState.isLoading,
                            onStartChat = {
                                scope.launch(Dispatchers.IO) {
                                    startMarmotChatDesktop(
                                        targetPubkey = user.pubkeyHex,
                                        relayManager = relayManager,
                                        marmotRouter = marmotRouter,
                                        account = account,
                                        onStateChange = { chatState = it },
                                    )
                                }
                            },
                        )
                    }
                }
            } else if (searchText.isNotBlank()) {
                Text(
                    "No users found",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun UserSearchResultCard(
    user: User,
    isLoading: Boolean,
    onStartChat: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserAvatar(
                userHex = user.pubkeyHex,
                pictureUrl = user.profilePicture(),
                size = 40.dp,
                contentDescription = "Profile picture",
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.toBestDisplayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = user.pubkeyNpub().take(24) + "\u2026",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(8.dp))
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                OutlinedButton(onClick = onStartChat) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Start Marmot Chat", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun LookupProgressRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private suspend fun startMarmotChatDesktop(
    targetPubkey: String,
    relayManager: DesktopRelayConnectionManager,
    marmotRouter: MarmotEventRouter,
    account: AccountState.LoggedIn?,
    onStateChange: (MarmotNewChatState) -> Unit,
) {
    try {
        // Check if Marmot init already failed
        marmotRouter.initError?.let { error ->
            onStateChange(MarmotNewChatState.Error(error))
            return
        }

        // Wait for Marmot to finish initializing (up to 10s)
        if (!marmotRouter.isInitialized) {
            onStateChange(MarmotNewChatState.Initializing)
            repeat(100) {
                if (marmotRouter.isInitialized) return@repeat
                // Check for init failure during polling
                marmotRouter.initError?.let { error ->
                    onStateChange(MarmotNewChatState.Error(error))
                    return
                }
                delay(100)
            }
            if (!marmotRouter.isInitialized) {
                onStateChange(
                    MarmotNewChatState.Error(
                        marmotRouter.initError
                            ?: "Marmot failed to initialize. Check console for native library errors (libmdk_uniffi).",
                    ),
                )
                return
            }
        }

        onStateChange(MarmotNewChatState.SearchingRelayList(targetPubkey))

        val client = relayManager.client

        // Step 1: Look for cached relay list
        var marmotRelays = marmotRouter.getMarmotRelaysForUser(targetPubkey)

        // Step 2: If not cached, query relays for kind 10051
        if (marmotRelays.isEmpty()) {
            val connectedRelays = relayManager.connectedRelays.value.toList()
            val relayListEvent =
                queryRelayForEvent(
                    client = client,
                    filter =
                        Filter(
                            authors = listOf(targetPubkey),
                            kinds = listOf(MarmotKeyPackageRelayListEvent.KIND),
                            limit = 1,
                        ),
                    relayUrls = connectedRelays,
                )

            if (relayListEvent is MarmotKeyPackageRelayListEvent) {
                marmotRelays = relayListEvent.relays().map { it.url }
            }
        }

        if (marmotRelays.isEmpty()) {
            onStateChange(MarmotNewChatState.NoKeyPackage(targetPubkey))
            return
        }

        // Step 3: Query marmot relays for kind 443 key package
        onStateChange(MarmotNewChatState.LookingUpKeyPackage(targetPubkey, marmotRelays))

        val normalizedRelays = marmotRelays.mapNotNull { RelayUrlNormalizer.normalizeOrNull(it) }
        val keyPackageEvent =
            queryRelayForEvent(
                client = client,
                filter =
                    Filter(
                        authors = listOf(targetPubkey),
                        kinds = listOf(MarmotKeyPackageEvent.KIND),
                        limit = 1,
                    ),
                relayUrls = normalizedRelays,
            )

        if (keyPackageEvent == null) {
            onStateChange(MarmotNewChatState.NoKeyPackage(targetPubkey))
            return
        }

        // Step 4: Create group
        if (account == null) {
            onStateChange(MarmotNewChatState.Error("Not logged in"))
            return
        }

        onStateChange(MarmotNewChatState.CreatingGroup(targetPubkey))

        val keyPackageJson = normalizeKeyPackageEventJson(keyPackageEvent)

        val result =
            marmotRouter.createGroup(
                creatorKey = account.pubKeyHex,
                memberKeyPackages = listOf(keyPackageJson),
                name = "Chat with ${targetPubkey.take(8)}",
                description = "",
                relays = marmotRelays,
                admins = listOf(account.pubKeyHex),
            )

        // Broadcast welcome events
        val normalizedMarmotRelays = marmotRelays.mapNotNull { RelayUrlNormalizer.normalizeOrNull(it) }.toSet()
        result.welcomeEventJsons.forEach { json ->
            val event = Event.fromJson(json)
            client.send(event, normalizedMarmotRelays)
        }

        marmotRouter.refreshGroups()
        onStateChange(MarmotNewChatState.GroupCreated(result.group.id, result.group.name))
    } catch (e: Exception) {
        val msg = e.message ?: "Unknown error"
        val userMsg =
            if (msg.contains("lifetime", ignoreCase = true) || msg.contains("leaf node is not valid", ignoreCase = true)) {
                "This user's key package has expired. They need to publish a fresh one before you can start a Marmot chat."
            } else {
                msg
            }
        onStateChange(MarmotNewChatState.Error(userMsg))
    }
}
