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
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.atna.bugreport.CrashHandler
import com.atna.bugreport.GitHubIssueSubmitter
import com.atna.bugreport.GitHubTokenProvider
import com.atna.ndb.EventPersistenceService
import com.vitorpamplona.amethyst.commons.ui.screens.MessagesPlaceholder
import com.vitorpamplona.amethyst.desktop.account.AccountManager
import com.vitorpamplona.amethyst.desktop.account.AccountState
import com.vitorpamplona.amethyst.desktop.cache.DesktopLocalCache
import com.vitorpamplona.amethyst.desktop.network.DesktopRelayConnectionManager
import com.vitorpamplona.amethyst.desktop.subscriptions.DesktopRelaySubscriptionsCoordinator
import com.vitorpamplona.amethyst.desktop.ui.BookmarksScreen
import com.vitorpamplona.amethyst.desktop.ui.ComposeNoteDialog
import com.vitorpamplona.amethyst.desktop.ui.CrashReportDialog
import com.vitorpamplona.amethyst.desktop.ui.DesktopBugReportScreen
import com.vitorpamplona.amethyst.desktop.ui.DesktopMarmotGroupsScreen
import com.vitorpamplona.amethyst.desktop.ui.FeedScreen
import com.vitorpamplona.amethyst.desktop.ui.LoginScreen
import com.vitorpamplona.amethyst.desktop.ui.NotificationsScreen
import com.vitorpamplona.amethyst.desktop.ui.ReadsScreen
import com.vitorpamplona.amethyst.desktop.ui.SearchScreen
import com.vitorpamplona.amethyst.desktop.ui.ThreadScreen
import com.vitorpamplona.amethyst.desktop.ui.UserProfileScreen
import com.vitorpamplona.amethyst.desktop.ui.ZapFeedback
import com.vitorpamplona.amethyst.desktop.ui.profile.ProfileInfoCard
import com.vitorpamplona.amethyst.desktop.ui.relay.RelayStatusCard
import com.vitorpamplona.quartz.nip01Core.relay.client.accessories.EventCollector
import com.vitorpamplona.quartz.nip47WalletConnect.Nip47WalletConnect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okio.Path.Companion.toOkioPath

private val isMacOS = System.getProperty("os.name").lowercase().contains("mac")

/**
 * Desktop navigation state - extends AppScreen with dynamic destinations.
 */
sealed class DesktopScreen {
    object Feed : DesktopScreen()

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

    object BugReport : DesktopScreen()

