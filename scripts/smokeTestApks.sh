#!/bin/bash
# Installs each APK on the connected device/emulator as a fresh install and goes through it as a
# new user does: from the welcome screen through every onboarding step (Continue offline, Start
# fresh, the currency, skipping accounts and categories, which adds the starter ones) to Home,
# allowing the notifications Home asks for. Then it opens the app the way app shortcuts do
# (straight on the add-transaction screen, waiting for its keypad) and presses back twice, which
# goes through the OnBackPressedDispatcher: Android 16 no longer calls Activity.onBackPressed()
# for apps targeting it. It fails if the app process dies, anything lands in the crash log, or a
# screen it waits for doesn't show.
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

# The first element in this screen dump showing exactly this text, in any case (system dialogs
# show their buttons in capitals).
node_with_text() {
    grep -io "<node [^>]*text=\"$2\"[^>]*>" <<< "$1" | head -1
}

# The bounds "x1 y1 x2 y2" of an element.
bounds() {
    sed -E 's/.*bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]".*/\1 \2 \3 \4/' <<< "$1"
}

# The bounds of the first element on screen showing this text, as UI Automator sees the screen.
# Waits up to 30 seconds for it. Another app's "isn't responding" dialog (a freshly booted
# emulator's launcher can stall) is told to wait; the app's own would be a failure, and stays.
# The last screen UI Automator could read is left in $OUT/screen.xml.
find_text() {
    local text=$1 start=$SECONDS screen node x1 y1 x2 y2
    while (( SECONDS - start < 30 )); do
        screen=$(adb shell "rm -f /sdcard/smoke-ui.xml; uiautomator dump /sdcard/smoke-ui.xml >/dev/null 2>&1; cat /sdcard/smoke-ui.xml 2>/dev/null")
        if [[ "$screen" == *"<hierarchy"* ]]; then
            printf '%s\n' "$screen" > "$OUT/screen.xml"
            node=$(node_with_text "$screen" "$text")
            if [ -n "$node" ]; then
                bounds "$node"
                return 0
            fi
            if grep -qE 'text="[^"]*isn.t responding"' <<< "$screen" \
                && ! grep -qiE 'text="Nivy[^"]*isn.t responding"' <<< "$screen"; then
                node=$(node_with_text "$screen" "Wait")
                if [ -n "$node" ] && read -r x1 y1 x2 y2 < <(bounds "$node"); then
                    echo "Another app isn't responding: waiting for it" >&2
                    adb shell input tap $(( (x1 + x2) / 2 )) $(( (y1 + y2) / 2 ))
                fi
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

# A freshly booted emulator's launcher can stall, and its "isn't responding" dialog then takes the
# taps meant for the app (run 364: "Continue offline" never went through). Without error dialogs,
# Android closes a stalled app instead. The app's own crash or stall still fails: check() finds
# its crash log, or its process gone.
adb shell settings put global hide_error_dialogs 1
echo "error dialogs hidden: $(adb shell settings get global hide_error_dialogs | tr -d '\r')"

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
    # A new transaction opens on its keypad, and only the keypad shows Enter: the editor opened,
    # not just an app that didn't crash.
    step "$name" 8-add-transaction "Enter" || { fail=1; continue; }

    adb shell input keyevent KEYCODE_BACK
    sleep 2
    adb shell input keyevent KEYCODE_BACK
    sleep 3
    check "$name" back || fail=1
done

exit $fail
