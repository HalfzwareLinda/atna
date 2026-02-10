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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vitorpamplona.amethyst.desktop.account.AccountState
import com.vitorpamplona.amethyst.desktop.network.DesktopRelayConnectionManager
import com.vitorpamplona.quartz.nip01Core.metadata.MetadataEvent
import com.vitorpamplona.quartz.nip39ExtIdentities.GitHubIdentity
import com.vitorpamplona.quartz.nip39ExtIdentities.MastodonIdentity
import com.vitorpamplona.quartz.nip39ExtIdentities.TwitterIdentity
import com.vitorpamplona.quartz.nip39ExtIdentities.identityClaims
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EditProfileDialog(
    onDismiss: () -> Unit,
    account: AccountState.LoggedIn,
    relayManager: DesktopRelayConnectionManager,
    currentMetadataEvent: MetadataEvent?,
) {
    var name by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }
    var picture by remember { mutableStateOf("") }
    var banner by remember { mutableStateOf("") }
    var pronouns by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var nip05 by remember { mutableStateOf("") }
    var lnAddress by remember { mutableStateOf("") }
    var lnURL by remember { mutableStateOf("") }
    var twitter by remember { mutableStateOf("") }
    var github by remember { mutableStateOf("") }
    var mastodon by remember { mutableStateOf("") }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Pre-populate from current metadata
    LaunchedEffect(currentMetadataEvent) {
        currentMetadataEvent?.let { event ->
            val metadata = event.contactMetaData()
            if (metadata != null) {
                name = metadata.name ?: ""
                displayName = metadata.displayName ?: ""
                about = metadata.about ?: ""
                picture = metadata.picture ?: ""
                banner = metadata.banner ?: ""
                pronouns = metadata.pronouns ?: ""
                website = metadata.website ?: ""
                nip05 = metadata.nip05 ?: ""
                lnAddress = metadata.lud16 ?: ""
                lnURL = metadata.lud06 ?: ""
            }

            // Identity claims
            event.identityClaims().forEach { claim ->
                when (claim) {
                    is TwitterIdentity -> twitter = claim.toProofUrl()
                    is GitHubIdentity -> github = claim.toProofUrl()
                    is MastodonIdentity -> mastodon = claim.toProofUrl()
                }
            }
        }
    }

    Dialog(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Card(
            modifier = Modifier.width(600.dp).padding(16.dp),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Edit Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(Modifier.height(16.dp))

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp)
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        label = { Text("Name (username)") },
                        modifier = Modifier.fillMaxWidth(),
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("satoshi") },
                        enabled = !isSaving,
                        singleLine = true,
                    )

                    OutlinedTextField(
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        value = displayName,
                        onValueChange = { displayName = it },
                        placeholder = { Text("Satoshi Nakamoto") },
                        enabled = !isSaving,
                        singleLine = true,
                    )

                    OutlinedTextField(
                        label = { Text("About") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        value = about,
                        onValueChange = { about = it },
                        placeholder = { Text("A short bio...") },
                        enabled = !isSaving,
                        maxLines = 5,
                    )

                    OutlinedTextField(
                        label = { Text("Avatar URL") },
                        modifier = Modifier.fillMaxWidth(),
                        value = picture,
                        onValueChange = { picture = it },
                        placeholder = { Text("https://example.com/avatar.jpg") },
                        enabled = !isSaving,
                        singleLine = true,
                    )

                    OutlinedTextField(
                        label = { Text("Banner URL") },
                        modifier = Modifier.fillMaxWidth(),
                        value = banner,
                        onValueChange = { banner = it },
                        placeholder = { Text("https://example.com/banner.jpg") },
                        enabled = !isSaving,
                        singleLine = true,
                    )

                    OutlinedTextField(
                        label = { Text("Pronouns") },
                        modifier = Modifier.fillMaxWidth(),
                        value = pronouns,
                        onValueChange = { pronouns = it },
                        placeholder = { Text("they/them, ...") },
                        enabled = !isSaving,
                        singleLine = true,
                    )

                    OutlinedTextField(
                        label = { Text("Website") },
                        modifier = Modifier.fillMaxWidth(),
                        value = website,
                        onValueChange = { website = it },
                        placeholder = { Text("https://mywebsite.com") },
                        enabled = !isSaving,
                        singleLine = true,
                    )

                    OutlinedTextField(
                        label = { Text("NIP-05") },
                        modifier = Modifier.fillMaxWidth(),
                        value = nip05,
                        onValueChange = { nip05 = it },
                        placeholder = { Text("_@mywebsite.com") },
                        enabled = !isSaving,
                        singleLine = true,
                    )

                    OutlinedTextField(
                        label = { Text("Lightning Address") },
                        modifier = Modifier.fillMaxWidth(),
                        value = lnAddress,
                        onValueChange = { lnAddress = it },
                        placeholder = { Text("me@mylightningnode.com") },
                        enabled = !isSaving,
                        singleLine = true,
                    )

                    OutlinedTextField(
                        label = { Text("Lightning URL (legacy)") },
                        modifier = Modifier.fillMaxWidth(),
                        value = lnURL,
                        onValueChange = { lnURL = it },
                        placeholder = { Text("LNURL...") },
                        enabled = !isSaving,
                        singleLine = true,
                    )

                    OutlinedTextField(
                        label = { Text("Twitter") },
                        modifier = Modifier.fillMaxWidth(),
                        value = twitter,
                        onValueChange = { twitter = it },
                        placeholder = { Text("https://x.com/user/status/123") },
                        enabled = !isSaving,
                        singleLine = true,
                    )

                    OutlinedTextField(
                        label = { Text("GitHub") },
                        modifier = Modifier.fillMaxWidth(),
                        value = github,
                        onValueChange = { github = it },
                        placeholder = { Text("https://gist.github.com/user/gistid") },
                        enabled = !isSaving,
                        singleLine = true,
                    )

                    OutlinedTextField(
                        label = { Text("Mastodon") },
                        modifier = Modifier.fillMaxWidth(),
                        value = mastodon,
                        onValueChange = { mastodon = it },
                        placeholder = { Text("https://mastodon.social/@user/123") },
                        enabled = !isSaving,
                        singleLine = true,
                    )
                }

                errorMessage?.let { error ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSaving,
                    ) {
                        Text("Cancel")
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                errorMessage = null

                                try {
                                    publishProfileMetadata(
                                        account = account,
                                        relayManager = relayManager,
                                        currentMetadataEvent = currentMetadataEvent,
                                        name = name,
                                        displayName = displayName,
                                        about = about,
                                        picture = picture,
                                        banner = banner,
                                        website = website,
                                        pronouns = pronouns,
                                        nip05 = nip05,
                                        lnAddress = lnAddress,
                                        lnURL = lnURL,
                                        twitter = twitter,
                                        github = github,
                                        mastodon = mastodon,
                                    )
                                    onDismiss()
                                } catch (e: Exception) {
                                    errorMessage = "Failed to save: ${e.message}"
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        enabled = !isSaving,
                    ) {
                        Text(if (isSaving) "Saving..." else "Save")
                    }
                }
            }
        }
    }
}