    object MarmotGroups : DesktopScreen()
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
    coil3.SingletonImageLoader.setSafe {
        coil3.ImageLoader
            .Builder(it)
            .diskCache {
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
                width = 1200.dp,
                height = 800.dp,
                position = WindowPosition.Aligned(Alignment.Center),
            )
        var showComposeDialog by remember { mutableStateOf(false) }
        var replyToNote by remember { mutableStateOf<com.vitorpamplona.quartz.nip01Core.core.Event?>(null) }
        var currentScreen by remember { mutableStateOf<DesktopScreen>(DesktopScreen.Feed) }
        var crashReport by remember { mutableStateOf(pendingCrash) }

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
                }
                Menu("Help") {
                    Item("About ATNA", onClick = { })
                    Item("Keyboard Shortcuts", onClick = { })
                }
            }

            App(
                currentScreen = currentScreen,
                onScreenChange = { currentScreen = it },
                showComposeDialog = showComposeDialog,
                onShowComposeDialog = { showComposeDialog = true },
                onShowReplyDialog = { event ->
                    replyToNote = event
                    showComposeDialog = true
                },
                onDismissComposeDialog = {
                    showComposeDialog = false
                    replyToNote = null
                },
                replyToNote = replyToNote,
            )

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
    onDismissComposeDialog: () -> Unit,
    replyToNote: com.vitorpamplona.quartz.nip01Core.core.Event?,
) {
    val relayManager = remember { DesktopRelayConnectionManager() }
    val localCache = remember { DesktopLocalCache() }
    val accountManager = remember { AccountManager.create() }
    val accountState by accountManager.accountState.collectAsState()
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

    // LMDB event persistence
    val eventPersistenceService = remember { EventPersistenceService(CoroutineScope(SupervisorJob() + Dispatchers.IO)) }

    // Marmot encrypted group DMs (stub on desktop — native library is Android-only)
    val marmotManager = remember { com.atna.marmot.StubMarmotManager() as com.atna.marmot.MarmotManager }
    val marmotRouter = remember { com.atna.marmot.MarmotEventRouter(marmotManager, CoroutineScope(SupervisorJob() + Dispatchers.IO)) }

    // Subscriptions coordinator for metadata/reactions loading
    val subscriptionsCoordinator =
        remember(relayManager, localCache) {
            DesktopRelaySubscriptionsCoordinator(
                client = relayManager.client,
                scope = scope,
                indexRelays = relayManager.availableRelays.value,
                localCache = localCache,
            )
        }

    // Try to load saved account on startup
    DisposableEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            // Load account on IO dispatcher to avoid blocking UI with password prompt (readLine)
            accountManager.loadSavedAccount()
        }

        // Start LMDB event persistence and rehydrate from persisted events
        val dbPath = System.getProperty("user.home") + "/.atna/nostrdb"
        java.io.File(dbPath).mkdirs()
        eventPersistenceService.start(dbPath)

        // Rehydrate local cache from LMDB (profiles first for display names)
        scope.launch(Dispatchers.IO) {
            // Phase 1: Load profiles so names/pictures appear immediately
            val profiles =
                eventPersistenceService.loadEvents(
                    com.vitorpamplona.quartz.nip01Core.relay.filters
                        .Filter(kinds = listOf(0), limit = 10000),
                )
            profiles.forEach { event ->
                if (event is com.vitorpamplona.quartz.nip01Core.metadata.MetadataEvent) {
                    localCache.consumeMetadata(event)
                }
            }

            // Phase 2: Load recent notes so feed has content on startup
            val oneDayAgo =
                com.vitorpamplona.quartz.utils.TimeUtils
                    .now() - 86400
            val notes =
                eventPersistenceService.loadEvents(
                    com.vitorpamplona.quartz.nip01Core.relay.filters
                        .Filter(kinds = listOf(1), since = oneDayAgo, limit = 3000),
                )
            notes.sortedBy { it.createdAt }.forEach { event ->
                val author = localCache.getOrCreateUser(event.pubKey)
                val note = localCache.getOrCreateNote(event.id)
                note.loadEvent(event, author, emptyList())
            }

            // Phase 3: Trust provider lists (10040) and contact cards (30382)
            val trustEvents =
                eventPersistenceService.loadEvents(
                    com.vitorpamplona.quartz.nip01Core.relay.filters
                        .Filter(kinds = listOf(10040, 30382), limit = 2000),
                )
            trustEvents.sortedBy { it.createdAt }.forEach { event ->
                val author = localCache.getOrCreateUser(event.pubKey)
                val note = localCache.getOrCreateNote(event.id)
                note.loadEvent(event, author, emptyList())
            }

            // Prune old events
            eventPersistenceService.prune()
        }

        val persistingCollector =
            EventCollector(relayManager.client) { event, _ ->
                eventPersistenceService.persistEvent(event)
            }

        relayManager.addDefaultRelays()
        relayManager.connect()

        // Start subscriptions coordinator
        subscriptionsCoordinator.start()

        onDispose {
            persistingCollector.destroy()
            eventPersistenceService.stop()
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
        ) {
            when (accountState) {
                is AccountState.LoggedOut -> {
                    LoginScreen(
                        accountManager = accountManager,
                        onLoginSuccess = { onScreenChange(DesktopScreen.Feed) },
                    )
                }
                is AccountState.LoggedIn -> {
                    val account = accountState as AccountState.LoggedIn
                    val nwcConnection by accountManager.nwcConnection.collectAsState()

                    // Load NWC connection on first composition
                    LaunchedEffect(Unit) {
                        accountManager.loadNwcConnection()
                    }

                    // Subscribe to trust provider list (NIP-85) for logged-in user
                    LaunchedEffect(account.pubKeyHex) {
                        subscriptionsCoordinator.loadTrustProviderList(account.pubKeyHex)
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
                        onShowComposeDialog = onShowComposeDialog,
                        onShowReplyDialog = onShowReplyDialog,
                    )

                    // Compose dialog
                    if (showComposeDialog) {
                        ComposeNoteDialog(
                            onDismiss = onDismissComposeDialog,
                            relayManager = relayManager,
                            account = account,
                            replyTo = replyToNote,
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
    onShowComposeDialog: () -> Unit,
    onShowReplyDialog: (com.vitorpamplona.quartz.nip01Core.core.Event) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val connectedRelays by relayManager.connectedRelays.collectAsState()

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
                AtnaSearchBar(onScreenChange = onScreenChange)

                // Content area
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    when (currentScreen) {
                        DesktopScreen.Feed ->
                            FeedScreen(
                                relayManager = relayManager,
                                localCache = localCache,
                                account = account,
                                nwcConnection = nwcConnection,
                                subscriptionsCoordinator = subscriptionsCoordinator,
                                onCompose = onShowComposeDialog,
                                onNavigateToProfile = { pubKeyHex ->
                                    onScreenChange(DesktopScreen.UserProfile(pubKeyHex))
                                },
                                onNavigateToThread = { noteId ->
                                    onScreenChange(DesktopScreen.Thread(noteId))
                                },
                                onZapFeedback = onZapFeedback,
                            )
                        DesktopScreen.Reads ->
                            ReadsScreen(
                                relayManager = relayManager,
                                localCache = localCache,
                                account = account,
                                onNavigateToProfile = { pubKeyHex ->
                                    onScreenChange(DesktopScreen.UserProfile(pubKeyHex))
                                },
                                onNavigateToArticle = { noteId ->
                                    onScreenChange(DesktopScreen.Thread(noteId))
                                },
                            )
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
                            )
                        DesktopScreen.Messages -> MessagesPlaceholder()
                        DesktopScreen.Notifications -> NotificationsScreen(relayManager, localCache, account, subscriptionsCoordinator)
                        DesktopScreen.MyProfile ->
                            UserProfileScreen(
                                pubKeyHex = account.pubKeyHex,
                                relayManager = relayManager,
                                localCache = localCache,
                                account = account,
                                nwcConnection = nwcConnection,
                                subscriptionsCoordinator = subscriptionsCoordinator,
                                onBack = { onScreenChange(DesktopScreen.Feed) },
                                onCompose = onShowComposeDialog,
                                onNavigateToProfile = { pubKeyHex ->
                                    onScreenChange(DesktopScreen.UserProfile(pubKeyHex))
                                },
                                onZapFeedback = onZapFeedback,
                            )
                        is DesktopScreen.UserProfile ->
                            UserProfileScreen(
                                pubKeyHex = currentScreen.pubKeyHex,
                                relayManager = relayManager,
                                localCache = localCache,
                                account = account,
                                nwcConnection = nwcConnection,
                                subscriptionsCoordinator = subscriptionsCoordinator,
                                onBack = { onScreenChange(DesktopScreen.Feed) },
                                onCompose = onShowComposeDialog,
                                onNavigateToProfile = { pubKeyHex ->
                                    onScreenChange(DesktopScreen.UserProfile(pubKeyHex))
                                },
                                onZapFeedback = onZapFeedback,
                            )
                        is DesktopScreen.Thread ->
                            ThreadScreen(
                                noteId = currentScreen.noteId,
                                relayManager = relayManager,
                                localCache = localCache,
                                account = account,
                                nwcConnection = nwcConnection,
                                subscriptionsCoordinator = subscriptionsCoordinator,
                                onBack = { onScreenChange(DesktopScreen.Feed) },
                                onNavigateToProfile = { pubKeyHex ->
                                    onScreenChange(DesktopScreen.UserProfile(pubKeyHex))
                                },
                                onNavigateToThread = { noteId ->
                                    onScreenChange(DesktopScreen.Thread(noteId))
                                },
                                onZapFeedback = onZapFeedback,
                                onReply = onShowReplyDialog,
                            )
                        DesktopScreen.MarmotGroups -> DesktopMarmotGroupsScreen()
                        DesktopScreen.Settings -> RelaySettingsScreen(relayManager, account, accountManager)
                        DesktopScreen.BugReport -> DesktopBugReportScreen()
                    }

                    // FAB for compose (only on feed-like screens)
                    if (!account.isReadOnly &&
                        (currentScreen == DesktopScreen.Feed || currentScreen == DesktopScreen.Reads)
                    ) {
                        FloatingActionButton(
                            onClick = onShowComposeDialog,
                            modifier =
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black,
                            shape = CircleShape,
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "New Post")
                        }
                    }
                }
            }

            VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // ---- Right column: status/icons header + panels (weight ~0.26) ----
            Column(
                modifier = Modifier.weight(0.26f).fillMaxHeight(),
            ) {
                // Right header: Connected status + action icons + avatar
                AtnaRightHeader(
                    connectedRelayCount = connectedRelays.size,
                    account = account,
                    onScreenChange = onScreenChange,
                )

                // Right sidebar panels
                AtnaRightSidebar()
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
fun AtnaSearchBar(onScreenChange: (DesktopScreen) -> Unit) {
    var searchText by remember { mutableStateOf("") }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier =
                Modifier.weight(1f).height(40.dp).onKeyEvent { keyEvent ->
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
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            },
            trailingIcon = {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Camera",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
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
    onScreenChange: (DesktopScreen) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
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
                pictureUrl = null,
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
            SidebarNavItem("Home", Icons.Default.Home, DesktopScreen.Feed),
            SidebarNavItem("Messages", Icons.Default.Email, DesktopScreen.Messages),
            SidebarNavItem("Groups", Icons.Default.Lock, DesktopScreen.MarmotGroups),
            SidebarNavItem("Reads", Icons.AutoMirrored.Filled.Article, DesktopScreen.Reads),
            SidebarNavItem("Notifications", Icons.Default.Notifications, DesktopScreen.Notifications),
            SidebarNavItem("Settings", Icons.Default.Settings, DesktopScreen.Settings),
        )

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
            val isSelected = currentScreen == item.screen

            SidebarNavItemRow(
                label = item.label,
                icon = item.icon,
                isSelected = isSelected,
                showBadge = item.showBadge,
                onClick = { onScreenChange(item.screen) },
            )
        }
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
fun AtnaRightSidebar() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // What to Follow panel
        RightSidebarPanel(title = "What to Follow") {
            Text(
                "Coming soon",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 24.dp),
            )
        }

        // What's Happening panel
        RightSidebarPanel(title = "What's Happening") {
            Text(
                "Coming soon",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 24.dp),
            )
        }

        // Likes Feed panel
        RightSidebarPanel(
            title = "Likes Feed",
            trailing = {
                var enabled by remember { mutableStateOf(false) }
                Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        ),
                    modifier = Modifier.height(20.dp),
                )
            },
        ) {
            Text(
                "Coming soon",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 24.dp),
            )
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
                    contentColor = Color.Red,
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
) {
    val relayStatuses by relayManager.relayStatuses.collectAsState()
    val connectedRelays by relayManager.connectedRelays.collectAsState()
    val nwcConnection by accountManager.nwcConnection.collectAsState()
    var newRelayUrl by remember { mutableStateOf("") }
    var nwcInput by remember { mutableStateOf("") }
    var nwcError by remember { mutableStateOf<String?>(null) }

    // Load NWC on first composition
    LaunchedEffect(Unit) {
        accountManager.loadNwcConnection()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(24.dp))

        // Wallet Connect Section
        Text(
            "Wallet Connect (NWC)",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))

        Text(
            "Connect a Lightning wallet to enable zaps. Get a connection string from Alby, Mutiny, or other NWC-compatible wallets.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(12.dp))

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

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))

        // Developer Settings Section (only in debug mode)
        if (DebugConfig.isDebugMode) {
            com.vitorpamplona.amethyst.desktop.ui
                .DevSettingsSection(account = account)
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))
        }

        Text(
            "Relay Settings",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))

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

        Spacer(Modifier.height(16.dp))

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

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(relayStatuses.values.toList(), key = { it.url.url }) { status ->
                RelayStatusCard(
                    status = status,
                    onRemove = { relayManager.removeRelay(status.url) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { relayManager.addDefaultRelays() }) {
                Text("Reset to Defaults")
            }
        }
    }
}
