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
package com.vitorpamplona.amethyst.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.atna.bugreport.CrashHandler
import com.atna.bugreport.GitHubIssueSubmitter
import com.atna.bugreport.GitHubTokenProvider
import com.atna.ndb.EventPersistenceService
import com.vitorpamplona.amethyst.commons.icons.Reply
import com.vitorpamplona.amethyst.commons.icons.Zap
import com.vitorpamplona.amethyst.commons.state.EventCollectionState
import com.vitorpamplona.amethyst.commons.ui.screens.TrustDomainPlaceholder
import com.vitorpamplona.amethyst.commons.ui.screens.WalletPlaceholder
import com.vitorpamplona.amethyst.commons.util.toShortDisplay
import com.vitorpamplona.amethyst.commons.viewmodels.SearchBarState
import com.vitorpamplona.amethyst.desktop.account.AccountManager
import com.vitorpamplona.amethyst.desktop.account.AccountState
import com.vitorpamplona.amethyst.desktop.cache.DesktopLocalCache
import com.vitorpamplona.amethyst.desktop.network.DesktopRelayConnectionManager
import com.vitorpamplona.amethyst.desktop.network.RelayStatus
import com.vitorpamplona.amethyst.desktop.subscriptions.DesktopRelaySubscriptionsCoordinator
import com.vitorpamplona.amethyst.desktop.subscriptions.FeedTab
import com.vitorpamplona.amethyst.desktop.subscriptions.createNotificationsSubscription
import com.vitorpamplona.amethyst.desktop.subscriptions.rememberSubscription
import com.vitorpamplona.amethyst.desktop.ui.BookmarksScreen
import com.vitorpamplona.amethyst.desktop.ui.ComposeNoteDialog
import com.vitorpamplona.amethyst.desktop.ui.CrashReportDialog
import com.vitorpamplona.amethyst.desktop.ui.DesktopBugReportScreen
import com.vitorpamplona.amethyst.desktop.ui.DesktopMarmotConversationScreen
import com.vitorpamplona.amethyst.desktop.ui.DesktopMarmotGroupsScreen
import com.vitorpamplona.amethyst.desktop.ui.DesktopMarmotNewChatScreen
import com.vitorpamplona.amethyst.desktop.ui.FeedScreen
import com.vitorpamplona.amethyst.desktop.ui.HashtagFeedScreen
import com.vitorpamplona.amethyst.desktop.ui.LoginScreen
import com.vitorpamplona.amethyst.desktop.ui.NotificationItem
import com.vitorpamplona.amethyst.desktop.ui.NotificationsScreen
import com.vitorpamplona.amethyst.desktop.ui.SearchScreen
import com.vitorpamplona.amethyst.desktop.ui.ThreadScreen
import com.vitorpamplona.amethyst.desktop.ui.UserProfileScreen
import com.vitorpamplona.amethyst.desktop.ui.ZapFeedback
import com.vitorpamplona.amethyst.desktop.ui.profile.ProfileInfoCard
import com.vitorpamplona.amethyst.desktop.ui.relay.RelayStatusCard
import com.vitorpamplona.quartz.nip01Core.core.hexToByteArrayOrNull
import com.vitorpamplona.quartz.nip01Core.relay.client.accessories.EventCollector
import com.vitorpamplona.quartz.nip01Core.relay.normalizer.NormalizedRelayUrl
import com.vitorpamplona.quartz.nip10Notes.TextNoteEvent
import com.vitorpamplona.quartz.nip18Reposts.GenericRepostEvent
import com.vitorpamplona.quartz.nip18Reposts.RepostEvent
import com.vitorpamplona.quartz.nip19Bech32.toNpub
import com.vitorpamplona.quartz.nip25Reactions.ReactionEvent
import com.vitorpamplona.quartz.nip47WalletConnect.Nip47WalletConnect
import com.vitorpamplona.quartz.nip57Zaps.LnZapEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path.Companion.toOkioPath

private val isMacOS = System.getProperty("os.name").lowercase().contains("mac")

/**
 * Desktop navigation state - extends AppScreen with dynamic destinations.
 */
sealed class DesktopScreen {
    data class Feed(
        val tab: FeedTab = FeedTab.NOTES,
    ) : DesktopScreen()

    object Reads : DesktopScreen()

    object Search : DesktopScreen()

    object Bookmarks : DesktopScreen()

    object Messages : DesktopScreen()

    object Notifications : DesktopScreen()

    object MyProfile : DesktopScreen()

    data class UserProfile(
        val pubKeyHex: String,
    ) : DesktopScreen()

    data class Thread(
        val noteId: String,
    ) : DesktopScreen()

    object Settings : DesktopScreen()

    object Wallet : DesktopScreen()

    object TrustDomain : DesktopScreen()

    object BugReport : DesktopScreen()

    data class MarmotNewChat(
        val prefillPubkey: String? = null,
    ) : DesktopScreen()

    data class MarmotConversation(
        val groupId: String,
        val groupName: String,
    ) : DesktopScreen()

    data class Hashtag(
        val hashtag: String,
    ) : DesktopScreen()
}

