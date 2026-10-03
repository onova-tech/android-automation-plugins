#!/usr/bin/env bash
# Validates, replay-tests and builds every plugin in plugins/ with agp.
# Usage: scripts/check-plugins.sh [out-dir]   (built packages go to out-dir, default build/packages)
set -euo pipefail
cd "$(dirname "$0")/.."

AGP=agp/build/install/agp/bin/agp
OUT=${1:-build/packages}
[ -x "$AGP" ] || ./gradlew --quiet :agp:installDist
mkdir -p "$OUT"

failed=()
for dir in plugins/*/; do
  name=$(basename "$dir")
  echo "::group::$name"
  if "$AGP" validate "$dir" --libs libraries \
     && "$AGP" test "$dir" --libs libraries \
     && "$AGP" build "$dir" --libs libraries -o "$OUT/$name.agp"; then
    echo "OK $name"
  else
    failed+=("$name")
  fi
  echo "::endgroup::"
done

if [ ${#failed[@]} -gt 0 ]; then
  echo "Failed plugins: ${failed[*]}" >&2
  exit 1
fi
echo "All plugins passed."
