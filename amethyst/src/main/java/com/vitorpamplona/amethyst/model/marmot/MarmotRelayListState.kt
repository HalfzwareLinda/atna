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
package com.vitorpamplona.amethyst.model.marmot

import com.vitorpamplona.amethyst.model.AccountSettings
import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.model.NoteState
import com.vitorpamplona.quartz.marmotMls.MarmotKeyPackageRelayListEvent
import com.vitorpamplona.quartz.nip01Core.relay.normalizer.NormalizedRelayUrl
import com.vitorpamplona.quartz.nip01Core.signers.NostrSigner
import com.vitorpamplona.quartz.utils.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MarmotRelayListState(
    val signer: NostrSigner,
    val cache: LocalCache,
    val scope: CoroutineScope,
    val settings: AccountSettings,
) {
    val marmotListNote = cache.getOrCreateAddressableNote(getMarmotRelayListAddress())

    fun getMarmotRelayListAddress() = MarmotKeyPackageRelayListEvent.createAddress(signer.pubKey)

    fun getMarmotRelayListFlow(): StateFlow<NoteState> = marmotListNote.flow().metadata.stateFlow

    fun getMarmotRelayList(): MarmotKeyPackageRelayListEvent? = marmotListNote.event as? MarmotKeyPackageRelayListEvent

    fun normalizeMarmotRelayListWithBackup(note: Note): Set<NormalizedRelayUrl> {
        val event = note.event as? MarmotKeyPackageRelayListEvent ?: settings.backupMarmotRelayList
        return event?.relays()?.toSet() ?: emptySet()
    }

    val flow =
        getMarmotRelayListFlow()
            .map { normalizeMarmotRelayListWithBackup(it.note) }
            .onStart { emit(normalizeMarmotRelayListWithBackup(marmotListNote)) }
            .flowOn(Dispatchers.IO)
            .stateIn(
                scope,
                SharingStarted.Eagerly,
                emptySet(),
            )

    suspend fun saveRelayList(relays: List<NormalizedRelayUrl>): MarmotKeyPackageRelayListEvent {
        val relayList = getMarmotRelayList()
        return if (relayList != null && relayList.tags.isNotEmpty()) {
            MarmotKeyPackageRelayListEvent.updateRelayList(
                earlierVersion = relayList,
                relays = relays,
                signer = signer,
            )
        } else {
            MarmotKeyPackageRelayListEvent.create(
                relays = relays,
                signer = signer,
            )
        }
    }

    init {
        settings.backupMarmotRelayList?.let {
            Log.d("AccountRegisterObservers", "Loading saved Marmot Relay List ${it.toJson()}")
            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch(Dispatchers.IO) {
                LocalCache.justConsumeMyOwnEvent(it)
            }
        }

        scope.launch(Dispatchers.IO) {
            Log.d("AccountRegisterObservers", "Marmot Relay List Collector Start")
            getMarmotRelayListFlow().collect {
                Log.d("AccountRegisterObservers", "Updating Marmot Relay List for ${signer.pubKey}")
                (it.note.event as? MarmotKeyPackageRelayListEvent)?.let {
                    settings.updateMarmotRelayList(it)
                }
            }
        }
    }
}