fun main() {
    val atnaDir = System.getProperty("user.home") + "/.atna"
    java.io.File(atnaDir).mkdirs()
    CrashHandler(
        crashFilePath = "$atnaDir/crash_report.json",
        appVersion = "1.0.0-dev",
        platform = "Linux ${System.getProperty("os.version")}",
        device =
            java.net.InetAddress
                .getLocalHost()
                .hostName,
    ).install()

    val pendingCrash = CrashHandler.loadPendingCrashReport("$atnaDir/crash_report.json")

    // Configure Coil image loader with disk cache for profile pictures
    val imageCacheDir = "$atnaDir/image_cache"
    java.io.File(imageCacheDir).mkdirs()
    val imageHttpClient =
        okhttp3.OkHttpClient
            .Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .connectionPool(okhttp3.ConnectionPool(5, 30, java.util.concurrent.TimeUnit.SECONDS))
            .dispatcher(okhttp3.Dispatcher().apply { maxRequestsPerHost = 10 })
            .build()
    coil3.SingletonImageLoader.setSafe {
        coil3.ImageLoader
            .Builder(it)
            .components {
                add(
                    coil3.network.okhttp.OkHttpNetworkFetcherFactory(
                        callFactory = { imageHttpClient },
                    ),
                )
            }.diskCache {
                coil3.disk.DiskCache
                    .Builder()
                    .directory(java.io.File(imageCacheDir).toOkioPath())
                    .maxSizeBytes(256L * 1024 * 1024) // 256 MB
                    .build()
            }.memoryCache {
                coil3.memory.MemoryCache
                    .Builder()
                    .maxSizeBytes(64L * 1024 * 1024) // 64 MB
                    .build()
            }.build()
    }

    application {
        val windowState =
            rememberWindowState(
                placement = WindowPlacement.Maximized,
            )
        var showComposeDialog by remember { mutableStateOf(false) }
        var replyToNote by remember { mutableStateOf<com.vitorpamplona.quartz.nip01Core.core.Event?>(null) }
        var quoteNote by remember { mutableStateOf<com.vitorpamplona.quartz.nip01Core.core.Event?>(null) }
        var currentScreen by remember { mutableStateOf<DesktopScreen>(DesktopScreen.Feed()) }
        var crashReport by remember { mutableStateOf(pendingCrash) }
        var uiScale by remember { mutableStateOf(DesktopPreferences.uiScale) }

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "ATNA",
        ) {
            MenuBar {
                Menu("File") {
                    Item(
                        "New Note",
                        shortcut =
                            if (isMacOS) {
                                KeyShortcut(Key.N, meta = true)
                            } else {
                                KeyShortcut(Key.N, ctrl = true)
                            },
                        onClick = { showComposeDialog = true },
                    )
                    Separator()
                    Item(
                        "Settings",
                        shortcut =
                            if (isMacOS) {
                                KeyShortcut(Key.Comma, meta = true)
                            } else {
                                KeyShortcut(Key.Comma, ctrl = true)
                            },
                        onClick = { currentScreen = DesktopScreen.Settings },
                    )
                    Separator()
                    Item(
                        "Quit",
                        shortcut =
                            if (isMacOS) {
                                KeyShortcut(Key.Q, meta = true)
                            } else {
                                KeyShortcut(Key.Q, ctrl = true)
                            },
                        onClick = ::exitApplication,
                    )
                }
                Menu("Edit") {
                    Item(
                        "Copy",
                        shortcut =
                            if (isMacOS) {
                                KeyShortcut(Key.C, meta = true)
                            } else {
                                KeyShortcut(Key.C, ctrl = true)
                            },
                        onClick = { },
                    )
                    Item(
                        "Paste",
                        shortcut =
                            if (isMacOS) {
                                KeyShortcut(Key.V, meta = true)
                            } else {
                                KeyShortcut(Key.V, ctrl = true)
                            },
                        onClick = { },
                    )
                }
                Menu("View") {
                    Item("Feed", onClick = { })
                    Item("Messages", onClick = { })
                    Item("Notifications", onClick = { })
                    Separator()
                    Item(
                        "Zoom In",
                        shortcut =
                            if (isMacOS) {
                                KeyShortcut(Key.Equals, meta = true)
                            } else {
                                KeyShortcut(Key.Equals, ctrl = true)
                            },
                        onClick = {
                            uiScale = (uiScale + 0.125f).coerceAtMost(2.5f)
                            DesktopPreferences.uiScale = uiScale
                        },
                    )
                    Item(
                        "Zoom Out",
                        shortcut =
                            if (isMacOS) {
                                KeyShortcut(Key.Minus, meta = true)
                            } else {
                                KeyShortcut(Key.Minus, ctrl = true)
                            },
                        onClick = {
                            uiScale = (uiScale - 0.125f).coerceAtLeast(0.75f)
                            DesktopPreferences.uiScale = uiScale
                        },
                    )
                    Item(
                        "Reset Zoom",
                        shortcut =
                            if (isMacOS) {
                                KeyShortcut(Key.Zero, meta = true)
                            } else {
                                KeyShortcut(Key.Zero, ctrl = true)
                            },
                        onClick = {
                            uiScale = 1.5f
                            DesktopPreferences.uiScale = uiScale
                        },
                    )
                }
                Menu("Help") {
                    Item("About ATNA", onClick = { })
                    Item("Keyboard Shortcuts", onClick = { })
                }
            }

            // Apply UI scale by overriding LocalDensity
            val currentDensity = LocalDensity.current
            val scaledDensity =
                remember(uiScale, currentDensity) {
                    Density(
                        density = currentDensity.density * uiScale,
                        fontScale = currentDensity.fontScale * uiScale,
                    )
                }
            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                App(
                    currentScreen = currentScreen,
                    onScreenChange = { currentScreen = it },
                    showComposeDialog = showComposeDialog,
                    onShowComposeDialog = { showComposeDialog = true },
                    onShowReplyDialog = { event ->
                        replyToNote = event
                        showComposeDialog = true
                    },
                    onShowQuoteDialog = { event ->
                        quoteNote = event
                        showComposeDialog = true
                    },
                    onDismissComposeDialog = {
                        showComposeDialog = false
                        replyToNote = null
                        quoteNote = null
                    },
                    replyToNote = replyToNote,
                    quoteNote = quoteNote,
                    uiScale = uiScale,
                    onUiScaleChange = { newScale ->
                        uiScale = newScale
                        DesktopPreferences.uiScale = newScale
                    },
                )
            }

            crashReport?.let { report ->
                val scope = rememberCoroutineScope()
                CrashReportDialog(
                    crashReport = report,
                    onSubmit = { submittedReport ->
                        val token = GitHubTokenProvider.resolveToken()
                        if (token != null) {
                            scope.launch(Dispatchers.IO) {
                                GitHubIssueSubmitter(
                                    repoOwner = "HalfzwareLinda",
                                    repoName = "atna",
                                    token = token,
                                ).submit(submittedReport)
                            }
                        }
                        CrashHandler.clearPendingCrashReport("$atnaDir/crash_report.json")
                        crashReport = null
                    },
                    onDismiss = {
                        CrashHandler.clearPendingCrashReport("$atnaDir/crash_report.json")
                        crashReport = null
                    },
                )
            }
        }
    }
}

