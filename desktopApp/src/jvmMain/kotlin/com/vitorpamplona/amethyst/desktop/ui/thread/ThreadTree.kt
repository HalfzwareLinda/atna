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
package com.vitorpamplona.amethyst.desktop.ui.thread

import com.vitorpamplona.amethyst.desktop.ui.findReplyToId
import com.vitorpamplona.quartz.nip01Core.core.Event

/**
 * A node in a flattened thread tree, suitable for rendering in a LazyColumn.
 * Pre-computes all the information needed for Reddit-style connector drawing.
 */
data class ThreadNode(
    val event: Event,
    val level: Int,
    val isLastChild: Boolean,
    val parentId: String?,
    val descendantCount: Int,
    /** For each ancestor level 0..(level-2), true if that ancestor has more siblings below. */
    val ancestorContinuation: BooleanArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ThreadNode) return false
        return event.id == other.event.id
    }

    override fun hashCode(): Int = event.id.hashCode()
}

/**
 * Builds a flattened depth-first thread tree from a sorted list of reply events.
 * Each node includes pre-computed information for Reddit-style connector rendering.
 *
 * @param rootNoteId The root note ID of the thread
 * @param replyEvents All reply events (sorted by createdAt ascending)
 * @return Flat list of ThreadNodes in depth-first order
 */
fun buildThreadTree(
    rootNoteId: String,
    replyEvents: List<Event>,
): List<ThreadNode> {
    if (replyEvents.isEmpty()) return emptyList()

    // Build parent→children map
    val childrenMap = mutableMapOf<String, MutableList<Event>>()
    for (event in replyEvents) {
        val replyToId = findReplyToId(event) ?: rootNoteId
        // If the parent isn't the root and isn't in our event set, attach to root
        val effectiveParent =
            if (replyToId != rootNoteId && replyEvents.none { it.id == replyToId }) {
                rootNoteId
            } else {
                replyToId
            }
        childrenMap.getOrPut(effectiveParent) { mutableListOf() }.add(event)
    }

    // Sort children by createdAt within each parent
    for ((_, children) in childrenMap) {
        children.sortBy { it.createdAt }
    }

    // Count descendants recursively
    val descendantCounts = mutableMapOf<String, Int>()

    fun countDescendants(eventId: String): Int {
        descendantCounts[eventId]?.let { return it }
        val children = childrenMap[eventId] ?: return 0
        val count = children.size + children.sumOf { countDescendants(it.id) }
        descendantCounts[eventId] = count
        return count
    }

    // Pre-compute all descendant counts
    for (event in replyEvents) {
        countDescendants(event.id)
    }

    // Flatten depth-first with continuation tracking
    val result = mutableListOf<ThreadNode>()

    fun flatten(
        parentId: String,
        level: Int,
        parentContinuation: BooleanArray,
    ) {
        val children = childrenMap[parentId] ?: return
        children.forEachIndexed { index, child ->
            val isLast = index == children.size - 1

            // Build continuation array for this node:
            // Copy parent's continuation + add whether parent level continues
            val continuation = BooleanArray(maxOf(0, level - 1))
            // Copy ancestor continuation from parent
            for (i in parentContinuation.indices) {
                if (i < continuation.size) {
                    continuation[i] = parentContinuation[i]
                }
            }
            // The parent level (level-2 index, since level is 1-based) continues
            // if this is NOT the last child at this level
            if (level >= 2 && level - 2 < continuation.size) {
                continuation[level - 2] = !isLast
            }

            result.add(
                ThreadNode(
                    event = child,
                    level = level,
                    isLastChild = isLast,
                    parentId = parentId,
                    descendantCount = descendantCounts[child.id] ?: 0,
                    ancestorContinuation = continuation,
                ),
            )

            // Recurse into children
            // For the child's children, the continuation at THIS level depends on
            // whether this child is the last at its level
            val childContinuation = BooleanArray(level)
            for (i in continuation.indices) {
                if (i < childContinuation.size) {
                    childContinuation[i] = continuation[i]
                }
            }
            // This node's level continues if it's not the last child
            if (level - 1 < childContinuation.size) {
                childContinuation[level - 1] = !isLast
            }

            flatten(child.id, level + 1, childContinuation)
        }
    }

    flatten(rootNoteId, 1, BooleanArray(0))
    return result
}

/**
 * Collects all descendant event IDs of a given event from the flattened node list.
 * Used for hiding descendants when a node is collapsed.
 */
fun collectDescendantIds(
    eventId: String,
    allNodes: List<ThreadNode>,
    result: MutableSet<String>,
) {
    for (node in allNodes) {
        if (node.parentId == eventId || node.parentId in result) {
            result.add(node.event.id)
        }
    }
}
