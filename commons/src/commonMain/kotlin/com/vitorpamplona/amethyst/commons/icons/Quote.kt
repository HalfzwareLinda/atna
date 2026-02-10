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
package com.vitorpamplona.amethyst.commons.icons

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun VectorPreview() {
    Image(Quote, null)
}

private var quote: androidx.compose.ui.graphics.vector.ImageVector? = null

val Quote: androidx.compose.ui.graphics.vector.ImageVector
    get() =
        quote ?: materialIcon(name = "Quote") {
            materialOutlinedPath {
                // Left quotation mark — open curve dropping into a tail
                moveTo(6.0f, 10.0f)
                curveTo(6.0f, 7.79f, 7.79f, 6.0f, 10.0f, 6.0f)
                verticalLineTo(5.0f)
                curveTo(7.24f, 5.0f, 5.0f, 7.24f, 5.0f, 10.0f)
                verticalLineTo(14.0f)
                curveTo(5.0f, 15.1f, 5.9f, 16.0f, 7.0f, 16.0f)
                horizontalLineTo(10.0f)
                curveTo(11.1f, 16.0f, 12.0f, 15.1f, 12.0f, 14.0f)
                verticalLineTo(11.0f)
                curveTo(12.0f, 9.9f, 11.1f, 9.0f, 10.0f, 9.0f)
                horizontalLineTo(6.0f)
                verticalLineTo(10.0f)
                close()
                // Right quotation mark — same shape shifted right
                moveTo(16.0f, 10.0f)
                curveTo(16.0f, 7.79f, 17.79f, 6.0f, 20.0f, 6.0f)
                verticalLineTo(5.0f)
                curveTo(17.24f, 5.0f, 15.0f, 7.24f, 15.0f, 10.0f)
                verticalLineTo(14.0f)
                curveTo(15.0f, 15.1f, 15.9f, 16.0f, 17.0f, 16.0f)
                horizontalLineTo(20.0f)
                curveTo(21.1f, 16.0f, 22.0f, 15.1f, 22.0f, 14.0f)
                verticalLineTo(11.0f)
                curveTo(22.0f, 9.9f, 21.1f, 9.0f, 20.0f, 9.0f)
                horizontalLineTo(16.0f)
                verticalLineTo(10.0f)
                close()
            }
        }.also { quote = it }
