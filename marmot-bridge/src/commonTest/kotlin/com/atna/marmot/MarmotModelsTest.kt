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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MarmotModelsTest {
    @Test
    fun marmotGroupCreation() {
        val group =
            MarmotGroup(
                id = "group1",
                name = "Test Group",
                description = "A test group",
                memberCount = 5,
                lastMessageAt = 1700000000L,
            )
        assertEquals("group1", group.id)
        assertEquals("Test Group", group.name)
        assertEquals("A test group", group.description)
        assertEquals(5, group.memberCount)
        assertEquals(1700000000L, group.lastMessageAt)
    }

    @Test
    fun marmotGroupNullLastMessage() {
        val group =
            MarmotGroup(
                id = "group2",
                name = "Empty Group",
                description = "",
                memberCount = 1,
                lastMessageAt = null,
            )
        assertNull(group.lastMessageAt)
    }

    @Test
    fun marmotMessageCreation() {
        val message =
            MarmotMessage(
                id = "msg1",
                groupId = "group1",
                senderKey = "npub1abc",
                content = "Hello, encrypted world!",
                timestamp = 1700000000L,
            )
        assertEquals("msg1", message.id)
        assertEquals("group1", message.groupId)
        assertEquals("npub1abc", message.senderKey)
        assertEquals("Hello, encrypted world!", message.content)
        assertEquals(1700000000L, message.timestamp)
    }

    @Test
    fun marmotInviteCreation() {
        val invite =
            MarmotInvite(
                groupId = "group1",
                groupName = "Test Group",
                inviterKey = "npub1xyz",
                welcomeJson = "{\"welcome\": \"data\"}",
            )
        assertEquals("group1", invite.groupId)
        assertEquals("Test Group", invite.groupName)
        assertEquals("npub1xyz", invite.inviterKey)
        assertEquals("{\"welcome\": \"data\"}", invite.welcomeJson)
    }

    @Test
    fun marmotGroupDataClassEquality() {
        val group1 = MarmotGroup("id", "name", "desc", 3, 100L)
        val group2 = MarmotGroup("id", "name", "desc", 3, 100L)
        assertEquals(group1, group2)
        assertEquals(group1.hashCode(), group2.hashCode())
    }

    @Test
    fun marmotMessageDataClassCopy() {
        val msg = MarmotMessage("id1", "group1", "sender1", "content", 100L)
        val updated = msg.copy(content = "new content", timestamp = 200L)
        assertEquals("new content", updated.content)
        assertEquals(200L, updated.timestamp)
        assertEquals("id1", updated.id)
    }
}
