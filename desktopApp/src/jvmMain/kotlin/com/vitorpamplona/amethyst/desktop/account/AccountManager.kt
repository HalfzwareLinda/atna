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
package com.vitorpamplona.amethyst.desktop.account

import androidx.compose.runtime.Stable
import com.vitorpamplona.amethyst.commons.keystorage.SecureKeyStorage
import com.vitorpamplona.amethyst.commons.keystorage.SecureStorageException
import com.vitorpamplona.amethyst.desktop.network.RelayConnectionManager
import com.vitorpamplona.quartz.marmotMls.MarmotKeyPackageRelayListEvent
import com.vitorpamplona.quartz.nip01Core.core.hexToByteArray
import com.vitorpamplona.quartz.nip01Core.core.toHexKey
import com.vitorpamplona.quartz.nip01Core.crypto.KeyPair
import com.vitorpamplona.quartz.nip01Core.relay.normalizer.RelayUrlNormalizer
import com.vitorpamplona.quartz.nip01Core.signers.NostrSigner
import com.vitorpamplona.quartz.nip01Core.signers.NostrSignerInternal
import com.vitorpamplona.quartz.nip17Dm.settings.ChatMessageRelayListEvent
import com.vitorpamplona.quartz.nip19Bech32.decodePrivateKeyAsHexOrNull
import com.vitorpamplona.quartz.nip19Bech32.decodePublicKeyAsHexOrNull
import com.vitorpamplona.quartz.nip19Bech32.toNpub
import com.vitorpamplona.quartz.nip19Bech32.toNsec
import com.vitorpamplona.quartz.nip47WalletConnect.Nip47WalletConnect
import com.vitorpamplona.quartz.nip65RelayList.AdvertisedRelayListEvent
import com.vitorpamplona.quartz.nip65RelayList.tags.AdvertisedRelayInfo
import com.vitorpamplona.quartz.nip65RelayList.tags.AdvertisedRelayType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

sealed class AccountState {
    data object LoggedOut : AccountState()

    data class LoggedIn(
        val signer: NostrSigner,
        val pubKeyHex: String,
        val npub: String,
        val nsec: String?,
        val isReadOnly: Boolean,
    ) : AccountState()
}

