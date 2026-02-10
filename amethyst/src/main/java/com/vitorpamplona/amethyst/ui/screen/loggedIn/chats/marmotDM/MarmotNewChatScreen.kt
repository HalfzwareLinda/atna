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
package com.vitorpamplona.amethyst.ui.screen.loggedIn.chats.marmotDM

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atna.marmot.MarmotNewChatState
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.model.User
import com.vitorpamplona.amethyst.ui.navigation.navs.INav
import com.vitorpamplona.amethyst.ui.navigation.routes.Route
import com.vitorpamplona.amethyst.ui.navigation.topbars.TopBarWithBackButton
import com.vitorpamplona.amethyst.ui.note.AboutDisplay
import com.vitorpamplona.amethyst.ui.note.UserPicture
import com.vitorpamplona.amethyst.ui.note.UsernameDisplay
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import com.vitorpamplona.amethyst.ui.theme.Size55dp

@Composable
fun MarmotNewChatScreen(
    prefillPubkey: String?,
    accountViewModel: AccountViewModel,
    nav: INav,
) {
    val viewModel: MarmotNewChatViewModel =
        viewModel(
            factory = MarmotNewChatViewModel.Factory(accountViewModel.account),
        )

    val chatState by viewModel.chatState.collectAsState()
    val searchResults by viewModel.searchResultsUsers.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle prefilled pubkey from profile page
    LaunchedEffect(prefillPubkey) {
        if (!prefillPubkey.isNullOrBlank()) {
            viewModel.startMarmotChat(prefillPubkey)
        }
    }

    // Navigate on successful group creation
    LaunchedEffect(chatState) {
        when (val state = chatState) {
            is MarmotNewChatState.GroupCreated -> {
                nav.nav(Route.MarmotConversation(state.groupId, state.groupName))
                viewModel.resetState()
            }
            is MarmotNewChatState.InviteSent -> {
                snackbarHostState.showSnackbar("Invitation sent!")
                viewModel.resetState()
            }
            is MarmotNewChatState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    // Auto-focus search on launch (unless prefill is set)
    LaunchedEffect(Unit) {
        if (prefillPubkey.isNullOrBlank()) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    Scaffold(
        topBar = {
            TopBarWithBackButton(
                caption = stringResource(R.string.marmot_new_chat),
                popBack = { nav.popBack() },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            // Marmot watermark in background
            MarmotWatermark(
                modifier =
                    Modifier
                        .size(200.dp)
                        .align(Alignment.Center)
                        .alpha(0.04f),
            )

            Column(modifier = Modifier.fillMaxSize()) {
                // Search bar
                OutlinedTextField(
                    value = viewModel.searchValue,
                    onValueChange = { viewModel.updateSearchValue(it) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .focusRequester(focusRequester),
                    placeholder = { Text(stringResource(R.string.marmot_search_users)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (viewModel.searchValue.isNotBlank()) {
                            IconButton(onClick = { viewModel.clear() }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                )

                // State overlay for lookup progress
                when (val state = chatState) {
                    is MarmotNewChatState.Initializing ->
                        LookupProgressRow(stringResource(R.string.marmot_initializing))
                    is MarmotNewChatState.SearchingRelayList ->
                        LookupProgressRow(stringResource(R.string.marmot_looking_up_relays))
                    is MarmotNewChatState.LookingUpKeyPackage ->
                        LookupProgressRow(stringResource(R.string.marmot_looking_up_keypackage))
                    is MarmotNewChatState.CreatingGroup ->
                        LookupProgressRow(stringResource(R.string.marmot_creating_group))
                    is MarmotNewChatState.NoKeyPackage -> {
                        val targetUser = LocalCache.getUserIfExists(state.pubkey)
                        val displayName = targetUser?.toBestDisplayName() ?: state.pubkey.take(8)
                        MarmotInviteDialog(
                            displayName = displayName,
                            onSendInvite = { viewModel.sendNip17Invitation(state.pubkey) },
                            onDismiss = { viewModel.resetState() },
                        )
                    }
                    else -> {}
                }

                // Search results
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(searchResults, key = { it.pubkeyHex }) { user ->
                        MarmotUserResultCard(
                            user = user,
                            onStartChat = { viewModel.startMarmotChat(it.pubkeyHex) },
                            accountViewModel = accountViewModel,
                            nav = nav,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LookupProgressRow(statusText: String) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MarmotUserResultCard(
    user: User,
    onStartChat: (User) -> Unit,
    accountViewModel: AccountViewModel,
    nav: INav,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserPicture(user, Size55dp, accountViewModel = accountViewModel, nav = nav)

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                UsernameDisplay(user, accountViewModel = accountViewModel)
                Spacer(Modifier.height(2.dp))
                AboutDisplay(user, accountViewModel)
            }

            Spacer(Modifier.width(8.dp))

            TextButton(onClick = { onStartChat(user) }) {
                Text(
                    text = stringResource(R.string.marmot_start_chat),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