@Composable
fun App(
    currentScreen: DesktopScreen,
    onScreenChange: (DesktopScreen) -> Unit,
    showComposeDialog: Boolean,
    onShowComposeDialog: () -> Unit,
    onShowReplyDialog: (com.vitorpamplona.quartz.nip01Core.core.Event) -> Unit,
    onShowQuoteDialog: (com.vitorpamplona.quartz.nip01Core.core.Event) -> Unit,
    onDismissComposeDialog: () -> Unit,
    replyToNote: com.vitorpamplona.quartz.nip01Core.core.Event?,
    quoteNote: com.vitorpamplona.quartz.nip01Core.core.Event? = null,
    uiScale: Float = 1.5f,
    onUiScaleChange: (Float) -> Unit = {},
) {
    val relayManager = remember { DesktopRelayConnectionManager() }
    val localCache = remember { DesktopLocalCache() }
    val accountManager = remember { AccountManager.create() }
    val accountState by accountManager.accountState.collectAsState()
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

    // LMDB event persistence
    val eventPersistenceService = remember { EventPersistenceService(CoroutineScope(SupervisorJob() + Dispatchers.IO), maxSizeMB = 4096) }

    // Marmot encrypted group DMs (real mdk-uniffi native library on desktop)
    val marmotManager = remember { com.atna.marmot.MdkMarmotManagerJvm() as com.atna.marmot.MarmotManager }
    val marmotRouter = remember { com.atna.marmot.MarmotEventRouter(marmotManager, CoroutineScope(SupervisorJob() + Dispatchers.IO)) }

    // Subscriptions coordinator for metadata/reactions loading
    val subscriptionsCoordinator =
        remember(relayManager, localCache) {
            DesktopRelaySubscriptionsCoordinator(
                client = relayManager.client,
                scope = scope,
                indexRelaysProvider = { relayManager.availableRelays.value },
                localCache = localCache,
            )
        }

    // NIP-42 relay authentication
    val authCoordinator =
        remember(relayManager) {
            com.vitorpamplona.amethyst.desktop.network.DesktopAuthCoordinator(
                client = relayManager.client,
                scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
                getSigners = { listOfNotNull(accountManager.currentAccount()?.signer) },
                onAuthSuccess = { relay -> relayManager.markRelayAuthenticated(relay) },
            )
        }

    // Memory manager: periodic cache stats logging + heap monitoring
    val memoryManager =
        remember(localCache) {
            com.vitorpamplona.amethyst.desktop.cache
                .DesktopMemoryManager(localCache, scope)
        }

    // NIP-65 outbox model resolver
    val outboxResolver =
        remember(localCache) {
            com.vitorpamplona.amethyst.desktop.network
                .DesktopOutboxResolver(localCache)
        }

    // Try to load saved account on startup
    DisposableEffect(Unit) {
        val appStartMs = System.currentTimeMillis()
        println("[Startup] DisposableEffect entered at ${java.time.LocalTime.now()}")

        // Start relay connections FIRST — they don't depend on account or LMDB
        // and OkHttp WebSocket handshakes happen asynchronously in background
        println("[Startup] Adding default relays...")
        relayManager.addDefaultRelays()
        println("[Startup] Calling connect()...")
        relayManager.connect()

        // Start subscriptions coordinator
        println("[Startup] Starting subscriptions coordinator...")
        subscriptionsCoordinator.start()

        // Start memory monitoring
        memoryManager.start()

        // Barrier: signals when account has been loaded (or failed)
        val accountLoaded = CompletableDeferred<Unit>()

        scope.launch(Dispatchers.IO) {
            // Load account on IO dispatcher to avoid blocking UI with password prompt (readLine)
            println("[Startup] Loading saved account...")
            val result = accountManager.loadSavedAccount()
            result.fold(
                onSuccess = { state ->
                    println("[Startup] Account loaded successfully: ${state.npub.take(16)}... in ${System.currentTimeMillis() - appStartMs}ms")
                },
                onFailure = { error ->
                    println("[Startup] Auto-login failed: ${error.message}")
                    System.err.println("[Startup] User will see login screen. Reason: ${error.message}")
                },
            )
            accountLoaded.complete(Unit)
        }

        // Barrier: signals when LMDB rehydration is complete so the relay list
        // collector can safely invalidate the outbox cache without racing
        val rehydrationComplete = CompletableDeferred<Unit>()

        // Start LMDB event persistence on IO thread and rehydrate from persisted events
        scope.launch(Dispatchers.IO) {
            try {
                val dbPath = System.getProperty("user.home") + "/.atna/nostrdb"
                println("[LMDB] Starting persistence at $dbPath...")
                eventPersistenceService.start(dbPath)

                // Wait for account to load so we can detect account switches
                accountLoaded.await()

                // Account-switch detection: wipe caches if a different user logged in
                val currentPubkey = accountManager.currentAccount()?.pubKeyHex
                val previousPubkey = DesktopPreferences.lastLoggedInPubkey
                if (currentPubkey != null && previousPubkey != null && currentPubkey != previousPubkey) {
                    println("[LMDB] Account switch detected ($previousPubkey → $currentPubkey), wiping caches...")
                    eventPersistenceService.wipeAndRestart()
                    localCache.clear()
                    val marmotPath = System.getProperty("user.home") + "/.atna/marmot"
                    marmotRouter.clearAndReinitialize(marmotPath)
                    println("[LMDB] Caches wiped for new account")
                }
                // Persist the current pubkey for next startup comparison
                if (currentPubkey != null) {
                    DesktopPreferences.lastLoggedInPubkey = currentPubkey
                }

                // Phase 0: Load relay lists first (required for outbox routing)
                val relayLists =
                    eventPersistenceService.loadEvents(
                        com.vitorpamplona.quartz.nip01Core.relay.filters
                            .Filter(kinds = listOf(10002, 10050, 10051), limit = 5000),
                    )
                relayLists.forEach { event ->
                    if (event is com.vitorpamplona.quartz.nip65RelayList.AdvertisedRelayListEvent) {
                        localCache.consumeRelayList(event)
                    }
                }

                val oneDayAgo =
                    com.vitorpamplona.quartz.utils.TimeUtils
                        .now() - 86400
                val oneWeekAgo =
                    com.vitorpamplona.quartz.utils.TimeUtils
                        .now() - 7 * 86400

                // All phases run in parallel — they are independent of each other
                // Phase 1: Profiles (kind 0) + user status (kind 30315)
                val profilesJob =
                    launch {
                        val profiles =
                            eventPersistenceService.loadEvents(
                                com.vitorpamplona.quartz.nip01Core.relay.filters
                                    .Filter(kinds = listOf(0, 30315), limit = 10000),
                            )
                        profiles.forEach { event ->
                            if (event is com.vitorpamplona.quartz.nip01Core.metadata.MetadataEvent) {
                                localCache.consumeMetadata(event)
                            } else {
                                val author = localCache.getOrCreateUser(event.pubKey)
                                val note = localCache.getOrCreateNote(event.id)
                                note.loadEvent(event, author, emptyList())
                            }
                        }
                    }

                // Phase 2: Contact lists (kind 3) — follow graph
                val contactsJob =
                    launch {
                        val contacts =
                            eventPersistenceService.loadEvents(
                                com.vitorpamplona.quartz.nip01Core.relay.filters
                                    .Filter(kinds = listOf(3), limit = 5000),
                            )
                        contacts.forEach { event ->
                            val author = localCache.getOrCreateUser(event.pubKey)
                            val note = localCache.getOrCreateNote(event.id)
                            note.loadEvent(event, author, emptyList())
                        }
                    }

                // Phase 3: Recent notes (kind 1) + highlights (kind 9802)
                val notesJob =
                    launch {
                        val notes =
                            eventPersistenceService.loadEvents(
                                com.vitorpamplona.quartz.nip01Core.relay.filters
                                    .Filter(kinds = listOf(1, 9802), since = oneDayAgo, limit = 3000),
                            )
                        notes.forEach { event ->
                            val author = localCache.getOrCreateUser(event.pubKey)
                            val note = localCache.getOrCreateNote(event.id)
                            note.loadEvent(event, author, emptyList())
                        }
                    }

                // Phase 4: Trust assertions (kinds 10040, 30382)
                val trustJob =
                    launch {
                        val trustEvents =
                            eventPersistenceService.loadEvents(
                                com.vitorpamplona.quartz.nip01Core.relay.filters
                                    .Filter(kinds = listOf(10040, 30382), limit = 2000),
                            )
                        trustEvents.forEach { event ->
                            val author = localCache.getOrCreateUser(event.pubKey)
                            val note = localCache.getOrCreateNote(event.id)
                            note.loadEvent(event, author, emptyList())
                        }
                    }

                // Phase 5: DMs and gift wraps (kinds 4, 14, 1059) — conversation history
                val dmsJob =
                    launch {
                        val dms =
                            eventPersistenceService.loadEvents(
                                com.vitorpamplona.quartz.nip01Core.relay.filters
                                    .Filter(kinds = listOf(4, 14, 1059), since = oneDayAgo, limit = 1000),
                            )
                        dms.forEach { event ->
                            val author = localCache.getOrCreateUser(event.pubKey)
                            val note = localCache.getOrCreateNote(event.id)
                            note.loadEvent(event, author, emptyList())
                        }
                    }

                // Phase 6: Engagement metrics (kinds 6, 7, 16, 9735) — reactions, reposts, zaps
                val engagementJob =
                    launch {
                        val engagement =
                            eventPersistenceService.loadEvents(
                                com.vitorpamplona.quartz.nip01Core.relay.filters
                                    .Filter(kinds = listOf(6, 7, 16, 9735), since = oneDayAgo, limit = 5000),
                            )
                        engagement.forEach { event ->
                            val author = localCache.getOrCreateUser(event.pubKey)
                            val note = localCache.getOrCreateNote(event.id)
                            note.loadEvent(event, author, emptyList())
                        }
                    }

                // Phase 7: Long-form content (kind 30023) — Reads screen
                val longFormJob =
                    launch {
                        val articles =
                            eventPersistenceService.loadEvents(
                                com.vitorpamplona.quartz.nip01Core.relay.filters
                                    .Filter(kinds = listOf(30023), since = oneWeekAgo, limit = 1000),
                            )
                        articles.forEach { event ->
                            val author = localCache.getOrCreateUser(event.pubKey)
                            val note = localCache.getOrCreateNote(event.id)
                            note.loadEvent(event, author, emptyList())
                        }
                    }

                // Phase 8: Bookmarks (kind 30001) — Bookmarks screen
                val bookmarksJob =
                    launch {
                        val bookmarks =
                            eventPersistenceService.loadEvents(
                                com.vitorpamplona.quartz.nip01Core.relay.filters
                                    .Filter(kinds = listOf(30001), limit = 500),
                            )
                        bookmarks.forEach { event ->
                            val author = localCache.getOrCreateUser(event.pubKey)
                            val note = localCache.getOrCreateNote(event.id)
                            note.loadEvent(event, author, emptyList())
                        }
                    }

                // Phase 9: Marmot group events (kinds 443-445) — encrypted group DM state
                val marmotJob =
                    launch {
                        val marmotEvents =
                            eventPersistenceService.loadEvents(
                                com.vitorpamplona.quartz.nip01Core.relay.filters
                                    .Filter(kinds = listOf(443, 444, 445), since = oneWeekAgo, limit = 2000),
                            )
                        marmotEvents.forEach { event ->
                            val author = localCache.getOrCreateUser(event.pubKey)
                            val note = localCache.getOrCreateNote(event.id)
                            note.loadEvent(event, author, emptyList())
                        }
                    }

                // Phase 10: Media events (NIP-68 pictures, NIP-71 videos, NIP-94 file metadata)
                val mediaJob =
                    launch {
                        val media =
                            eventPersistenceService.loadEvents(
                                com.vitorpamplona.quartz.nip01Core.relay.filters
                                    .Filter(kinds = listOf(20, 21, 22, 34235, 34236, 1063), since = oneWeekAgo, limit = 500),
                            )
                        media.forEach { event ->
                            val author = localCache.getOrCreateUser(event.pubKey)
                            val note = localCache.getOrCreateNote(event.id)
                            note.loadEvent(event, author, emptyList())
                        }
                    }

                // Phase 11: Live activities (NIP-53, kind 30311) + live chat (kind 1311)
                val liveJob =
                    launch {
                        val live =
                            eventPersistenceService.loadEvents(
                                com.vitorpamplona.quartz.nip01Core.relay.filters
                                    .Filter(kinds = listOf(1311, 30311), since = oneDayAgo, limit = 200),
                            )
                        live.forEach { event ->
                            val author = localCache.getOrCreateUser(event.pubKey)
                            val note = localCache.getOrCreateNote(event.id)
                            note.loadEvent(event, author, emptyList())
                        }
                    }

                // Wait for all parallel phases
                profilesJob.join()
                contactsJob.join()
                notesJob.join()
                trustJob.join()
                dmsJob.join()
                engagementJob.join()
                longFormJob.join()
                bookmarksJob.join()
                marmotJob.join()
                mediaJob.join()
                liveJob.join()
            } finally {
                println("[LMDB] Rehydration complete in ${System.currentTimeMillis() - appStartMs}ms")
                rehydrationComplete.complete(Unit)
                outboxResolver.invalidateCache()
            }
        }

        // Initialize Marmot encrypted group DMs
        scope.launch(Dispatchers.IO) {
            try {
                val marmotPath = System.getProperty("user.home") + "/.atna/marmot"
                println("[Marmot] Initializing at $marmotPath...")
                val startMs = System.currentTimeMillis()
                marmotManager.initialize(marmotPath)
                val elapsed = System.currentTimeMillis() - startMs
                println("[Marmot] Initialized in ${elapsed}ms")
                marmotRouter.refreshGroups()
                marmotRouter.refreshInvites()
                println("[Marmot] Groups and invites refreshed")
            } catch (e: Throwable) {
                val msg = "Marmot init failed: ${e.message}"
                System.err.println("[Marmot] $msg")
                e.printStackTrace()
                marmotRouter.setInitFailed(msg)
            }
        }

        val persistingCollector =
            EventCollector(relayManager.client) { event, _ ->
                eventPersistenceService.persistEvent(event)
            }

        // Relay list + hint collector: processes incoming events for outbox model.
        // Waits for rehydration to complete before invalidating outbox cache to
        // prevent race conditions between LMDB loading and live relay events.
        val relayListCollector =
            EventCollector(relayManager.client) { event, relay ->
                // Record relay hints for all events (safe without barrier)
                localCache.recordRelayHint(event, relay.url)
                // Consume relay list events into local cache
                if (event is com.vitorpamplona.quartz.nip65RelayList.AdvertisedRelayListEvent) {
                    localCache.consumeRelayList(event)
                    // Only invalidate outbox cache after rehydration is done
                    if (rehydrationComplete.isCompleted) {
                        outboxResolver.invalidateCache()
                    }
                }
            }

        // Marmot event collector: routes incoming Marmot events to the router
        val marmotCollector =
            EventCollector(relayManager.client) { event, _ ->
                when (event) {
                    is com.vitorpamplona.quartz.marmotMls.MarmotGroupEvent,
                    is com.vitorpamplona.quartz.marmotMls.MarmotWelcomeEvent,
                    is com.vitorpamplona.quartz.marmotMls.MarmotKeyPackageEvent,
                    is com.vitorpamplona.quartz.marmotMls.MarmotKeyPackageRelayListEvent,
                    -> marmotRouter.onMarmotEvent(event)
                }
            }

        // Monitor relay connections for debugging
        scope.launch {
            kotlinx.coroutines.delay(3000)
            val connected = relayManager.connectedRelays.value
            val available = relayManager.availableRelays.value
            val statuses = relayManager.relayStatuses.value
            println("[Startup] After 3s: ${connected.size}/${available.size} relays connected")
            statuses.forEach { (url, status) ->
                val state =
                    when {
                        status.connected -> "CONNECTED (${status.pingMs}ms)"
                        status.error != null -> "ERROR: ${status.error}"
                        else -> "CONNECTING..."
                    }
                println("[Startup]   $url → $state")
            }
        }

        onDispose {
            memoryManager.stop()
            authCoordinator.destroy()
            marmotCollector.destroy()
            relayListCollector.destroy()
            persistingCollector.destroy()
            eventPersistenceService.stop()
            marmotManager.close()
            subscriptionsCoordinator.clear()
            relayManager.disconnect()
        }
    }

    MaterialTheme(
        colorScheme =
            darkColorScheme(
                primary = Color(0xFF00E5CC),
                secondary = Color(0xFF9370DB),
                tertiary = Color(0xFF9370DB),
                background = Color(0xFF0D0D1A),
                surface = Color(0xFF121228),
                surfaceDim = Color(0xFF0D0D1A),
                surfaceVariant = Color(0xFF1A1A2E),
            ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            when (accountState) {
                is AccountState.LoggedOut -> {
                    LoginScreen(
                        accountManager = accountManager,
                    )
                }
                is AccountState.LoggedIn -> {
                    val account = accountState as AccountState.LoggedIn
                    val nwcConnection by accountManager.nwcConnection.collectAsState()

                    // Load NWC connection on first composition
                    LaunchedEffect(Unit) {
                        accountManager.loadNwcConnection()
                    }

                    // Broadcast initial relay events for newly created accounts
                    LaunchedEffect(account.pubKeyHex) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            accountManager.broadcastInitialRelayEvents(relayManager)
                        }
                    }

                    // Re-initialize Marmot if it was closed (e.g. after logout/re-login)
                    LaunchedEffect(account.pubKeyHex) {
                        if (!marmotManager.isInitialized) {
                            marmotRouter.clearInitError()
                            kotlinx.coroutines.withContext(Dispatchers.IO) {
                                try {
                                    val marmotPath = System.getProperty("user.home") + "/.atna/marmot"
                                    println("[Marmot] Re-initializing after login at $marmotPath...")
                                    marmotManager.initialize(marmotPath)
                                    println("[Marmot] Re-initialized successfully")
                                    marmotRouter.refreshGroups()
                                    marmotRouter.refreshInvites()
                                } catch (e: Throwable) {
                                    val msg = "Marmot re-init failed: ${e.message}"
                                    System.err.println("[Marmot] $msg")
                                    e.printStackTrace()
                                    marmotRouter.setInitFailed(msg)
                                }
                            }
                        }
                    }

                    // Wire the current user's pubkey into MarmotEventRouter for own-message detection
                    LaunchedEffect(account.pubKeyHex) {
                        marmotRouter.setCurrentUserPubkey(account.pubKeyHex)
                    }

                    // Subscribe to trust provider list (NIP-85) for logged-in user
                    LaunchedEffect(account.pubKeyHex) {
                        subscriptionsCoordinator.loadTrustProviderList(account.pubKeyHex)
                    }

                    // Subscribe to NIP-65 relay lists (kind 10002) for the logged-in user
                    // and any users we interact with. The relay list collector handles
                    // incoming relay list events automatically.
                    LaunchedEffect(account.pubKeyHex) {
                        subscriptionsCoordinator.loadRelayLists(listOf(account.pubKeyHex))
                    }

                    // Add the user's published NIP-65 relays to the connection manager
                    // so we actually connect to the relays they've chosen
                    LaunchedEffect(account.pubKeyHex) {
                        val user = localCache.getUserIfExists(account.pubKeyHex)
                        val relayList = user?.authorRelayList()
                        relayList?.relays()?.forEach { info ->
                            relayManager.addRelay(info.relayUrl.url)
                        }
                    }

                    MainContent(
                        currentScreen = currentScreen,
                        onScreenChange = onScreenChange,
                        relayManager = relayManager,
                        localCache = localCache,
                        accountManager = accountManager,
                        account = account,
                        nwcConnection = nwcConnection,
                        subscriptionsCoordinator = subscriptionsCoordinator,
                        outboxResolver = outboxResolver,
                        marmotRouter = marmotRouter,
                        eventPersistenceService = eventPersistenceService,
                        onShowComposeDialog = onShowComposeDialog,
                        onShowReplyDialog = onShowReplyDialog,
                        onShowQuoteDialog = onShowQuoteDialog,
                        onLogout = {
                            scope.launch(Dispatchers.IO) {
                                relayManager.disconnect()
                                relayManager.clearRelays()
                                subscriptionsCoordinator.clear()
                                eventPersistenceService.stop()
                                marmotManager.close()
                                DesktopPreferences.lastLoggedInPubkey = null
                                accountManager.logout()
                            }
                        },
                        uiScale = uiScale,
                        onUiScaleChange = onUiScaleChange,
                    )

                    // Compose dialog
                    if (showComposeDialog) {
                        ComposeNoteDialog(
                            onDismiss = onDismissComposeDialog,
                            relayManager = relayManager,
                            account = account,
                            replyTo = replyToNote,
                            quotedEvent = quoteNote,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainContent(
    currentScreen: DesktopScreen,
    onScreenChange: (DesktopScreen) -> Unit,
    relayManager: DesktopRelayConnectionManager,
    localCache: DesktopLocalCache,
    accountManager: AccountManager,
    account: AccountState.LoggedIn,
    nwcConnection: Nip47WalletConnect.Nip47URINorm?,
    subscriptionsCoordinator: DesktopRelaySubscriptionsCoordinator,
    outboxResolver: com.vitorpamplona.amethyst.desktop.network.DesktopOutboxResolver,
    marmotRouter: com.atna.marmot.MarmotEventRouter,
    eventPersistenceService: com.atna.ndb.EventPersistenceService,
    onShowComposeDialog: () -> Unit,
    onShowReplyDialog: (com.vitorpamplona.quartz.nip01Core.core.Event) -> Unit,
    onShowQuoteDialog: (com.vitorpamplona.quartz.nip01Core.core.Event) -> Unit,
    onLogout: () -> Unit,
    uiScale: Float = 1.5f,
    onUiScaleChange: (Float) -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val connectedRelays by relayManager.connectedRelays.collectAsState()

    // Shared search state between top bar and SearchScreen
    val searchState = remember { SearchBarState(localCache, scope) }

    val onZapFeedback: (ZapFeedback) -> Unit = { feedback ->
        scope.launch {
            val message =
                when (feedback) {
                    is ZapFeedback.Success -> "Zapped ${feedback.amountSats} sats"
                    is ZapFeedback.ExternalWallet -> "Invoice sent to wallet (${feedback.amountSats} sats)"
                    is ZapFeedback.Error -> "Zap failed: ${feedback.message}"
                    is ZapFeedback.Timeout -> "Zap timed out"
                    is ZapFeedback.NoLightningAddress -> "User has no lightning address"
                }
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(
        Modifier.fillMaxSize().drawBehind {
            // Dark gradient background that shows through transparent sidebars
            drawRect(
                brush =
                    Brush.linearGradient(
                        colors =
                            listOf(
                                Color(0xFF0A0A1A),
                                Color(0xFF0D0D2A),
                                Color(0xFF121235),
                                Color(0xFF0D0D2A),
                                Color(0xFF0A0A1A),
                            ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height),
                    ),
            )
            // Subtle radial glow in center-top area
            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors =
                            listOf(
                                Color(0x15663399),
                                Color(0x08442266),
                                Color.Transparent,
                            ),
                        center = Offset(size.width * 0.5f, size.height * 0.2f),
                        radius = size.width * 0.6f,
                    ),
            )
        },
    ) {
        // Three-column layout spanning the full window height
        Row(Modifier.fillMaxSize()) {
            // ---- Left column: logo + nav (weight ~0.16) ----
            Column(modifier = Modifier.weight(0.16f).fillMaxHeight()) {
                AtnaLeftSidebar(
                    currentScreen = currentScreen,
                    onScreenChange = onScreenChange,
                )
            }

            VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // ---- Center column: search bar + content (weight ~0.58) ----
            Column(
                modifier = Modifier.weight(0.58f).fillMaxHeight(),
            ) {
                // Search bar header (aligned with center column)
                AtnaSearchBar(searchState = searchState, onScreenChange = onScreenChange)

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                )

                // Content area
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    when (currentScreen) {
                        is DesktopScreen.Feed ->
                            FeedScreen(
                                tab = currentScreen.tab,
                                relayManager = relayManager,
                                localCache = localCache,
                                account = account,
                                nwcConnection = nwcConnection,
                                subscriptionsCoordinator = subscriptionsCoordinator,
                                outboxResolver = outboxResolver,
                                onNavigateToProfile = { pubKeyHex ->
                                    onScreenChange(DesktopScreen.UserProfile(pubKeyHex))
                                },
                                onNavigateToThread = { noteId ->
                                    onScreenChange(DesktopScreen.Thread(noteId))
                                },
                                onNavigateToHashtag = { tag ->
                                    onScreenChange(DesktopScreen.Hashtag(tag))
                                },
                                onZapFeedback = onZapFeedback,
                            )
                        DesktopScreen.Reads -> {
                            // Redirect to Articles feed tab
                            LaunchedEffect(Unit) {
                                onScreenChange(DesktopScreen.Feed(FeedTab.ARTICLES))
                            }
                        }
                        DesktopScreen.Search ->
                            SearchScreen(
                                localCache = localCache,
                                relayManager = relayManager,
                                subscriptionsCoordinator = subscriptionsCoordinator,
                                onNavigateToProfile = { pubKeyHex ->
                                    onScreenChange(DesktopScreen.UserProfile(pubKeyHex))
                                },
                                onNavigateToThread = { noteId ->
                                    onScreenChange(DesktopScreen.Thread(noteId))
                                },
                                onNavigateToHashtag = { tag ->
                                    onScreenChange(DesktopScreen.Hashtag(tag))
                                },
                                searchState = searchState,
                            )
                        DesktopScreen.Bookmarks ->
                            BookmarksScreen(
                                relayManager = relayManager,
                                localCache = localCache,
                                account = account,
                                nwcConnection = nwcConnection,
                                subscriptionsCoordinator = subscriptionsCoordinator,
                                onNavigateToProfile = { pubKeyHex ->
                                    onScreenChange(DesktopScreen.UserProfile(pubKeyHex))
                                },
                                onNavigateToThread = { noteId ->
                                    onScreenChange(DesktopScreen.Thread(noteId))
                                },
                                onZapFeedback = onZapFeedback,
                                onQuote = onShowQuoteDialog,
                            )
                        DesktopScreen.Messages ->
                            DesktopMarmotGroupsScreen(
                                marmotRouter = marmotRouter,
                                onNewChat = { onScreenChange(DesktopScreen.MarmotNewChat()) },
                                onOpenConversation = { groupId, groupName ->
                                    onScreenChange(DesktopScreen.MarmotConversation(groupId, groupName))
                                },
                            )
                        DesktopScreen.Wallet -> WalletPlaceholder()
                        DesktopScreen.TrustDomain -> TrustDomainPlaceholder()
                        DesktopScreen.Notifications ->
                            NotificationsScreen(
                                relayManager,
                                localCache,
                                onNavigateToThread = { noteId ->
                                    onScreenChange(DesktopScreen.Thread(noteId))
                                },
                                account,
                                subscriptionsCoordinator,
                            )
                        DesktopScreen.MyProfile ->
                            UserProfileScreen(
                                pubKeyHex = account.pubKeyHex,
                                relayManager = relayManager,
                                localCache = localCache,
                                account = account,
                                nwcConnection = nwcConnection,
                                subscriptionsCoordinator = subscriptionsCoordinator,
                                onBack = { onScreenChange(DesktopScreen.Feed()) },
                                onCompose = onShowComposeDialog,
                                onNavigateToProfile = { pubKeyHex ->
                                    onScreenChange(DesktopScreen.UserProfile(pubKeyHex))
                                },
                                onMarmotMessage = { pubKeyHex ->
                                    onScreenChange(DesktopScreen.MarmotNewChat(prefillPubkey = pubKeyHex))
                                },
                                onZapFeedback = onZapFeedback,
                                onQuote = onShowQuoteDialog,
                            )
                        is DesktopScreen.UserProfile ->
                            UserProfileScreen(
                                pubKeyHex = currentScreen.pubKeyHex,
                                relayManager = relayManager,
                                localCache = localCache,
                                account = account,
                                nwcConnection = nwcConnection,
                                subscriptionsCoordinator = subscriptionsCoordinator,
                                onBack = { onScreenChange(DesktopScreen.Feed()) },
                                onCompose = onShowComposeDialog,
                                onNavigateToProfile = { pubKeyHex ->
                                    onScreenChange(DesktopScreen.UserProfile(pubKeyHex))
                                },
                                onMarmotMessage = { pubKeyHex ->
                                    onScreenChange(DesktopScreen.MarmotNewChat(prefillPubkey = pubKeyHex))
                                },
                                onZapFeedback = onZapFeedback,
                                onQuote = onShowQuoteDialog,
                            )
                        is DesktopScreen.Thread ->
                            ThreadScreen(
                                noteId = currentScreen.noteId,
                                relayManager = relayManager,
                                localCache = localCache,
                                account = account,
                                nwcConnection = nwcConnection,
                                subscriptionsCoordinator = subscriptionsCoordinator,
                                onBack = { onScreenChange(DesktopScreen.Feed()) },
                                onNavigateToProfile = { pubKeyHex ->
                                    onScreenChange(DesktopScreen.UserProfile(pubKeyHex))
                                },
                                onNavigateToThread = { noteId ->
                                    onScreenChange(DesktopScreen.Thread(noteId))
                                },
                                onZapFeedback = onZapFeedback,
                                onReply = onShowReplyDialog,
                                onQuote = onShowQuoteDialog,
                            )
                        is DesktopScreen.MarmotNewChat ->
                            DesktopMarmotNewChatScreen(
                                prefillPubkey = currentScreen.prefillPubkey,
                                relayManager = relayManager,
                                localCache = localCache,
                                account = account,
                                marmotRouter = marmotRouter,
                                onBack = { onScreenChange(DesktopScreen.Messages) },
                                onNavigateToConversation = { groupId, groupName ->
                                    onScreenChange(DesktopScreen.MarmotConversation(groupId, groupName))
                                },
                            )
                        is DesktopScreen.MarmotConversation ->
                            DesktopMarmotConversationScreen(
                                groupId = currentScreen.groupId,
                                groupName = currentScreen.groupName,
                                marmotRouter = marmotRouter,
                                relayManager = relayManager,
                                account = account,
                                onBack = { onScreenChange(DesktopScreen.Messages) },
                            )
                        is DesktopScreen.Hashtag ->
                            HashtagFeedScreen(
                                hashtag = currentScreen.hashtag,
                                relayManager = relayManager,
                                localCache = localCache,
                                account = account,
                                onNavigateToProfile = { pubKeyHex ->
                                    onScreenChange(DesktopScreen.UserProfile(pubKeyHex))
                                },
                                onNavigateToThread = { noteId ->
                                    onScreenChange(DesktopScreen.Thread(noteId))
                                },
                                onNavigateToHashtag = { tag ->
                                    onScreenChange(DesktopScreen.Hashtag(tag))
                                },
                                onBack = { onScreenChange(DesktopScreen.Feed()) },
                            )
                        DesktopScreen.Settings -> RelaySettingsScreen(relayManager, account, accountManager, onLogout, eventPersistenceService, marmotRouter, uiScale, onUiScaleChange)
                        DesktopScreen.BugReport -> DesktopBugReportScreen()
                    }
                }
            }

            VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // ---- Right column: status/icons header + panels (weight ~0.26) ----
            Box(
                modifier = Modifier.weight(0.26f).fillMaxHeight(),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // Right header: Connected status + action icons + avatar
                    // Observe profile picture reactively via the user's metadata flow
                    val user = remember(account.pubKeyHex) { localCache.getOrCreateUser(account.pubKeyHex) }
                    val userMetadata by user.metadataOrNull()?.flow?.collectAsState() ?: remember { mutableStateOf(null) }
                    val profilePictureUrl = userMetadata?.info?.picture
                    AtnaRightHeader(
                        connectedRelayCount = connectedRelays.size,
                        account = account,
                        profilePictureUrl = profilePictureUrl,
                        onScreenChange = onScreenChange,
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    )

                    // Right sidebar panels
                    AtnaRightSidebar(
                        relayManager = relayManager,
                        localCache = localCache,
                        account = account,
                        subscriptionsCoordinator = subscriptionsCoordinator,
                        onNavigateToThread = { noteId ->
                            onScreenChange(DesktopScreen.Thread(noteId))
                        },
                    )
                }

                // FAB for compose (only on feed-like screens)
                if (!account.isReadOnly &&
                    (currentScreen is DesktopScreen.Feed || currentScreen == DesktopScreen.Reads)
                ) {
                    FloatingActionButton(
                        onClick = onShowComposeDialog,
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Post")
                    }
                }
            }
        }

        // Snackbar for zap feedback
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Center column: Search Bar header
// ---------------------------------------------------------------------------

@Composable
fun AtnaSearchBar(
    searchState: SearchBarState,
    onScreenChange: (DesktopScreen) -> Unit,
) {
    val searchText by searchState.searchText.collectAsState()

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchState.updateSearchText(it) },
            modifier =
                Modifier.weight(1f).onKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Enter && keyEvent.type == androidx.compose.ui.input.key.KeyEventType.KeyUp) {
                        onScreenChange(DesktopScreen.Search)
                        true
                    } else {
                        false
                    }
                },
            placeholder = {
                Text(
                    "Search",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            },
            trailingIcon = {
                IconButton(onClick = { onScreenChange(DesktopScreen.Search) }) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors =
                OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                ),
            textStyle =
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
        )
    }
}

