#!/usr/bin/env bash
# The plugins repo builds against the engine (git submodule of android-automation-app).
# Pure JVM: no Android SDK needed.
set -euo pipefail
cd "$(dirname "$0")/.."

# adb client only (to capture screen dumps); devices are reached through the host adb server
# via ADB_SERVER_SOCKET, see README of the app repo.
# The base image ships a Yarn apt source whose GPG key has expired, which makes apt-get update fail;
# Yarn isn't needed here, so disable it.
if [ -f /etc/apt/sources.list.d/yarn.list ]; then
  sudo mv /etc/apt/sources.list.d/yarn.list /etc/apt/sources.list.d/yarn.list.disabled
fi
sudo apt-get update -qq && sudo apt-get install -y -qq adb

git submodule update --init --recursive
./gradlew --no-daemon -q :agp:installDist
echo "Ready: scripts/check-plugins.sh validates, replay-tests and builds every plugin."

# Claude Code CLI (native installer -> ~/.local/bin/claude). Non-fatal: the build above is what matters.
if ! command -v claude >/dev/null 2>&1 && [ ! -x "$HOME/.local/bin/claude" ]; then
  curl -fsSL https://claude.ai/install.sh | bash || echo "WARN: Claude Code install failed; run the installer manually"
fi
