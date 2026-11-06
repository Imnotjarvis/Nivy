#!/bin/bash
# TEMPORARY, for the pull request's screenshots; removed before it's merged. On the onboarded demo
# build the smoke test leaves, in airplane mode: a base currency change and the exchange rates
# screen, whose downloads fail, then a new expense whose save fails (this temporary build fails
# to save an amount of 52). Screenshots go to build/smoke/messages/, with what UI Automator saw
# when a step didn't find its text.
set -u
cd "$(dirname "$0")/.." || exit 1

PACKAGE="${PACKAGE:-com.ivy.wallet.offline.debug}"
ACTIVITY="com.ivy.wallet.RootActivity"
OUT="build/smoke/messages"
mkdir -p "$OUT"

dump() {
    adb shell "rm -f /sdcard/ui.xml; uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; cat /sdcard/ui.xml 2>/dev/null"
}

# The bounds "x1 y1 x2 y2" of the first element showing exactly this text, waiting up to 20
# seconds for it. When it doesn't come, what UI Automator saw is kept.
find_text() {
    local text=$1 start=$SECONDS screen node
    while (( SECONDS - start < 20 )); do
        screen=$(dump)
        node=$(grep -o "<node [^>]*text=\"$text\"[^>]*>" <<< "$screen" | head -1)
        if [ -n "$node" ]; then
            sed -E 's/.*bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]".*/\1 \2 \3 \4/' <<< "$node"
            return 0
        fi
        sleep 1
    done
    echo "no \"$text\" on screen" >&2
    printf '%s\n' "$screen" > "$OUT/missing-${text// /_}.xml"
    adb exec-out screencap -p > "$OUT/missing-${text// /_}.png"
    return 1
}

tap_text() {
    local x1 y1 x2 y2
    read -r x1 y1 x2 y2 < <(find_text "$1") || return 1
    adb shell input tap $(( (x1 + x2) / 2 )) $(( (y1 + y2) / 2 ))
    echo "tapped \"$1\""
}

# Three screenshots, a second apart, while a message shows.
shots() {
    for i in 1 2 3; do
        sleep 1
        adb exec-out screencap -p > "$OUT/$1-$i.png"
    done
    echo "screenshots: $1"
}

# The app from a cold start: Home, or the screen the given action opens.
launch() {
    adb shell cmd statusbar collapse
    adb shell am force-stop "$PACKAGE"
    adb shell am start -W -n "$PACKAGE/$ACTIVITY" "$@" > /dev/null
    sleep 3
}

# Settings, from Home's menu, which opens under the arrow at Home's top end: it has no text.
open_settings() {
    launch
    find_text "Home" > /dev/null || return 1
    adb shell input tap 975 244
    tap_text "Settings" && sleep 2
}

adb shell cmd connectivity airplane-mode enable
sleep 3

# Another base currency, whose rates it downloads.
open_settings && tap_text "Set currency" && sleep 2 && tap_text "AED" && sleep 1 && tap_text "Save" \
    && shots base-currency

# The exchange rates screen, which downloads them as it opens. It's further down Settings.
if open_settings; then
    if ! find_text "Exchange rates" > /dev/null; then
        adb shell input swipe 540 1800 540 800 400
        sleep 1
    fi
    tap_text "Exchange rates" && shots exchange-rates
fi

# A new expense of 52, after the category it then asks for (skipped).
launch -a ivy.wallet.intent.action.add_transaction --es add_transaction_type_extra EXPENSE
tap_text "5" && tap_text "2" && tap_text "Enter" && sleep 1 && tap_text "Skip" && sleep 1 \
    && tap_text "Add" && shots save

adb shell cmd connectivity airplane-mode disable
exit 0
