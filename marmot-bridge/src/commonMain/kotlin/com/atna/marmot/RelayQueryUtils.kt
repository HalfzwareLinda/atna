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
package com.atna.marmot

import com.vitorpamplona.quartz.marmotMls.MarmotKeyPackageEvent
import com.vitorpamplona.quartz.nip01Core.core.Event
import com.vitorpamplona.quartz.nip01Core.core.hexToByteArray
import com.vitorpamplona.quartz.nip01Core.relay.client.INostrClient
import com.vitorpamplona.quartz.nip01Core.relay.client.reqs.IOpenNostrRequest
import com.vitorpamplona.quartz.nip01Core.relay.client.reqs.req
import com.vitorpamplona.quartz.nip01Core.relay.filters.Filter
import com.vitorpamplona.quartz.nip01Core.relay.normalizer.NormalizedRelayUrl
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Default timeout for one-shot relay queries (ms). */
const val RELAY_QUERY_TIMEOUT_MS = 5000L

/**
 * Sends a one-shot query to the given relays and returns the first matching event,
 * or null if no event arrives within the timeout.
 *
 * Shared between Android ViewModel and Desktop screen to eliminate code duplication.
 */
suspend fun queryRelayForEvent(
    client: INostrClient,
    filter: Filter,
    relayUrls: List<NormalizedRelayUrl>,
    timeoutMs: Long = RELAY_QUERY_TIMEOUT_MS,
): Event? {
    if (relayUrls.isEmpty()) return null

    return withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine { cont: CancellableContinuation<Event> ->
            var reqHolder: IOpenNostrRequest? = null
            val req =
                client.req(relayUrls, filter) { event ->
                    if (cont.isActive) {
                        cont.resume(event)
                        reqHolder?.close()
                    }
                }
            reqHolder = req
            cont.invokeOnCancellation { req.close() }
        }
    }
}

/** OpenMLS extension type name-to-ID mapping. */
private val MLS_EXTENSION_IDS =
    mapOf(
        "ApplicationId" to 0x0001,
        "RatchetTree" to 0x0002,
        "RequiredCapabilities" to 0x0003,
        "ExternalPub" to 0x0004,
        "ExternalSenders" to 0x0005,
        "LastResort" to 0x000A,
    )

/** Regex to extract the numeric ID from Unknown(NNNNN) extension names. */
private val UNKNOWN_EXT_REGEX = Regex("""Unknown\((\d+)\)""")

/**
 * Converts an MLS extension name (e.g. "RequiredCapabilities", "Unknown(62190)")
 * to the 0xXXXX hex format required by the MDK library.
 */
private fun extensionNameToHex(name: String): String {
    MLS_EXTENSION_IDS[name]?.let { id ->
        return "0x${id.toString(16).padStart(4, '0')}"
    }
    UNKNOWN_EXT_REGEX.matchEntire(name)?.let { match ->
        val id = match.groupValues[1].toIntOrNull() ?: return name
        return "0x${id.toString(16).padStart(4, '0')}"
    }
    return name
}

/** Converts a single value to 0xXXXX hex if it's a plain integer. */
private fun intToHex(value: String): String {
    if (value.startsWith("0x")) return value
    val intVal = value.toIntOrNull() ?: return value
    return "0x${intVal.toString(16).padStart(4, '0')}"
}

/** Returns true if the string looks like a hex-encoded byte string (even length, all hex chars). */
private fun isHexEncoded(s: String): Boolean = s.length >= 2 && s.length % 2 == 0 && s.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }

/**
 * Normalizes a key package event for the MDK library. Handles:
 * - ciphersuite/protocol_version as plain integers → 0xXXXX hex
 * - extensions as comma-separated names → separate hex values
 * - missing ["encoding", "base64"] tag (required by MDK ≥ v1)
 * - hex-encoded content → base64 conversion for older key packages
 *
 * Returns the event JSON with normalized tags, reconstructing the event if needed.
 */
@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
fun normalizeKeyPackageEventJson(event: Event): String {
    var hasEncodingTag = false
    var currentEncoding: String? = null

    val normalizedTags =
        event.tags
            .map { tag ->
                if (tag.size < 2) return@map tag
                when (tag[0]) {
                    "mls_ciphersuite", "mls_protocol_version" -> {
                        val hex = intToHex(tag[1])
                        if (hex == tag[1]) tag else arrayOf(tag[0], hex)
                    }
                    "mls_extensions" -> {
                        if (tag.size == 2 && tag[1].contains(",")) {
                            val names = tag[1].split(",").map { it.trim() }
                            arrayOf(tag[0]) + names.map { extensionNameToHex(it) }.toTypedArray()
                        } else {
                            arrayOf(tag[0]) + tag.drop(1).map { intToHex(it) }.toTypedArray()
                        }
                    }
                    "encoding" -> {
                        hasEncodingTag = true
                        currentEncoding = tag[1].lowercase()
                        tag
                    }
                    else -> tag
                }
            }.toTypedArray()

    // Determine if content needs hex→base64 conversion
    var content = event.content
    val finalTags: Array<Array<String>>

    if (!hasEncodingTag) {
        // No encoding tag — older client. Content is likely hex-encoded.
        // Convert to base64 and add the required encoding tag.
        if (isHexEncoded(content)) {
            val bytes = content.hexToByteArray()
            content =
                kotlin.io.encoding.Base64
                    .encode(bytes)
        }
        finalTags = normalizedTags + arrayOf(arrayOf("encoding", "base64"))
    } else {
        finalTags = normalizedTags
    }

    // Check if anything actually changed
    if (finalTags.contentDeepEquals(event.tags) && content == event.content) {
        return event.toJson()
    }

    return MarmotKeyPackageEvent(
        id = event.id,
        pubKey = event.pubKey,
        createdAt = event.createdAt,
        tags = finalTags,
        content = content,
        sig = event.sig,
    ).toJson()
}
