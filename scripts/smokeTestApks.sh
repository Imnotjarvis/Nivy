#!/bin/bash
# Installs each APK on the connected device/emulator as a fresh install, launches it and
# fails if the app process dies or anything lands in the crash log. After the plain launch it
# opens the app the way app shortcuts do (straight on the add-transaction screen) and presses
# back twice, which goes through the OnBackPressedDispatcher: Android 16 no longer calls
# Activity.onBackPressed() for apps targeting it.
# Screenshots of each launched APK go to build/smoke/ for a visual check.
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
    check "$name" launch || { fail=1; continue; }

    adb shell am start -W -n "$PACKAGE/$ACTIVITY" -a "$ADD_TRANSACTION_ACTION" \
        --es add_transaction_type_extra EXPENSE
    sleep 5
    adb exec-out screencap -p > "$OUT/$name-add-transaction.png"
    check "$name" "add-transaction launch" || { fail=1; continue; }

    adb shell input keyevent KEYCODE_BACK
    sleep 2
    adb shell input keyevent KEYCODE_BACK
    sleep 3
    check "$name" back || fail=1
done

exit $fail
