# ATNA

### All The Nostr Apps

A fork of [Amethyst](https://github.com/vitorpamplona/amethyst) extended with:

- **nostrdb** -- persistent local event storage via LMDB ([rust-nostr SDK](https://github.com/rust-nostr/nostr))
- **Marmot DMs** -- end-to-end encrypted group messaging using MLS (RFC 9420)
- **Desktop app** -- Compose Multiplatform for Linux (.deb)
- **NIP-42/65/85** -- relay authentication, outbox model, trust assertions

> Early development. Things will break.

## Status

| Platform | Build | Notes |
|----------|-------|-------|
| Android | `assemblePlayDebug` | Inherits Amethyst's 60+ NIP support |
| Desktop (Linux) | `packageDeb` | Subset of features, sidebar navigation |
| Marmot DMs | Android: functional | Desktop: stubs (Phase 12) |

## Building

### Prerequisites

- Java 21+
- Android SDK with platform 36 (for Android builds)

### Android APK

```bash
./gradlew :amethyst:assemblePlayDebug
```

### Desktop (.deb)

```bash
./gradlew :desktopApp:packageDeb
```

### Run Desktop directly

```bash
./gradlew :desktopApp:run
```

### Tests

```bash
./gradlew test
```

### Linting

```bash
./gradlew spotlessCheck
./gradlew spotlessApply
```

## What ATNA Adds to Amethyst

| Feature | Module | Description |
|---------|--------|-------------|
| LMDB storage | `ndb-bridge/` | Persistent event cache -- 44 event kinds, 4 GB cap, smart pruning |
| Marmot DMs | `marmot-bridge/` | MLS group messaging (kinds 443-445, 10051) |
| Bug reporter | `bug-reporter/` | In-app crash & bug reporting to GitHub Issues |
| Desktop app | `desktopApp/` | Compose Multiplatform with relay auth, outbox model, video playback |
| Memory management | integrated | LRU caching, bounded stores, heap monitoring |
| NIP-42 | `desktopApp/` | Relay authentication (Desktop) |
| NIP-65 | `desktopApp/` | Outbox model with greedy set-cover relay selection |
| NIP-85 | `quartz/`, `desktopApp/` | Trust assertions, provider lists, contact cards |

## Architecture

```
amethyst/       Android app (Kotlin, Jetpack Compose)
desktopApp/     Desktop JVM app (Compose Multiplatform)
commons/        Shared KMP UI components
quartz/         Nostr KMP library (protocol + business logic)
ndb-bridge/     LMDB event persistence via rust-nostr SDK
marmot-bridge/  Marmot MLS messaging via mdk-kotlin
bug-reporter/   Crash handling + GitHub issue submission
```

## Upstream

Fork of [vitorpamplona/amethyst](https://github.com/vitorpamplona/amethyst). ATNA inherits all of Amethyst's NIP support (60+). See [their README](https://github.com/vitorpamplona/amethyst#supported-features) for the full feature list.

## Contributing

Issues and pull requests welcome. This is an early-stage project -- expect rough edges.

## License

MIT License. Original work copyright (c) 2023 Vitor Pamplona. ATNA additions copyright (c) 2025-2026 HalfzwareLinda. See [LICENSE](LICENSE).
