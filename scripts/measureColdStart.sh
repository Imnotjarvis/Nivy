#!/bin/bash
# TEMPORARY, to measure the baseline profiles' effect; removed before the PR. On the onboarded
# demo build the smoke test leaves, cold starts with the code compiled three ways, the way
# Macrobenchmark prepares them (through ProfileInstaller's receiver, which the app always had):
# - none: nothing compiled, as on a fresh sideloaded install;
# - JIT profile only: what the background dexopt compiled before, without an installed profile;
# - library profiles: what it compiles now that ProfileInstaller installs them at the first launch.
set -u
cd "$(dirname "$0")/.." || exit 1

PACKAGE="${PACKAGE:-com.ivy.wallet.offline.debug}"
ACTIVITY="com.ivy.wallet.RootActivity"
RECEIVER="$PACKAGE/androidx.profileinstaller.ProfileInstallReceiver"

broadcast() {
    adb shell am broadcast -a "androidx.profileinstaller.action.$1" "${@:2}" "$RECEIVER" \
        | tr -d '\r' | tail -1
}

to_ms() {
    if [[ $1 =~ ^([0-9]+)s([0-9]+)ms$ ]]; then
        echo $(( BASH_REMATCH[1] * 1000 + 10#${BASH_REMATCH[2]} ))
    else
        echo "${1%ms}"
    fi
}

median() { tr ' ' '\n' | grep . | sort -n | awk '{ a[NR] = $1 } END { print a[int((NR + 1) / 2)] }'; }

# One cold start: "<ms to the first frame> <ms to fully drawn>".
launch() {
    adb shell am force-stop "$PACKAGE"
    sleep 2
    adb logcat -c
    local total full
    total=$(adb shell am start -W -n "$PACKAGE/$ACTIVITY" | sed -n 's/^TotalTime: //p' | tr -d '\r')
    sleep 5
    full=$(adb logcat -d | grep "Fully drawn $PACKAGE" | tail -1 | sed -E 's/.*: \+//' | tr -d '\r')
    echo "${total:-0} $(to_ms "${full:-0ms}")"
}

# Three launches whose JIT profile is saved, then the background dexopt's compile.
warm_up() {
    for _ in 1 2 3; do
        launch > /dev/null
        echo "   save profile: $(broadcast SAVE_PROFILE)"
        sleep 2
    done
    adb shell cmd package compile -m speed-profile -f "$PACKAGE"
}

measure() {
    local label=$1 firsts="" fulls="" line
    for _ in $(seq 10); do
        line=$(launch)
        firsts="$firsts ${line% *}"
        fulls="$fulls ${line#* }"
    done
    echo "== $label"
    echo "   first frame, ms:$firsts (median $(median <<< "$firsts"))"
    echo "   fully drawn, ms:$fulls (median $(median <<< "$fulls"))"
}

echo "== Written by ProfileInstaller during the smoke test (root)"
adb root > /dev/null 2>&1
sleep 3
adb shell ls -la "/data/data/$PACKAGE/files/" 2>&1 | grep -i profile
adb shell ls -la "/data/misc/profiles/cur/0/$PACKAGE/" "/data/misc/profiles/ref/$PACKAGE/" 2>&1

adb shell cmd package compile -m verify -f "$PACKAGE"
measure "Nothing compiled (fresh sideloaded install)"

echo "== Preparing: JIT profile only"
adb shell cmd package compile --reset "$PACKAGE"
echo "   skip file: $(broadcast SKIP_FILE --es EXTRA_SKIP_FILE_OPERATION WRITE_SKIP_FILE)"
warm_up
measure "Compiled from the JIT profile only (before)"

echo "== Preparing: library profiles installed"
adb shell cmd package compile --reset "$PACKAGE"
echo "   skip file: $(broadcast SKIP_FILE --es EXTRA_SKIP_FILE_OPERATION DELETE_SKIP_FILE)"
echo "   install profile: $(broadcast INSTALL_PROFILE)"
warm_up
measure "Compiled with the library profiles (now)"

adb shell pm art dump "$PACKAGE" 2>/dev/null | head -20
