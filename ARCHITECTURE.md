# ATNA Architecture

**ATNA** (All The Nostr Apps) is a fork of [Amethyst](https://github.com/vitorpamplona/amethyst) — the most feature-complete Nostr client (60+ NIPs) — extended with persistent local storage via [nostrdb](https://github.com/damus-io/nostrdb) and end-to-end encrypted group DMs via the [Marmot protocol](https://github.com/marmot-protocol).

## Module Overview

```
atna/
├── amethyst/         Android app (Jetpack Compose, forked from upstream)
├── quartz/           KMP Nostr protocol library (60+ NIPs, forked)
├── commons/          KMP shared UI components (forked)
├── desktopApp/       Compose Desktop for Linux (.deb)
├── ammolite/         Android library (forked, unchanged)
├── benchmark/        Performance benchmarks
├── ndb-bridge/       [NEW] nostrdb storage via rust-nostr SDK
├── marmot-bridge/    [NEW] Marmot encrypted DMs via mdk-kotlin
└── bug-reporter/     [NEW] In-app bug reporting to GitHub Issues
```

## Key Integration Points

### 1. nostrdb Storage (ndb-bridge)

**Goal**: Persistent local event database using nostrdb (LMDB-backed, zero-copy).

**How**: Amethyst has an `IEventStore` interface in `quartz/` that abstracts storage. We implement `NdbEventStore` using the [rust-nostr SDK](https://github.com/rust-nostr/nostr) Kotlin bindings (`io.github.rust-nostr:nostr-sdk`), which wraps nostrdb internally. SQLite remains the default; nostrdb is opt-in.

**Dependency**: `io.github.rust-nostr:nostr-sdk` (Maven Central, UniFFI-generated Kotlin bindings)

### 2. Marmot Encrypted DMs (marmot-bridge)

**Goal**: MLS-over-Nostr group messaging (RFC 9420), interoperable with WhiteNoise.

**How**: Wraps [mdk-kotlin](https://github.com/marmot-protocol/mdk) which provides UniFFI/JNA bindings to the Marmot Development Kit (Rust). Adds a "Secure Groups" tab alongside existing NIP-04/NIP-44 DMs.

**Event kinds**: 443 (KeyPackage), 444 (Welcome), 445 (Group), 10051 (KeyPackage relay list)

**Dependency**: `com.github.marmot-protocol:mdk-kotlin:0.5.2` (JitPack)

### 3. Bug Reporter (bug-reporter)

**Goal**: Users can report bugs from inside the app, creating GitHub Issues with device info and logs.

**How**: Direct POST to GitHub REST API using OkHttp (already an Amethyst dep). Uses a PAT with `public_repo` scope for development; serverless proxy for production.

## Build Targets

| Target | Command | Output |
|--------|---------|--------|
| Android APK | `./gradlew :amethyst:assemblePlayDebug` | `amethyst/build/outputs/apk/play/debug/` |
| Ubuntu .deb | `./gradlew :desktopApp:packageDeb` | `desktopApp/build/compose/binaries/main/deb/` |
| All tests | `./gradlew test` | JVM unit tests, no emulator needed |

## Correlated Repositories

| Repo | Purpose | Upstream |
|------|---------|----------|
| [HalfzwareLinda/atna](https://github.com/HalfzwareLinda/atna) | Main app | vitorpamplona/amethyst |
| [HalfzwareLinda/nostr](https://github.com/HalfzwareLinda/nostr) | rust-nostr SDK (pinned) | rust-nostr/nostr |
| [HalfzwareLinda/mdk](https://github.com/HalfzwareLinda/mdk) | Marmot MDK (pinned) | marmot-protocol/mdk |

## System Requirements (Development)

- Java 21+ (SDKMAN: `sdk install java 21.0.6-tem`)
- Android SDK with platform 36
- ~6GB RAM for builds (gradle.properties tuned for 14GB systems)
- `JAVA_HOME` must be on PATH for git hooks
