#!/bin/bash
# Installs each APK on the connected device/emulator as a fresh install, launches it and
# fails if the app process dies or anything lands in the crash log within a few seconds.
# Screenshots of each launched APK go to build/smoke/ for a visual check.
# Usage: scripts/smokeTestApks.sh path/to/app-debug.apk [more.apk ...]
set -u
cd "$(dirname "$0")/.." || exit 1

PACKAGE="${PACKAGE:-com.ivy.wallet.offline.debug}"
ACTIVITY="com.ivy.wallet.RootActivity"
OUT="build/smoke"
mkdir -p "$OUT"
fail=0

for apk in "$@"; do
    name=$(basename "$apk" .apk)
    echo "== $name"
    adb uninstall "$PACKAGE" >/dev/null 2>&1 || true
    if ! adb install "$apk"; then
        echo "FAIL [$name] install"
        fail=1
        continue
    fi

    adb logcat -c
    adb shell am start -W -n "$PACKAGE/$ACTIVITY"
    sleep 15
    adb exec-out screencap -p > "$OUT/$name.png"

    crashes=$(adb logcat -d -b crash)
    if adb shell pidof "$PACKAGE" >/dev/null && [ -z "$crashes" ]; then
        echo "ok   [$name] running after launch"
    else
        echo "FAIL [$name] crashed or exited"
        echo "$crashes" | head -80
        adb logcat -d | grep -E "AndroidRuntime|$PACKAGE" | tail -80
        fail=1
    fi
done

exit $fail
