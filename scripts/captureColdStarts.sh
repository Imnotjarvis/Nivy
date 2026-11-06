#!/bin/bash
# TEMPORARY, for reviewing the splash screen: videos of cold starts, removed before the PR.
#   onboarded-light  the app the smoke test left onboarded (light theme), phone light: to Home
#   fresh-light      no data, phone light: to the welcome screen
#   fresh-dark       no data, phone dark: to the welcome screen
set -u
cd "$(dirname "$0")/.." || exit 1

PACKAGE="${PACKAGE:-com.ivy.wallet.offline.debug}"
ACTIVITY="com.ivy.wallet.RootActivity"
OUT="build/smoke"
mkdir -p "$OUT"

record() {
    local name=$1
    adb shell am force-stop "$PACKAGE"
    sleep 3
    adb shell screenrecord --time-limit 6 --size 540x1200 "/sdcard/$name.mp4" &
    sleep 1
    adb shell am start -n "$PACKAGE/$ACTIVITY" >/dev/null
    wait
    sleep 1
    adb pull "/sdcard/$name.mp4" "$OUT/$name.mp4" >/dev/null
    adb exec-out screencap -p > "$OUT/$name-end.png"
}

adb shell cmd uimode night no
sleep 3
record onboarded-light

adb shell pm clear "$PACKAGE" >/dev/null
record fresh-light

adb shell pm clear "$PACKAGE" >/dev/null
adb shell cmd uimode night yes
sleep 3
record fresh-dark

adb shell cmd uimode night no
