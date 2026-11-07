#!/bin/bash
# TEMPORARY, for the app lock pull request's screenshots; removed before it's merged. On the
# onboarded demo build the smoke test leaves: Settings with the lock turned on, its "Lock after
# leaving" row and menu, then the recent-apps view with the lock on, in light and dark mode.
# Screenshots go to build/smoke/applock/, with what UI Automator saw when a step didn't find its
# text.
set -u
cd "$(dirname "$0")/.." || exit 1

PACKAGE="${PACKAGE:-com.ivy.wallet.offline.debug}"
ACTIVITY="com.ivy.wallet.RootActivity"
OUT="build/smoke/applock"
mkdir -p "$OUT"
shots=0

dump() {
    adb shell "rm -f /sdcard/ui.xml; uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; cat /sdcard/ui.xml 2>/dev/null"
}

# The bounds "x1 y1 x2 y2" of the first element showing exactly $1, scrolling down for it up to
# 6 times. When it doesn't come, what UI Automator saw is kept.
find_text() {
    local text=$1 tries screen node
    for tries in 1 2 3 4 5 6 7; do
        screen=$(dump)
        node=$(grep -o "<node [^>]* text=\"$text\"[^>]*>" <<< "$screen" | head -1)
        if [ -n "$node" ]; then
            sed -E 's/.*bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]".*/\1 \2 \3 \4/' <<< "$node"
            return 0
        fi
        adb shell input swipe 540 1700 540 1000 400
        sleep 1
    done
    echo "FAIL: no \"$text\" on screen" >&2
    printf '%s\n' "$screen" > "$OUT/missing-${text// /_}.xml"
    adb exec-out screencap -p > "$OUT/missing-${text// /_}.png"
    return 1
}

tap_text() {
    local x1 y1 x2 y2
    read -r x1 y1 x2 y2 < <(find_text "$1") || return 1
    adb shell input tap $(( (x1 + x2) / 2 )) $(( (y1 + y2) / 2 ))
    echo "tapped \"$1\""
    sleep 1
}

shot() {
    shots=$((shots + 1))
    sleep 1
    adb exec-out screencap -p > "$OUT/$(printf %02d $shots)-$1.png"
    echo "screenshot: $1"
}

# The app from a cold start, on Home.
launch() {
    adb shell cmd statusbar collapse
    adb shell am force-stop "$PACKAGE"
    adb shell am start -W -n "$PACKAGE/$ACTIVITY" > /dev/null
    sleep 4
}

# Home, then the recent-apps view with the app in it.
recents() {
    adb shell input keyevent KEYCODE_HOME
    sleep 2
    adb shell input keyevent KEYCODE_APP_SWITCH
    sleep 3
    shot "$1"
    adb shell input keyevent KEYCODE_HOME
    sleep 1
}

# Settings, from Home's menu, which opens under the arrow at Home's top end: it has no text.
launch
recents recents-lock-off
launch
adb shell input tap 975 244
sleep 1
if tap_text "Settings" && tap_text "Lock App"; then
    shot settings-lock-on
    tap_text "Lock after leaving" && shot lock-delay-menu \
        && tap_text "5 minutes" && shot lock-delay-five-minutes
fi

# The recent-apps view with the lock on, in light and dark mode. Without a screen lock on the
# phone, the app unlocks itself as it starts.
recents recents-lock-on-light
adb shell cmd uimode night yes
sleep 2
launch
recents recents-lock-on-dark
adb shell cmd uimode night no
sleep 2

# The app's own dark and Amoled themes, chosen in Settings: its theme row goes Light, Dark, Amoled.
for theme in "Light mode:app-dark" "Dark mode:app-amoled"; do
    launch
    adb shell input tap 975 244
    sleep 1
    tap_text "Settings" && tap_text "${theme%%:*}" && sleep 2 && recents "recents-lock-on-${theme##*:}"
done

echo "app lock screenshots: $shots"
exit 0
