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
package com.vitorpamplona.amethyst.desktop.preview

import com.vitorpamplona.amethyst.commons.preview.OpenGraphParser
import com.vitorpamplona.amethyst.commons.preview.UrlInfoItem
import com.vitorpamplona.amethyst.desktop.network.DesktopHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.coroutines.executeAsync

class DesktopUrlPreviewFetcher {
    suspend fun getDocument(url: String): UrlInfoItem =
        withContext(Dispatchers.IO) {
            val client = DesktopHttpClient.getHttpClientForUrl(url)

            // HEAD request first to detect Content-Type without downloading the body.
            // This avoids downloading entire video/image files just for MIME detection.
            val headRequest =
                Request
                    .Builder()
                    .url(url)
                    .head()
                    .build()

            val headResponse = client.newCall(headRequest).executeAsync()
            val contentType = headResponse.headers["Content-Type"]
            headResponse.close()

            if (!headResponse.isSuccessful || contentType == null) {
                // Fall back to GET if HEAD fails (some servers return 405)
                return@withContext getDocumentViaGet(client, url)
            }

            val mimeType = contentType.toMediaType()

            if (mimeType.type == "text" && mimeType.subtype == "html") {
                // Only do full GET for HTML pages (to parse OpenGraph tags)
                getDocumentViaGet(client, url)
            } else if (mimeType.type == "image") {
                UrlInfoItem(url, image = url, mimeType = mimeType.toString())
            } else if (mimeType.type == "video") {
                UrlInfoItem(url, image = url, mimeType = mimeType.toString())
            } else {
                throw IllegalArgumentException("Unsupported content type: $mimeType")
            }
        }

    private suspend fun getDocumentViaGet(
        client: okhttp3.OkHttpClient,
        url: String,
    ): UrlInfoItem {
        val request =
            Request
                .Builder()
                .url(url)
                .get()
                .build()

        return client.newCall(request).executeAsync().use { response ->
            if (response.isSuccessful) {
                val mimeType =
                    response.headers["Content-Type"]?.toMediaType()
                        ?: throw IllegalArgumentException(
                            "Website returned unknown mimetype: ${response.headers["Content-Type"]}",
                        )
                if (mimeType.type == "text" && mimeType.subtype == "html") {
                    val metaTags =
                        DesktopHtmlParser().parseHtml(
                            response.body.source(),
                            mimeType.charset(),
                        )
                    val data = OpenGraphParser().extractUrlInfo(metaTags)
                    UrlInfoItem(url, data.title, data.description, data.image, mimeType.toString())
                } else if (mimeType.type == "image") {
                    UrlInfoItem(url, image = url, mimeType = mimeType.toString())
                } else if (mimeType.type == "video") {
                    UrlInfoItem(url, image = url, mimeType = mimeType.toString())
                } else {
                    throw IllegalArgumentException("Unsupported content type: $mimeType")
                }
            } else {
                throw IllegalArgumentException("Website returned: ${response.code}")
            }
        }
    }
}