private suspend fun publishProfileMetadata(
    account: AccountState.LoggedIn,
    relayManager: DesktopRelayConnectionManager,
    currentMetadataEvent: MetadataEvent?,
    name: String,
    displayName: String,
    about: String,
    picture: String,
    banner: String,
    website: String,
    pronouns: String,
    nip05: String,
    lnAddress: String,
    lnURL: String,
    twitter: String,
    github: String,
    mastodon: String,
) {
    withContext(Dispatchers.IO) {
        if (account.isReadOnly) {
            throw IllegalStateException("Cannot edit profile in read-only mode")
        }

        val template =
            if (currentMetadataEvent != null) {
                MetadataEvent.updateFromPast(
                    latest = currentMetadataEvent,
                    name = name,
                    displayName = displayName,
                    picture = picture,
                    banner = banner,
                    website = website,
                    about = about,
                    nip05 = nip05,
                    lnAddress = lnAddress,
                    lnURL = lnURL,
                    pronouns = pronouns,
                    twitter = twitter,
                    mastodon = mastodon,
                    github = github,
                )
            } else {
                MetadataEvent.createNew(
                    name = name,
                    displayName = displayName,
                    picture = picture,
                    banner = banner,
                    website = website,
                    about = about,
                    nip05 = nip05,
                    lnAddress = lnAddress,
                    lnURL = lnURL,
                    pronouns = pronouns,
                    twitter = twitter,
                    mastodon = mastodon,
                    github = github,
                )
            }

        val signedEvent = account.signer.sign<MetadataEvent>(template)

        val connectedRelays = relayManager.connectedRelays.value
        if (connectedRelays.isEmpty()) {
            throw IllegalStateException("Cannot save profile: No connected relays")
        }

        relayManager.broadcastToAll(signedEvent)
    }
}
