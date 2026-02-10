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

import build.marmot.mdk.Mdk
import build.marmot.mdk.MdkUniffiException
import build.marmot.mdk.ProcessMessageResult
import build.marmot.mdk.newMdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalUnsignedTypes::class)
class MdkMarmotManager : MarmotManager {
    private var mdk: Mdk? = null

    override val isInitialized: Boolean get() = mdk != null

    override fun initialize(dbPath: String) {
        if (mdk != null) return
        try {
            java.io
                .File(dbPath)
                .parentFile
                ?.mkdirs()
            mdk = newMdk(dbPath)
        } catch (e: MdkUniffiException) {
            throw IllegalStateException("Failed to initialize Marmot: ${e.message}", e)
        }
    }

    override fun close() {
        mdk?.close()
        mdk = null
    }

    override fun clearData(dbPath: String) {
        close()
        java.io.File(dbPath).deleteRecursively()
        java.io
            .File(dbPath)
            .parentFile
            ?.mkdirs()
        initialize(dbPath)
    }

    override suspend fun createKeyPackage(
        publicKey: String,
        relays: List<String>,
    ): String =
        withContext(Dispatchers.IO) {
            val result = requireMdk().createKeyPackageForEvent(publicKey, relays)
            result.keyPackage
        }

    override suspend fun createGroup(
        creatorKey: String,
        memberKeyPackages: List<String>,
        name: String,
        description: String,
        relays: List<String>,
        admins: List<String>,
    ): MarmotCreateGroupResult =
        withContext(Dispatchers.IO) {
            val result = requireMdk().createGroup(creatorKey, memberKeyPackages, name, description, relays, admins)
            MarmotCreateGroupResult(
                group = result.group.toMarmotGroup(),
                welcomeEventJsons = result.welcomeRumorsJson,
            )
        }

    override suspend fun getGroups(): List<MarmotGroup> =
        withContext(Dispatchers.IO) {
            requireMdk().getGroups().map { it.toMarmotGroup() }
        }

    override suspend fun getGroup(groupId: String): MarmotGroup? =
        withContext(Dispatchers.IO) {
            try {
                requireMdk().getGroup(groupId)?.toMarmotGroup()
            } catch (e: MdkUniffiException) {
                null
            }
        }

    override suspend fun getMembers(groupId: String): List<String> =
        withContext(Dispatchers.IO) {
            requireMdk().getMembers(groupId)
        }

    override suspend fun addMembers(
        groupId: String,
        keyPackages: List<String>,
    ): List<String> =
        withContext(Dispatchers.IO) {
            val result = requireMdk().addMembers(groupId, keyPackages)
            result.welcomeRumorsJson ?: emptyList()
        }

    override suspend fun removeMembers(
        groupId: String,
        memberKeys: List<String>,
    ) = withContext(Dispatchers.IO) {
        requireMdk().removeMembers(groupId, memberKeys)
        Unit
    }

    override suspend fun sendMessage(
        groupId: String,
        senderKey: String,
        content: String,
        kind: Int,
        tags: List<List<String>>,
    ): String =
        withContext(Dispatchers.IO) {
            requireMdk().createMessage(groupId, senderKey, content, kind.toUShort(), tags)
        }

    override suspend fun processIncomingMessage(eventJson: String): MarmotProcessResult =
        withContext(Dispatchers.IO) {
            when (val result = requireMdk().processMessage(eventJson)) {
                is ProcessMessageResult.ApplicationMessage ->
                    MarmotProcessResult.Message(result.message.toMarmotMessage())
                is ProcessMessageResult.Commit ->
                    MarmotProcessResult.Commit(result.mlsGroupId)
                is ProcessMessageResult.Proposal ->
                    MarmotProcessResult.Proposal(
                        evolutionEventJson = result.result.evolutionEventJson,
                        welcomeEventJsons = result.result.welcomeRumorsJson ?: emptyList(),
                        mlsGroupId = result.result.mlsGroupId,
                    )
                is ProcessMessageResult.ExternalJoinProposal ->
                    MarmotProcessResult.Commit(result.mlsGroupId)
                is ProcessMessageResult.Unprocessable ->
                    MarmotProcessResult.Unprocessable(result.mlsGroupId)
                else ->
                    // Catch-all for newer variants (PendingProposal, IgnoredProposal,
                    // PreviouslyFailed) that may be added in future mdk-kotlin versions
                    MarmotProcessResult.Unprocessable("")
            }
        }

    override suspend fun getMessages(groupId: String): List<MarmotMessage> =
        withContext(Dispatchers.IO) {
            requireMdk().getMessages(groupId).map { it.toMarmotMessage() }
        }

    override suspend fun getPendingInvites(): List<MarmotInvite> =
        withContext(Dispatchers.IO) {
            requireMdk().getPendingWelcomes().map { it.toMarmotInvite() }
        }

    override suspend fun processWelcome(
        eventJson: String,
        ourPubkey: String,
    ): MarmotInvite =
        withContext(Dispatchers.IO) {
            requireMdk().processWelcome(eventJson, ourPubkey).toMarmotInvite()
        }

    override suspend fun acceptInvite(welcomeId: String) =
        withContext(Dispatchers.IO) {
            requireMdk().acceptWelcome(welcomeId)
        }

    override suspend fun declineInvite(welcomeId: String) =
        withContext(Dispatchers.IO) {
            requireMdk().declineWelcome(welcomeId)
        }

    private fun requireMdk(): Mdk = mdk ?: throw IllegalStateException("MarmotManager not initialized. Call initialize() first.")

    private fun build.marmot.mdk.Group.toMarmotGroup(): MarmotGroup =
        MarmotGroup(
            id = mlsGroupId,
            nostrGroupId = nostrGroupId,
            name = name,
            description = description,
            adminPubkeys = adminPubkeys,
            lastMessageAt =
                lastMessageAt?.toLong()?.let { ts ->
                    if (ts > 0L) ts else null
                },
            state = state,
        )

    private fun build.marmot.mdk.Message.toMarmotMessage(): MarmotMessage =
        MarmotMessage(
            id = id,
            groupId = mlsGroupId,
            nostrGroupId = nostrGroupId,
            eventId = eventId,
            senderKey = senderPubkey,
            content = eventJson,
            timestamp = processedAt.toLong(),
            kind = kind.toInt(),
            state = state,
        )

    private fun build.marmot.mdk.Welcome.toMarmotInvite(): MarmotInvite =
        MarmotInvite(
            welcomeId = id,
            groupId = mlsGroupId,
            nostrGroupId = nostrGroupId,
            groupName = groupName,
            groupDescription = groupDescription,
            inviterKey = welcomer,
            memberCount = memberCount.toInt(),
            relays = groupRelays,
            state = state,
        )
}
