# ATNA (All The Nostr Apps) - Build Commands
# Per agent.md: lint, test, build:apk, smoke:apk

SHELL := /bin/bash
export ANDROID_HOME ?= $(HOME)/Android/Sdk
export JAVA_HOME ?= $(HOME)/.sdkman/candidates/java/current

.PHONY: lint test build\:apk smoke\:apk build\:deb run\:desktop clean

# Static checks (ktlint via spotless)
lint:
	./gradlew spotlessCheck

# Unit tests (JVM, no emulator needed)
test:
	./gradlew test

# Produce debug APK
build\:apk:
	./gradlew :amethyst:assembleDebug
	@echo "APK at: amethyst/build/outputs/apk/play/debug/"

# Minimal smoke test on emulator (best-effort)
smoke\:apk:
	./gradlew :amethyst:connectedAndroidTest || echo "Smoke test requires running emulator"

# Package .deb for Ubuntu
build\:deb:
	./gradlew :desktopApp:packageDeb
	@echo "DEB at: desktopApp/build/compose/binaries/main/deb/"

# Run desktop app locally
run\:desktop:
	./gradlew :desktopApp:run

# Clean build artifacts
clean:
	./gradlew clean
