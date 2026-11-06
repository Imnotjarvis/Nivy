#!/bin/bash
# TEMPORARY, for the pull request's screenshots; removed before it's merged. On the onboarded demo
# build the smoke test leaves, in airplane mode: the exchange rates screen and a base currency
# change, whose downloads fail, then a new expense whose save fails (this temporary build fails
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

# Taps the first element showing exactly this text, waiting up to 20 seconds for it.
tap_text() {
    local text=$1 start=$SECONDS screen node x1 y1 x2 y2
    while (( SECONDS - start < 20 )); do
        screen=$(dump)
        node=$(grep -o "<node [^>]*text=\"$text\"[^>]*>" <<< "$screen" | head -1)
        if [ -n "$node" ]; then
            read -r x1 y1 x2 y2 < <(sed -E 's/.*bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]".*/\1 \2 \3 \4/' <<< "$node")
            adb shell input tap $(( (x1 + x2) / 2 )) $(( (y1 + y2) / 2 ))
            echo "tapped \"$text\""
            return 0
        fi
        sleep 1
    done
    echo "no \"$text\" on screen"
    printf '%s\n' "$screen" > "$OUT/missing-${text// /_}.xml"
    adb exec-out screencap -p > "$OUT/missing-${text// /_}.png"
    return 1
}

# Three screenshots, a second apart, while a message shows.
shots() {
    for i in 1 2 3; do
        sleep 1
        adb exec-out screencap -p > "$OUT/$1-$i.png"
    done
    echo "screenshots: $1"
}

adb shell cmd connectivity airplane-mode enable
sleep 3
adb shell am start -W -n "$PACKAGE/$ACTIVITY"
sleep 3

# Home's menu opens under the arrow at its top end (no text to find it by), then Settings.
adb shell input tap 975 244
sleep 2
tap_text "Settings" && sleep 2

if ! tap_text "Exchange rates"; then
    adb shell input swipe 540 1800 540 700
    sleep 1
    tap_text "Exchange rates"
fi && shots exchange-rates

adb shell input keyevent KEYCODE_BACK
sleep 2
adb shell input swipe 540 700 540 1800
sleep 1
tap_text "Set currency" && sleep 2 && tap_text "AED" && sleep 1 && tap_text "Save" && shots base-currency

adb shell am start -W -n "$PACKAGE/$ACTIVITY" -a ivy.wallet.intent.action.add_transaction \
    --es add_transaction_type_extra EXPENSE
sleep 3
tap_text "5" && tap_text "2" && tap_text "Enter" && sleep 1 && tap_text "Add" && shots save

adb shell cmd connectivity airplane-mode disable
exit 0
