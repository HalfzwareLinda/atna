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

import com.vitorpamplona.quartz.nip01Core.core.Event
import com.vitorpamplona.quartz.nip01Core.relay.client.NostrClient
import com.vitorpamplona.quartz.nip01Core.relay.client.listeners.IRelayClientListener
import com.vitorpamplona.quartz.nip01Core.relay.client.reqs.IRequestListener
import com.vitorpamplona.quartz.nip01Core.relay.client.single.IRelayClient
import com.vitorpamplona.quartz.nip01Core.relay.commands.toClient.Message
import com.vitorpamplona.quartz.nip01Core.relay.commands.toRelay.Command
import com.vitorpamplona.quartz.nip01Core.relay.filters.Filter
import com.vitorpamplona.quartz.nip01Core.relay.normalizer.NormalizedRelayUrl
import com.vitorpamplona.quartz.nip01Core.relay.normalizer.RelayUrlNormalizer
import com.vitorpamplona.quartz.nip01Core.relay.sockets.WebsocketBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages Nostr relay connections, subscriptions, and status tracking.
 * Can be used by both Android and Desktop apps.
 *
 * @param websocketBuilder Platform-specific websocket builder (e.g., OkHttp-based)
 */
open class RelayConnectionManager(
    websocketBuilder: WebsocketBuilder,
) : IRelayClientListener {
    private val _client = NostrClient(websocketBuilder)

    /** Exposes the underlying INostrClient for subscription coordinators */
    val client: com.vitorpamplona.quartz.nip01Core.relay.client.INostrClient get() = _client

    private val _relayStatuses = MutableStateFlow<Map<NormalizedRelayUrl, RelayStatus>>(emptyMap())
    val relayStatuses: StateFlow<Map<NormalizedRelayUrl, RelayStatus>> = _relayStatuses.asStateFlow()

    val connectedRelays: StateFlow<Set<NormalizedRelayUrl>> = _client.connectedRelaysFlow()
    val availableRelays: StateFlow<Set<NormalizedRelayUrl>> = _client.availableRelaysFlow()

    /**
     * Marks a relay as authenticated in status tracking.
     * Call from auth coordinator when NIP-42 AUTH succeeds.
     */
    fun markRelayAuthenticated(url: NormalizedRelayUrl) {
        updateRelayStatus(url) { it.copy(authenticated = true) }
    }

    init {
        _client.subscribe(this)
    }

    fun connect() {
        println("[Relay] connect() called — available relays: ${availableRelays.value.size}, connected: ${connectedRelays.value.size}")
        _client.connect()
    }

    fun disconnect() {
        _client.disconnect()
    }

    fun addRelay(url: String): NormalizedRelayUrl? {
        val normalized = RelayUrlNormalizer.Companion.normalizeOrNull(url) ?: return null
        updateRelayStatus(normalized) { it.copy(connected = false, error = null) }
        _client.addRelayToPool(normalized)
        return normalized
    }

    fun removeRelay(url: NormalizedRelayUrl) {
        _relayStatuses.update { it - url }
    }

    /**
     * Clears all relay state (pool + status tracking).
     * Call on logout before re-adding relays for a new session.
     */
    fun clearRelays() {
        _client.removeAllRelaysFromPool()
        _relayStatuses.update { emptyMap() }
    }

    fun addDefaultRelays() {
        println("[Relay] Adding ${DefaultRelays.RELAYS.size} default relays: ${DefaultRelays.RELAYS}")
        DefaultRelays.RELAYS.forEach { addRelay(it) }
    }

    fun subscribe(
        subId: String,
        filters: List<Filter>,
        relays: Set<NormalizedRelayUrl> = availableRelays.value,
        listener: IRequestListener? = null,
    ) {
        val filterMap = relays.associateWith { filters }
        _client.openReqSubscription(subId, filterMap, listener)
    }

    /**
     * Subscribes with per-relay filter routing (outbox model).
     * Each relay receives only the filters relevant to users on that relay.
     */
    fun subscribeRouted(
        subId: String,
        filterMap: Map<NormalizedRelayUrl, List<Filter>>,
        listener: IRequestListener? = null,
    ) {
        _client.openReqSubscription(subId, filterMap, listener)
    }

    fun unsubscribe(subId: String) {
        _client.close(subId)
    }

    fun send(
        event: Event,
        relays: Set<NormalizedRelayUrl> = connectedRelays.value,
    ) {
        _client.send(event, relays)
    }

    /**
     * Broadcasts an event to all connected relays.
     */
    fun broadcastToAll(event: Event) {
        val connected = connectedRelays.value
        send(event, connected)
    }

    /**
     * Sends an event to a specific relay (for NWC).
     * Adds the relay if not already in the list.
     */
    fun sendToRelay(
        relay: NormalizedRelayUrl,
        event: Event,
    ) {
        if (relay !in availableRelays.value) {
            updateRelayStatus(relay) { it.copy(connected = false, error = null) }
        }
        _client.send(event, setOf(relay))
    }

    /**
     * Subscribes on a specific relay (for NWC).
     * Adds the relay if not already in the list.
     */
    fun subscribeOnRelay(
        relay: NormalizedRelayUrl,
        subId: String,
        filters: List<Filter>,
        onEvent: (Event, NormalizedRelayUrl) -> Unit,
    ) {
        if (relay !in availableRelays.value) {
            updateRelayStatus(relay) { it.copy(connected = false, error = null) }
        }
        val filterMap = mapOf(relay to filters)
        _client.openReqSubscription(
            subId = subId,
            filters = filterMap,
            listener =
                object : IRequestListener {
                    override fun onEvent(
                        event: Event,
                        isLive: Boolean,
                        relay: NormalizedRelayUrl,
                        forFilters: List<Filter>?,
                    ) {
                        onEvent(event, relay)
                    }
                },
        )
    }

    /**
     * Closes a subscription on a specific relay.
     */
    fun closeSubscription(
        relay: NormalizedRelayUrl,
        subId: String,
    ) {
        _client.close(subId)
    }

    private fun updateRelayStatus(
        url: NormalizedRelayUrl,
        update: (RelayStatus) -> RelayStatus,
    ) {
        _relayStatuses.update { current ->
            val status = current[url] ?: RelayStatus(url, connected = false)
            current + (url to update(status))
        }
    }

    override fun onConnecting(relay: IRelayClient) {
        println("[Relay] Connecting to ${relay.url}...")
        updateRelayStatus(relay.url) {
            it.copy(connected = false, error = null)
        }
    }

    override fun onConnected(
        relay: IRelayClient,
        pingMillis: Int,
        compressed: Boolean,
    ) {
        println("[Relay] Connected to ${relay.url} (ping=${pingMillis}ms, compressed=$compressed)")
        updateRelayStatus(relay.url) {
            it.copy(connected = true, pingMs = pingMillis, compressed = compressed, error = null)
        }
    }

    override fun onDisconnected(relay: IRelayClient) {
        println("[Relay] Disconnected from ${relay.url}")
        updateRelayStatus(relay.url) {
            it.copy(connected = false)
        }
    }

    override fun onCannotConnect(
        relay: IRelayClient,
        errorMessage: String,
    ) {
        System.err.println("[Relay] Cannot connect to ${relay.url}: $errorMessage")
        updateRelayStatus(relay.url) {
            it.copy(connected = false, error = errorMessage)
        }
    }

    override fun onIncomingMessage(
        relay: IRelayClient,
        msgStr: String,
        msg: Message,
    ) {
        // Events are handled by subscription listeners
    }

    override fun onSent(
        relay: IRelayClient,
        cmdStr: String,
        cmd: Command,
        success: Boolean,
    ) {
        // Command send tracking
    }
}
