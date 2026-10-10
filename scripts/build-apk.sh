#!/usr/bin/env bash
# Assembles the APK and publishes it for the web control plane.
#
# Requires: JDK 17, Android SDK (platform 34, build-tools 34), app/libs/libbox.aar.
# Produce libbox.aar first with scripts/build-libbox.sh if it is missing.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PUBLISH="$(cd "$ROOT/.." && pwd)/public"

if [ ! -f "$ROOT/app/libs/libbox.aar" ]; then
  echo "app/libs/libbox.aar is missing — building the real core first" >&2
  bash "$ROOT/scripts/build-libbox.sh"
fi

echo "sdk.dir=${ANDROID_HOME}" > "$ROOT/local.properties"
cd "$ROOT"

if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
  if command -v gradle >/dev/null 2>&1; then
    echo "gradle-wrapper.jar is missing — regenerating it with the installed Gradle" >&2
    gradle wrapper --gradle-version 8.11.1
  else
    echo "gradle/wrapper/gradle-wrapper.jar is missing and no 'gradle' is installed to regenerate it." >&2
    echo "Install Gradle 8.11.1 (or copy the wrapper jar from any Gradle 8.11.1 project) and re-run." >&2
    exit 1
  fi
fi
./gradlew --no-daemon --console=plain assembleDebug

APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
mkdir -p "$PUBLISH"
cp "$APK" "$PUBLISH/nora-tunnel-debug.apk"

echo "==> apk: $APK ($(stat -c%s "$APK") bytes)"
echo "==> published to $PUBLISH/nora-tunnel-debug.apk"
sha256sum "$PUBLISH/nora-tunnel-debug.apk"

if command -v unzip >/dev/null 2>&1; then
  echo "==> native core inside the apk:"
  unzip -l "$APK" | grep -E "lib/.*\.so" || echo "    (no native library found — the core is missing!)"
fi
