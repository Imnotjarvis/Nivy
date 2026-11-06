#!/bin/bash
# Installs each APK on the connected device/emulator as a fresh install and goes through it as a
# new user does: from the welcome screen through every onboarding step (Continue offline, Start
# fresh, the currency, skipping accounts and categories, which adds the starter ones) to Home,
# allowing the notifications Home asks for. Then it opens the app the way app shortcuts do
# (straight on the add-transaction screen) and presses back twice, which goes through the
# OnBackPressedDispatcher: Android 16 no longer calls Activity.onBackPressed() for apps targeting
# it. It fails if the app process dies, anything lands in the crash log, or a screen it waits for
# doesn't show.
# Screenshots of each step go to build/smoke/ for a visual check.
# Usage: scripts/smokeTestApks.sh path/to/app-debug.apk [more.apk ...]
set -u
cd "$(dirname "$0")/.." || exit 1

PACKAGE="${PACKAGE:-com.ivy.wallet.offline.debug}"
ACTIVITY="com.ivy.wallet.RootActivity"
ADD_TRANSACTION_ACTION="ivy.wallet.intent.action.add_transaction"
OUT="build/smoke"
mkdir -p "$OUT"
fail=0

# Fails the APK if something landed in the crash log or, except after back (which may leave
# the app), its process is gone.
check() {
    local name=$1 step=$2 crashes
    crashes=$(adb logcat -d -b crash)
    if [ -n "$crashes" ] || { [ "$step" != back ] && ! adb shell pidof "$PACKAGE" >/dev/null; }; then
        echo "FAIL [$name] $step: crashed or exited"
        echo "$crashes" | head -80
        adb logcat -d | grep -E "AndroidRuntime|$PACKAGE" | tail -80
        return 1
    fi
    echo "ok   [$name] $step"
}

# The bounds "x1 y1 x2 y2" of the first element on screen showing this text, in any case (system
# dialogs show their buttons in capitals), as UI Automator sees the screen. Waits up to 30
# seconds for it. The last screen UI Automator could read is left in $OUT/screen.xml.
find_text() {
    local text=$1 start=$SECONDS screen node
    while (( SECONDS - start < 30 )); do
        screen=$(adb shell "rm -f /sdcard/smoke-ui.xml; uiautomator dump /sdcard/smoke-ui.xml >/dev/null 2>&1; cat /sdcard/smoke-ui.xml 2>/dev/null")
        if [[ "$screen" == *"<hierarchy"* ]]; then
            printf '%s\n' "$screen" > "$OUT/screen.xml"
            node=$(grep -io "<node [^>]*text=\"$text\"[^>]*>" <<< "$screen" | head -1)
            if [ -n "$node" ]; then
                sed -E 's/.*bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]".*/\1 \2 \3 \4/' <<< "$node"
                return 0
            fi
        fi
        sleep 1
    done
    return 1
}

# Waits for the screen showing $3, screenshots it as $OUT/<apk>-$2.png and checks the app, then
# taps the element showing $4, when given. When the screen doesn't come, what UI Automator saw
# of it is kept beside its screenshot.
step() {
    local name=$1 step=$2 shows=$3 tap=${4:-} x1 y1 x2 y2
    if ! find_text "$shows" >/dev/null; then
        adb exec-out screencap -p > "$OUT/$name-$step.png"
        cp "$OUT/screen.xml" "$OUT/$name-$step.xml" 2>/dev/null
        echo "FAIL [$name] $step: no \"$shows\" on screen"
        return 1
    fi
    adb exec-out screencap -p > "$OUT/$name-$step.png"
    check "$name" "$step" || return 1
    [ -z "$tap" ] && return 0
    if ! read -r x1 y1 x2 y2 < <(find_text "$tap"); then
        echo "FAIL [$name] $step: no \"$tap\" to tap"
        return 1
    fi
    adb shell input tap $(( (x1 + x2) / 2 )) $(( (y1 + y2) / 2 ))
}

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
    # Each step waits for a text only its screen shows: accounts and categories both have Skip.
    step "$name" 1-welcome "Continue offline" "Continue offline" \
        && step "$name" 2-start "Start fresh" "Start fresh" \
        && step "$name" 3-currency "Set currency" "Set" \
        && step "$name" 4-accounts "Add accounts" "Skip" \
        && step "$name" 5-categories "Add categories" "Skip" \
        && step "$name" 6-notifications "Allow" "Allow" \
        && step "$name" 7-home "Home" \
        || { fail=1; continue; }

    adb shell am start -W -n "$PACKAGE/$ACTIVITY" -a "$ADD_TRANSACTION_ACTION" \
        --es add_transaction_type_extra EXPENSE
    sleep 5
    adb exec-out screencap -p > "$OUT/$name-8-add-transaction.png"
    check "$name" "add-transaction launch" || { fail=1; continue; }

    adb shell input keyevent KEYCODE_BACK
    sleep 2
    adb shell input keyevent KEYCODE_BACK
    sleep 3
    check "$name" back || fail=1
done

exit $fail
