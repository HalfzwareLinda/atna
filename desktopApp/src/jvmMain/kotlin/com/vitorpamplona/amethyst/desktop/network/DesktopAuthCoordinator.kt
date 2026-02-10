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
package com.vitorpamplona.amethyst.desktop.network

import com.vitorpamplona.quartz.nip01Core.relay.client.INostrClient
import com.vitorpamplona.quartz.nip01Core.relay.client.auth.RelayAuthenticator
import com.vitorpamplona.quartz.nip01Core.relay.normalizer.NormalizedRelayUrl
import com.vitorpamplona.quartz.nip01Core.signers.NostrSigner
import kotlinx.coroutines.CoroutineScope

/**
 * Desktop NIP-42 relay authentication coordinator.
 *
 * Wraps the KMP [RelayAuthenticator] to handle AUTH challenges from relays.
 * When a relay sends an AUTH message, this coordinator signs the challenge
 * with all logged-in signers and sends the response back.
 *
 * @param onAuthSuccess Optional callback invoked when a relay completes AUTH.
 *                      Use to update relay status tracking.
 */
class DesktopAuthCoordinator(
    client: INostrClient,
    scope: CoroutineScope,
    private val getSigners: () -> List<NostrSigner>,
    private val onAuthSuccess: ((NormalizedRelayUrl) -> Unit)? = null,
) {
    private val notifiedRelays =
        java.util.concurrent.ConcurrentHashMap
            .newKeySet<NormalizedRelayUrl>()

    private val authenticator =
        RelayAuthenticator(
            client = client,
            scope = scope,
            signWithAllLoggedInUsers = { template ->
                getSigners()
                    .filter { it.isWriteable() }
                    .map { signer -> signer.sign(template) }
            },
        )

    fun hasFinishedAuth(relay: NormalizedRelayUrl): Boolean {
        val finished = authenticator.hasFinishedAuthentication(relay)
        if (finished && notifiedRelays.add(relay)) {
            onAuthSuccess?.invoke(relay)
        }
        return finished
    }

    fun destroy() {
        authenticator.destroy()
    }
}
