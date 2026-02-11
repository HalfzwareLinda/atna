# ATNA Changelog

All notable changes to ATNA are documented here. For the upstream Amethyst changelog, see [CHANGELOG-amethyst.md](CHANGELOG-amethyst.md).

## [Unreleased]

### nostrdb / LMDB Storage
- Persistent local event database using rust-nostr SDK 0.44.2 (LMDB-backed)
- 44 event kinds persisted across 4 groups: Engagement, Content, Private, Metadata
- 4 GB size cap with 3-tier pruning (normal at <85%, moderate at 85-100%, aggressive at >100%)
- Phased startup rehydration: relay lists first, then profiles, contacts, notes, DMs in parallel
- Write-behind cache with Channel(2000, DROP_OLDEST), batch size 100
- LRU eviction for in-memory caches (20k users, 50k notes, 10k addressable)

### Marmot Encrypted DMs
- MLS group messaging (RFC 9420) via mdk-kotlin 0.5.2
- Event kinds: 443 (KeyPackage), 444 (Welcome), 445 (Group), 10051 (KeyPackage relay list)
- Group creation, messaging, invite accept/decline UI
- NIP-17 DM invitation fallback when no key package found
- Marmot relay list management with outbox model integration
- Full conversation UI with message bubbles, timestamps, Marmot watermark

### Desktop App (Linux)
- Compose Multiplatform desktop application (.deb package)
- Sidebar navigation with feed, notifications, DMs, search, settings
- NIP-42 relay authentication (DesktopAuthCoordinator)
- NIP-65 outbox model with greedy set-cover relay selection (DesktopOutboxResolver)
- Bounded in-memory caches with LRU eviction
- GStreamer-based video playback (ComposeMediaPlayer)
- Coil image caching (256MB disk + 64MB memory)
- Relay management UI with categorized sections (NWC, Connected, Marmot)
- Memory manager with periodic stats and heap monitoring

### Bug Reporter
- In-app bug reporting to GitHub Issues via OkHttp + GitHub PAT
- CrashHandler with file-based persistence (Android + Desktop)
- Crash dialog for user-submitted reports with system info

### NIP Support (beyond Amethyst)
- NIP-42: Relay authentication on Desktop
- NIP-65: Outbox model with relay hints on Desktop
- NIP-85: Trust assertions with provider lists and contact cards

### Infrastructure
- CI: build.yml builds Android APK + Desktop DEB on push/PR, creates GitHub Release on tags
- Three new modules: ndb-bridge/, marmot-bridge/, bug-reporter/
- Marmot protocol event kinds registered in EventFactory
- Spotless enforces MIT license headers on all Kotlin files
