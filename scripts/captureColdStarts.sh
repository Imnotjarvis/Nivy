#!/bin/bash
# TEMPORARY, for reviewing the splash screen: screenshots in quick succession of a cold start of
# the app the smoke test left onboarded, in light and in dark mode. Removed before the PR.
set -u
cd "$(dirname "$0")/.." || exit 1

PACKAGE="${PACKAGE:-com.ivy.wallet.offline.debug}"
ACTIVITY="com.ivy.wallet.RootActivity"
OUT="build/smoke"
mkdir -p "$OUT"

for mode in light dark; do
    if [ "$mode" = dark ]; then adb shell cmd uimode night yes; else adb shell cmd uimode night no; fi
    sleep 3
    adb shell am force-stop "$PACKAGE"
    sleep 2
    adb shell am start -n "$PACKAGE/$ACTIVITY" >/dev/null &
    for i in 1 2 3 4 5 6 7 8 9 10; do
        adb exec-out screencap -p > "$OUT/coldstart-$mode-$i.png"
    done
    wait
    sleep 5
    adb exec-out screencap -p > "$OUT/coldstart-$mode-final.png"
done
adb shell cmd uimode night no
