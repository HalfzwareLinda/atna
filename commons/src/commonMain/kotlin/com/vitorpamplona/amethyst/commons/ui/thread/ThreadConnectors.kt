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
package com.vitorpamplona.amethyst.commons.ui.thread

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Draws Reddit-style thread connector lines for a reply at the given nesting level.
 *
 * Visual behavior:
 * - Vertical continuation lines are drawn for ancestor levels that have more siblings below
 * - An L-shaped curved connector links the parent's vertical line to this reply
 * - If this is the last child, the vertical line stops at the curve; otherwise it continues
 *
 * @param level Nesting level (1 = direct reply to root, 2 = reply to a reply, etc.)
 * @param isLastChild Whether this is the last child of its parent
 * @param ancestorContinuation For each ancestor level index 0..(level-2), true means
 *        "draw a continuation line at that level because the ancestor has more siblings below"
 * @param lineColor Color for all connector lines
 * @param indentDp Horizontal indent per nesting level in dp
 */
fun Modifier.drawRedditConnectors(
    level: Int,
    isLastChild: Boolean,
    ancestorContinuation: BooleanArray,
    lineColor: Color,
    indentDp: Float = 24f,
): Modifier {
    if (level <= 0) return this

    return this
        .drawBehind {
            val indent = indentDp.dp.toPx()
            val strokeWidth = 2.dp.toPx()
            val curveRadius = 8.dp.toPx()

            // 1. Draw vertical continuation lines for ancestors that have more siblings
            for (i in ancestorContinuation.indices) {
                if (ancestorContinuation[i]) {
                    val x = (i + 1) * indent
                    drawLine(
                        color = lineColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }
            }

            // 2. Draw the L-shaped connector for this item
            val connectorX = level * indent

            // Vertical segment: from top down to the curve start
            val verticalEnd = curveRadius
            drawLine(
                color = lineColor,
                start = Offset(connectorX, 0f),
                end = Offset(connectorX, verticalEnd),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )

            // Curved corner: quarter arc from vertical to horizontal
            val path =
                Path().apply {
                    moveTo(connectorX, verticalEnd)
                    quadraticBezierTo(
                        connectorX,
                        verticalEnd + curveRadius,
                        connectorX + curveRadius,
                        verticalEnd + curveRadius,
                    )
                }
            drawPath(
                path,
                lineColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // 3. If not the last child, continue the vertical line below the curve
            if (!isLastChild) {
                drawLine(
                    color = lineColor,
                    start = Offset(connectorX, verticalEnd + curveRadius),
                    end = Offset(connectorX, size.height),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }.padding(start = (level * indentDp).dp + 10.dp)
}