@Stable
class AccountManager private constructor(
    private val secureStorage: SecureKeyStorage,
) {
    companion object {
        /**
         * Creates an AccountManager instance.
         *
         * @param context Platform-specific context (required on Android, ignored on Desktop)
         * @return AccountManager instance
         */
        fun create(context: Any? = null): AccountManager {
            val storage = SecureKeyStorage.create(context)
            return AccountManager(storage)
        }
    }

    private val _accountState = MutableStateFlow<AccountState>(AccountState.LoggedOut)
    val accountState: StateFlow<AccountState> = _accountState.asStateFlow()

    private val _nwcConnection = MutableStateFlow<Nip47WalletConnect.Nip47URINorm?>(null)
    val nwcConnection: StateFlow<Nip47WalletConnect.Nip47URINorm?> = _nwcConnection.asStateFlow()

    /**
     * Loads the last saved account from secure storage.
     * Call on app startup.
     */
    suspend fun loadSavedAccount(): Result<AccountState.LoggedIn> {
        return try {
            // For simplicity, we'll store the last logged-in npub in a simple file
            // and use SecureKeyStorage to retrieve the private key
            val lastNpub = getLastNpub()
            if (lastNpub == null) {
                println("[AutoLogin] No saved account found (no last_account.txt)")
                return Result.failure(Exception("No saved account"))
            }
            println("[AutoLogin] Found saved npub: ${lastNpub.take(16)}...")

            val privKeyHex =
                try {
                    secureStorage.getPrivateKey(lastNpub)
                } catch (e: Exception) {
                    println("[AutoLogin] SecureKeyStorage.getPrivateKey() threw: ${e.javaClass.simpleName}: ${e.message}")
                    e.printStackTrace()
                    null
                }
            if (privKeyHex == null) {
                println("[AutoLogin] Private key not found in secure storage for $lastNpub")
                return Result.failure(Exception("Private key not found for $lastNpub"))
            }
            println("[AutoLogin] Private key retrieved successfully (${privKeyHex.length} hex chars)")

            val keyPair = KeyPair(privKey = privKeyHex.hexToByteArray())
            val signer = NostrSignerInternal(keyPair)

            val state =
                AccountState.LoggedIn(
                    signer = signer,
                    pubKeyHex = keyPair.pubKey.toHexKey(),
                    npub = keyPair.pubKey.toNpub(),
                    nsec = keyPair.privKey?.toNsec(),
                    isReadOnly = false,
                )
            _accountState.value = state
            println("[AutoLogin] Account loaded successfully: ${state.npub.take(16)}...")
            Result.success(state)
        } catch (e: Exception) {
            println("[AutoLogin] Failed to load account: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Saves the current account to secure storage.
     */
    suspend fun saveCurrentAccount(): Result<Unit> {
        val current = currentAccount() ?: return Result.failure(Exception("No account logged in"))
        if (current.isReadOnly || current.nsec == null) {
            return Result.failure(Exception("Cannot save read-only account"))
        }

        return try {
            val privKeyHex =
                decodePrivateKeyAsHexOrNull(current.nsec)
                    ?: return Result.failure(Exception("Invalid nsec format"))

            secureStorage.savePrivateKey(current.npub, privKeyHex)
            saveLastNpub(current.npub)
            Result.success(Unit)
        } catch (e: SecureStorageException) {
            Result.failure(e)
        }
    }

    fun generateNewAccount(): AccountState.LoggedIn {
        val keyPair = KeyPair()
        val signer = NostrSignerInternal(keyPair)

        return AccountState.LoggedIn(
            signer = signer,
            pubKeyHex = keyPair.pubKey.toHexKey(),
            npub = keyPair.pubKey.toNpub(),
            nsec = keyPair.privKey?.toNsec(),
            isReadOnly = false,
        )
        // Don't set _accountState here — caller must call persistAndActivate()
    }

    fun loginWithKey(keyInput: String): Result<AccountState.LoggedIn> {
        val trimmedInput = keyInput.trim()

        // Try as private key first (nsec or hex)
        val privKeyHex = decodePrivateKeyAsHexOrNull(trimmedInput)
        if (privKeyHex != null) {
            return try {
                val keyPair = KeyPair(privKey = privKeyHex.hexToByteArray())
                val signer = NostrSignerInternal(keyPair)

                val state =
                    AccountState.LoggedIn(
                        signer = signer,
                        pubKeyHex = keyPair.pubKey.toHexKey(),
                        npub = keyPair.pubKey.toNpub(),
                        nsec = keyPair.privKey?.toNsec(),
                        isReadOnly = false,
                    )
                // Don't set _accountState here — caller must call activateAccount()
                // after persisting credentials, to avoid the state change triggering
                // navigation before persistence completes.
                Result.success(state)
            } catch (e: Exception) {
                Result.failure(IllegalArgumentException("Invalid private key format"))
            }
        }

        // Try as public key (npub or hex) - read-only mode
        val pubKeyHex = decodePublicKeyAsHexOrNull(trimmedInput)
        if (pubKeyHex != null) {
            return try {
                val keyPair = KeyPair(pubKey = pubKeyHex.hexToByteArray())
                val signer = NostrSignerInternal(keyPair)

                val state =
                    AccountState.LoggedIn(
                        signer = signer,
                        pubKeyHex = keyPair.pubKey.toHexKey(),
                        npub = keyPair.pubKey.toNpub(),
                        nsec = null,
                        isReadOnly = true,
                    )
                // Read-only accounts have no credentials to persist, activate immediately
                _accountState.value = state
                Result.success(state)
            } catch (e: Exception) {
                Result.failure(IllegalArgumentException("Invalid public key format"))
            }
        }

        return Result.failure(IllegalArgumentException("Invalid key format. Use nsec1, npub1, or hex format."))
    }

    /**
     * Persists the given account credentials and then activates the account state.
     * Must be called after [loginWithKey] or [generateNewAccount] for non-read-only accounts.
     * This ensures credentials are saved before the state change triggers UI navigation.
     */
    suspend fun persistAndActivate(state: AccountState.LoggedIn) {
        if (!state.isReadOnly && state.nsec != null) {
            val privKeyHex = decodePrivateKeyAsHexOrNull(state.nsec)
            if (privKeyHex != null) {
                try {
                    secureStorage.savePrivateKey(state.npub, privKeyHex)
                    saveLastNpub(state.npub)
                } catch (e: Exception) {
                    System.err.println("Warning: Failed to persist account: ${e.message}")
                }
            }
        }
        _accountState.value = state
    }

    suspend fun logout(deleteKey: Boolean = false) {
        val current = currentAccount()
        if (current != null) {
            if (deleteKey) {
                try {
                    secureStorage.deletePrivateKey(current.npub)
                } catch (e: SecureStorageException) {
                    // Log error but still logout
                }
            }
            // Always clear saved session so the app doesn't auto-login on restart
            clearLastNpub()
        }
        _accountState.value = AccountState.LoggedOut
    }

    fun isLoggedIn(): Boolean = _accountState.value is AccountState.LoggedIn

    fun currentAccount(): AccountState.LoggedIn? = _accountState.value as? AccountState.LoggedIn

    // NWC (Nostr Wallet Connect) methods
    fun hasNwcSetup(): Boolean = _nwcConnection.value != null

    fun setNwcConnection(uri: String): Result<Nip47WalletConnect.Nip47URINorm> =
        try {
            val parsed = Nip47WalletConnect.parse(uri)
            _nwcConnection.value = parsed
            saveNwcUri(uri)
            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }

    fun clearNwcConnection() {
        _nwcConnection.value = null
        getNwcFile().delete()
    }

    /**
     * Broadcasts initial relay events for a new account.
     * Mirrors what Android does in AccountStateViewModel.newKey().
     * Idempotent: uses a marker file to only broadcast once per account.
     */
    suspend fun broadcastInitialRelayEvents(relayManager: RelayConnectionManager) {
        val account = currentAccount() ?: return
        if (account.isReadOnly) return

        // Check if relay events have already been broadcast for this account
        val markerFile = getRelayBroadcastMarker(account.npub)
        if (markerFile.exists()) return

        val signer = account.signer

        // Wait for at least one relay to connect (up to 5s timeout)
        withTimeoutOrNull(5000) {
            relayManager.connectedRelays.first { it.isNotEmpty() }
        }

        val defaultRelays =
            listOf(
                "wss://nostr.mom",
                "wss://nos.lol",
                "wss://nostr.bitcoiner.social",
            ).mapNotNull { RelayUrlNormalizer.normalizeOrNull(it) }

        val dmRelays =
            listOf(
                "wss://auth.nostr1.com",
                "wss://relay.0xchat.com",
                "wss://nos.lol",
            ).mapNotNull { RelayUrlNormalizer.normalizeOrNull(it) }

        val marmotRelays =
            listOf(
                "wss://relay.damus.io",
                "wss://relay.primal.net",
                "wss://nos.lol",
            ).mapNotNull { RelayUrlNormalizer.normalizeOrNull(it) }

        // Kind 10002: NIP-65 Advertised Relay List
        val nip65Event =
            AdvertisedRelayListEvent.create(
                list = defaultRelays.map { AdvertisedRelayInfo(it, AdvertisedRelayType.BOTH) },
                signer = signer,
            )
        relayManager.broadcastToAll(nip65Event)

        // Kind 10050: DM Relay List
        val dmEvent =
            ChatMessageRelayListEvent.create(
                relays = dmRelays,
                signer = signer,
            )
        relayManager.broadcastToAll(dmEvent)

        // Kind 10051: Marmot Key Package Relay List
        val marmotEvent =
            MarmotKeyPackageRelayListEvent.create(
                relays = marmotRelays,
                signer = signer,
            )
        relayManager.broadcastToAll(marmotEvent)

        // Mark relay events as broadcast for this account
        markerFile.parentFile?.mkdirs()
        markerFile.writeText("broadcast")
    }

    private fun getRelayBroadcastMarker(npub: String): File {
        val homeDir = System.getProperty("user.home")
        return File(homeDir, ".amethyst/relay_broadcast_${npub.takeLast(8)}.marker")
    }

    fun loadNwcConnection() {
        val uri = getNwcFile().takeIf { it.exists() }?.readText()?.trim()
        if (!uri.isNullOrEmpty()) {
            try {
                _nwcConnection.value = Nip47WalletConnect.parse(uri)
            } catch (e: Exception) {
                // Invalid stored URI, clear it
                getNwcFile().delete()
            }
        }
    }

    private fun saveNwcUri(uri: String) {
        val file = getNwcFile()
        file.parentFile?.mkdirs()
        file.writeText(uri)
    }

    private fun getNwcFile(): java.io.File {
        val homeDir = System.getProperty("user.home")
        return java.io.File(homeDir, ".amethyst/nwc_connection.txt")
    }

    // Simple file-based storage for last npub (non-sensitive data)
    private fun getLastNpub(): String? {
        val file = getPrefsFile()
        return if (file.exists()) file.readText().trim().takeIf { it.isNotEmpty() } else null
    }

    private fun saveLastNpub(npub: String) {
        val file = getPrefsFile()
        file.parentFile?.mkdirs()
        file.writeText(npub)
    }

    private fun clearLastNpub() {
        getPrefsFile().delete()
    }

    private fun getPrefsFile(): File {
        val homeDir = System.getProperty("user.home")
        return File(homeDir, ".amethyst/last_account.txt")
    }
}
