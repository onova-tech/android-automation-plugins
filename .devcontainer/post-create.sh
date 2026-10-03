#!/usr/bin/env bash
# The plugins repo builds against the engine (git submodule of android-automation-app).
# Pure JVM: no Android SDK needed.
set -euo pipefail
cd "$(dirname "$0")/.."

git submodule update --init --recursive
./gradlew --no-daemon -q :agp:installDist
echo "Ready: scripts/check-plugins.sh validates, replay-tests and builds every plugin."