// ---------------------------------------------------------------------------
// Right column: header with status + icons + avatar
// ---------------------------------------------------------------------------

@Composable
fun AtnaRightHeader(
    connectedRelayCount: Int,
    account: AccountState.LoggedIn,
    profilePictureUrl: String?,
    onScreenChange: (DesktopScreen) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        // Connected status indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (connectedRelayCount > 0) Color(0xFF4CAF50) else Color(0xFFFF5722),
                        ),
            )
            Text(
                if (connectedRelayCount > 0) "Connected" else "Connecting...",
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (connectedRelayCount > 0) {
                        Color(0xFF4CAF50)
                    } else {
                        Color(0xFFFF5722)
                    },
            )
        }

        Spacer(Modifier.width(16.dp))

        // Action icons
        IconButton(
            onClick = { onScreenChange(DesktopScreen.Messages) },
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                Icons.Default.Email,
                contentDescription = "Messages",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        IconButton(
            onClick = { onScreenChange(DesktopScreen.Notifications) },
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        IconButton(
            onClick = { onScreenChange(DesktopScreen.Settings) },
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(8.dp))

        // User avatar
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    .clickable { onScreenChange(DesktopScreen.MyProfile) },
            contentAlignment = Alignment.Center,
        ) {
            com.vitorpamplona.amethyst.commons.ui.components.UserAvatar(
                userHex = account.pubKeyHex,
                pictureUrl = profilePictureUrl,
                size = 32.dp,
                contentDescription = "Profile",
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Left Sidebar
// ---------------------------------------------------------------------------

/**
 * Data class representing a sidebar navigation item.
 */
private data class SidebarNavItem(
    val label: String,
    val icon: ImageVector,
    val screen: DesktopScreen,
    val showBadge: Boolean = false,
)

@Composable
fun AtnaLeftSidebar(
    currentScreen: DesktopScreen,
    onScreenChange: (DesktopScreen) -> Unit,
) {
    val navItems =
        listOf(
            SidebarNavItem("Home", Icons.Default.Home, DesktopScreen.Feed()),
            SidebarNavItem("Messages", Icons.Default.Email, DesktopScreen.Messages),
            SidebarNavItem("Wallet", Icons.Default.AccountBalanceWallet, DesktopScreen.Wallet),
            SidebarNavItem("Trust Domain", Icons.Default.VerifiedUser, DesktopScreen.TrustDomain),
            SidebarNavItem("Notifications", Icons.Default.Notifications, DesktopScreen.Notifications),
            SidebarNavItem("Settings", Icons.Default.Settings, DesktopScreen.Settings),
        )

    val isFeedActive = currentScreen is DesktopScreen.Feed

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(top = 12.dp, bottom = 16.dp),
    ) {
        // ATNA Logo + branding (separated from nav)
        Row(
            modifier =
                Modifier
                    .height(44.dp)
                    .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(
                painter = painterResource("icon.png"),
                contentDescription = "ATNA Logo",
                modifier =
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Text(
                "ATNA",
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    ),
                color = Color.White,
            )
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(12.dp))

        // Navigation items
        navItems.forEach { item ->
            val isSelected =
                when {
                    item.screen is DesktopScreen.Feed -> isFeedActive
                    item.screen == DesktopScreen.Messages ->
                        currentScreen == DesktopScreen.Messages ||
                            currentScreen is DesktopScreen.MarmotConversation ||
                            currentScreen is DesktopScreen.MarmotNewChat
                    else -> currentScreen == item.screen
                }

            SidebarNavItemRow(
                label = item.label,
                icon = item.icon,
                isSelected = isSelected,
                showBadge = item.showBadge,
                onClick = { onScreenChange(item.screen) },
            )

            // Feed sub-items (shown when Home/Feed is active)
            if (item.screen is DesktopScreen.Feed && isFeedActive) {
                val activeTab = (currentScreen as? DesktopScreen.Feed)?.tab ?: FeedTab.NOTES
                FeedTab.entries.forEach { feedTab ->
                    SidebarFeedSubItem(
                        label = feedTab.label,
                        isSelected = activeTab == feedTab,
                        onClick = { onScreenChange(DesktopScreen.Feed(feedTab)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarFeedSubItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        } else {
            Color.Transparent
        }
    val contentColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .padding(start = 24.dp, end = 12.dp, top = 1.dp, bottom = 1.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .clickable(onClick = onClick)
                .padding(start = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                ),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SidebarNavItemRow(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    showBadge: Boolean = false,
    onClick: () -> Unit,
) {
    val backgroundColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            Color.Transparent
        }
    val contentColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 12.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label,
                style =
                    MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (showBadge) {
                Box(
                    modifier =
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50)),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Right Sidebar
// ---------------------------------------------------------------------------

@Composable
fun AtnaRightSidebar(
    relayManager: DesktopRelayConnectionManager,
    localCache: DesktopLocalCache,
    account: AccountState.LoggedIn,
    subscriptionsCoordinator: DesktopRelaySubscriptionsCoordinator,
    onNavigateToThread: (String) -> Unit = {},
) {
    val relayStatuses by relayManager.relayStatuses.collectAsState()

    @Suppress("UNUSED_VARIABLE")
    val metadataVersion by localCache.metadataVersion.collectAsState()

    val scope = rememberCoroutineScope()

    val commentsState =
        remember {
            EventCollectionState<NotificationItem>(
                getId = { it.event.id },
                sortComparator = null,
                maxSize = 50,
                scope = scope,
            )
        }
    val zapsState =
        remember {
            EventCollectionState<NotificationItem>(
                getId = { it.event.id },
                sortComparator = null,
                maxSize = 50,
                scope = scope,
            )
        }
    val reactionsState =
        remember {
            EventCollectionState<NotificationItem>(
                getId = { it.event.id },
                sortComparator = null,
                maxSize = 50,
                scope = scope,
            )
        }

    val comments by commentsState.items.collectAsState()
    val zaps by zapsState.items.collectAsState()
    val reactions by reactionsState.items.collectAsState()

    // Load metadata for notification authors
    LaunchedEffect(comments, zaps, reactions, subscriptionsCoordinator) {
        val allPubkeys =
            (comments + zaps + reactions)
                .map { it.event.pubKey }
                .distinct()
        if (allPubkeys.isNotEmpty()) {
            subscriptionsCoordinator.loadMetadataForPubkeys(allPubkeys)
        }
    }

    // Single subscription that dispatches to 3 states
    rememberSubscription(relayStatuses, account.pubKeyHex, relayManager = relayManager) {
        val configuredRelays = relayStatuses.keys
        if (configuredRelays.isNotEmpty()) {
            createNotificationsSubscription(
                relays = configuredRelays,
                pubKeyHex = account.pubKeyHex,
                limit = 50,
                onEvent = { event, _, _, _ ->
                    if (event.pubKey == account.pubKeyHex && event !is LnZapEvent) return@createNotificationsSubscription

                    when (event) {
                        is ReactionEvent -> {
                            reactionsState.addItem(
                                NotificationItem.Reaction(event, event.createdAt, event.content),
                            )
                        }
                        is LnZapEvent -> {
                            zapsState.addItem(
                                NotificationItem.Zap(event, event.createdAt, event.amount?.toLong()),
                            )
                        }
                        is TextNoteEvent -> {
                            commentsState.addItem(
                                if (event.tags.any { it.size > 1 && it[0] == "e" }) {
                                    NotificationItem.Reply(event, event.createdAt)
                                } else {
                                    NotificationItem.Mention(event, event.createdAt)
                                },
                            )
                        }
                        is RepostEvent, is GenericRepostEvent -> {
                            reactionsState.addItem(
                                NotificationItem.Repost(event, event.createdAt),
                            )
                        }
                        else -> {
                            commentsState.addItem(
                                NotificationItem.Mention(event, event.createdAt),
                            )
                        }
                    }
                },
                onEose = { _, _ -> },
            )
        } else {
            null
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        // Comments panel (1/3)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            RightSidebarPanel(title = "Comments") {
                if (comments.isEmpty()) {
                    SidebarEmptyHint("No comments yet")
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        comments.take(8).forEach { item ->
                            SidebarCommentItem(item, localCache, onNavigateToThread)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Zaps panel (1/3)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            RightSidebarPanel(title = "Zaps") {
                if (zaps.isEmpty()) {
                    SidebarEmptyHint("No zaps yet")
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        zaps.take(8).forEach { item ->
                            SidebarZapItem(item, localCache, onNavigateToThread)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Reactions panel (1/3)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            RightSidebarPanel(title = "Reactions") {
                if (reactions.isEmpty()) {
                    SidebarEmptyHint("No reactions yet")
                } else {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        reactions.take(8).forEach { item ->
                            SidebarReactionItem(item, localCache, onNavigateToThread)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RightSidebarPanel(
    title: String,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style =
                    MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            trailing?.invoke()
        }

        content()

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
        )
    }
}

@Composable
private fun SidebarEmptyHint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.padding(vertical = 12.dp),
    )
}

@Composable
private fun SidebarCommentItem(
    item: NotificationItem,
    localCache: DesktopLocalCache,
    onNavigateToThread: (String) -> Unit = {},
) {
    val authorName = resolveAuthorName(item.event.pubKey, localCache)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onNavigateToThread(item.targetNoteId()) }
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Reply,
            contentDescription = "Comment",
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(14.dp).padding(top = 2.dp),
        )
        Column {
            Text(
                authorName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                item.event.content.take(80),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun SidebarZapItem(
    item: NotificationItem,
    localCache: DesktopLocalCache,
    onNavigateToThread: (String) -> Unit = {},
) {
    val authorName = resolveAuthorName(item.event.pubKey, localCache)
    val amount = (item as? NotificationItem.Zap)?.amount?.let { "${it / 1000} sats" } ?: ""
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onNavigateToThread(item.targetNoteId()) }
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Zap,
                contentDescription = "Zap",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                authorName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
        if (amount.isNotEmpty()) {
            Text(
                amount,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SidebarReactionItem(
    item: NotificationItem,
    localCache: DesktopLocalCache,
    onNavigateToThread: (String) -> Unit = {},
) {
    val authorName = resolveAuthorName(item.event.pubKey, localCache)
    val emoji =
        when (item) {
            is NotificationItem.Reaction -> item.content.ifEmpty { "+" }
            is NotificationItem.Repost -> "\u2B6F"
            else -> "+"
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onNavigateToThread(item.targetNoteId()) }
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = "Reaction",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                authorName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
        Text(
            emoji,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun resolveAuthorName(
    pubKeyHex: String,
    localCache: DesktopLocalCache,
): String =
    localCache
        .getUserIfExists(pubKeyHex)
        ?.toBestDisplayName()
        ?: try {
            pubKeyHex
                .hexToByteArrayOrNull()
                ?.toNpub()
                ?.toShortDisplay(5) ?: pubKeyHex.take(12)
        } catch (e: Exception) {
            pubKeyHex.take(12)
        }

@Composable
fun ProfileScreen(
    account: AccountState.LoggedIn,
    accountManager: AccountManager,
) {
    val scope = rememberCoroutineScope()

    Column {
        Text(
            "Profile",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(16.dp))

        ProfileInfoCard(
            npub = account.npub,
            pubKeyHex = account.pubKeyHex,
            isReadOnly = account.isReadOnly,
        )

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = { scope.launch { accountManager.logout() } },
            colors =
                androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
        ) {
            Text("Logout")
        }
    }
}

@Composable
fun RelaySettingsScreen(
    relayManager: DesktopRelayConnectionManager,
    account: AccountState.LoggedIn,
    accountManager: AccountManager,
    onLogout: () -> Unit,
    eventPersistenceService: com.atna.ndb.EventPersistenceService,
    marmotRouter: com.atna.marmot.MarmotEventRouter,
    uiScale: Float = 1.5f,
    onUiScaleChange: (Float) -> Unit = {},
) {
    val relayStatuses by relayManager.relayStatuses.collectAsState()
    val connectedRelays by relayManager.connectedRelays.collectAsState()
    val nwcConnection by accountManager.nwcConnection.collectAsState()
    var nwcInput by remember { mutableStateOf("") }
    var nwcError by remember { mutableStateOf<String?>(null) }

    // Load NWC on first composition
    LaunchedEffect(Unit) {
        accountManager.loadNwcConnection()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // ---- UI Scale Section ----
        item {
            Spacer(Modifier.height(16.dp))
            Text(
                "UI Scale",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Adjust the size of text and UI elements. Use Ctrl+/- to zoom.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            // Preset buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val presets = listOf("Small" to 1.0f, "Medium" to 1.25f, "Large" to 1.5f, "Extra Large" to 2.0f)
                presets.forEach { (label, value) ->
                    FilterChip(
                        selected = (uiScale - value).let { it > -0.06f && it < 0.06f },
                        onClick = { onUiScaleChange(value) },
                        label = { Text(label) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // Slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "75%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = uiScale,
                    onValueChange = { onUiScaleChange(it) },
                    valueRange = 0.75f..2.5f,
                    steps = 13,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                Text(
                    "250%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "Current: ${(uiScale * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // ---- Wallet Connect Section ----
        item {
            Spacer(Modifier.height(16.dp))
            Text(
                "Wallet Connect (NWC)",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Connect a Lightning wallet to enable zaps.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            if (nwcConnection != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "Wallet Connected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "Relay: ${nwcConnection!!.relayUri.url}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(
                        onClick = { accountManager.clearNwcConnection() },
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                    ) {
                        Text("Disconnect")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = nwcInput,
                        onValueChange = {
                            nwcInput = it
                            nwcError = null
                        },
                        label = { Text("NWC Connection String") },
                        placeholder = { Text("nostr+walletconnect://...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = nwcError != null,
                        supportingText = nwcError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    )
                    Button(
                        onClick = {
                            val result = accountManager.setNwcConnection(nwcInput)
                            result.fold(
                                onSuccess = { nwcInput = "" },
                                onFailure = { nwcError = it.message ?: "Invalid connection string" },
                            )
                        },
                        enabled = nwcInput.isNotBlank(),
                    ) {
                        Text("Connect")
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
        }

        // Developer Settings Section (only in debug mode)
        if (DebugConfig.isDebugMode) {
            item {
                com.vitorpamplona.amethyst.desktop.ui
                    .DevSettingsSection(account = account)
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
            }
        }

        // ---- Database Management ----
        item {
            var showClearDialog by remember { mutableStateOf(false) }
            var clearLmdb by remember { mutableStateOf(true) }
            var clearMarmot by remember { mutableStateOf(true) }
            var isClearing by remember { mutableStateOf(false) }
            var statusMessage by remember { mutableStateOf<String?>(null) }
            var dbSizeRefresh by remember { mutableStateOf(0) }
            val scope = rememberCoroutineScope()

            Text(
                "Database Management",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Clear cached events and Marmot messages for testing",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            // Database size display (refreshes after clearing)
            var dbSizeInfo by remember { mutableStateOf<Pair<Long, Int>?>(null) }
            LaunchedEffect(dbSizeRefresh) {
                withContext(Dispatchers.IO) {
                    dbSizeInfo = eventPersistenceService.getDbSizeInfo()
                }
            }
            dbSizeInfo?.let { (currentMB, maxMB) ->
                val pct = if (maxMB > 0) (currentMB.toFloat() / maxMB * 100).toInt() else 0
                Text(
                    "Database size: $currentMB MB / $maxMB MB ($pct%)",
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        if (pct > 85) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
                Spacer(Modifier.height(8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = clearLmdb, onCheckedChange = { clearLmdb = it }, enabled = !isClearing)
                Spacer(Modifier.width(4.dp))
                Text("Event cache (LMDB)")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = clearMarmot, onCheckedChange = { clearMarmot = it }, enabled = !isClearing)
                Spacer(Modifier.width(4.dp))
                Text("Marmot DMs")
            }
            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = { showClearDialog = true },
                enabled = !isClearing && (clearLmdb || clearMarmot),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(if (isClearing) "Clearing..." else "Clear Database")
            }

            statusMessage?.let { msg ->
                Spacer(Modifier.height(4.dp))
                Text(msg, color = if (msg.startsWith("Failed")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }

            if (showClearDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDialog = false },
                    title = { Text("Clear Local Database?") },
                    text = {
                        Text(
                            "This will permanently delete selected local data. This cannot be undone.",
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showClearDialog = false
                                isClearing = true
                                statusMessage = null
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val home = System.getProperty("user.home")
                                        if (clearLmdb) {
                                            eventPersistenceService.wipeAndRestart()
                                        }
                                        if (clearMarmot) {
                                            val marmotPath = "$home/.atna/marmot"
                                            marmotRouter.clearAndReinitialize(marmotPath)
                                        }
                                        withContext(Dispatchers.Main) {
                                            statusMessage = "Database cleared successfully"
                                            dbSizeRefresh++
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            statusMessage = "Failed: ${e.message}"
                                        }
                                    } finally {
                                        isClearing = false
                                    }
                                }
                            },
                        ) {
                            Text("Clear", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearDialog = false }) {
                            Text("Cancel")
                        }
                    },
                )
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
        }

        // ---- Connected Relays Overview ----
        item {
            Text(
                "Connected Relays",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "${connectedRelays.size} of ${relayStatuses.size} relays connected",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                IconButton(onClick = { relayManager.connect() }) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reconnect",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        items(relayStatuses.values.toList(), key = { it.url.url }) { status ->
            RelayStatusCard(
                status = status,
                onRemove = { relayManager.removeRelay(status.url) },
            )
        }

        // ---- Add Relay + Reset ----
        item {
            Spacer(Modifier.height(8.dp))
            DesktopRelayAddField(relayManager)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { relayManager.addDefaultRelays() }) {
                Text("Reset to Defaults")
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
        }

        // ---- Marmot Group Relays Section ----
        item {
            MarmotRelaySection(
                account = account,
                relayManager = relayManager,
                relayStatuses = relayStatuses,
                onReconnect = { relayManager.connect() },
            )
        }

        item {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
        }

        // ---- Account / Logout Section ----
        item {
            Text(
                "Account",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Logged in as ${account.npub.take(16)}...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onLogout,
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Log out")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DesktopRelayAddField(relayManager: DesktopRelayConnectionManager) {
    var newRelayUrl by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = newRelayUrl,
            onValueChange = { newRelayUrl = it },
            label = { Text("Add relay") },
            placeholder = { Text("wss://relay.example.com") },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        Button(
            onClick = {
                if (newRelayUrl.isNotBlank()) {
                    relayManager.addRelay(newRelayUrl)
                    newRelayUrl = ""
                }
            },
            enabled = newRelayUrl.isNotBlank(),
        ) {
            Text("Add")
        }
    }
}

@Composable
private fun MarmotRelaySection(
    account: AccountState.LoggedIn,
    relayManager: DesktopRelayConnectionManager,
    relayStatuses: Map<NormalizedRelayUrl, RelayStatus>,
    onReconnect: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var marmotRelays by remember {
        mutableStateOf(
            com.vitorpamplona.amethyst.desktop.network.DefaultRelays.MARMOT_RELAYS,
        )
    }
    var newMarmotRelay by remember { mutableStateOf("") }

    fun broadcastMarmotRelayList(relayUrls: List<String>) {
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val normalized =
                relayUrls.mapNotNull {
                    com.vitorpamplona.quartz.nip01Core.relay.normalizer.RelayUrlNormalizer
                        .normalizeOrNull(it)
                }
            val event =
                com.vitorpamplona.quartz.marmotMls.MarmotKeyPackageRelayListEvent.create(
                    relays = normalized,
                    signer = account.signer,
                )
            relayManager.broadcastToAll(event)
        }
    }

    Text(
        "Marmot Group Relays",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(4.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Relays that hold your Marmot MLS key packages for encrypted group invitations. Others will look for your key packages on these relays.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onReconnect) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Reconnect",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
    Spacer(Modifier.height(8.dp))

    marmotRelays.forEach { relayUrl ->
        Surface(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    val normalized =
                        com.vitorpamplona.quartz.nip01Core.relay.normalizer.RelayUrlNormalizer
                            .normalizeOrNull(relayUrl)
                    val status = normalized?.let { relayStatuses[it] }
                    when {
                        status?.connected == true ->
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Connected",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp),
                            )
                        status?.error != null ->
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                        else ->
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                    }
                    Text(
                        relayUrl,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (!account.isReadOnly) {
                    IconButton(
                        onClick = {
                            marmotRelays = marmotRelays - relayUrl
                            broadcastMarmotRelayList(marmotRelays)
                        },
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (!account.isReadOnly) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = newMarmotRelay,
                onValueChange = { newMarmotRelay = it },
                label = { Text("Add Marmot relay") },
                placeholder = { Text("wss://relay.example.com") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Button(
                onClick = {
                    if (newMarmotRelay.isNotBlank()) {
                        val normalized =
                            com.vitorpamplona.quartz.nip01Core.relay.normalizer.RelayUrlNormalizer
                                .normalizeOrNull(newMarmotRelay)
                        if (normalized != null && normalized.url !in marmotRelays) {
                            marmotRelays = marmotRelays + normalized.url
                            broadcastMarmotRelayList(marmotRelays)
                        }
                        newMarmotRelay = ""
                    }
                },
                enabled = newMarmotRelay.isNotBlank(),
            ) {
                Text("Add")
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                marmotRelays =
                    com.vitorpamplona.amethyst.desktop.network.DefaultRelays.MARMOT_RELAYS
                broadcastMarmotRelayList(marmotRelays)
            },
        ) {
            Text("Reset to Defaults")
        }
    }
}
