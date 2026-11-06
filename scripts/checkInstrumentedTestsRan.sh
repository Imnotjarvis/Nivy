#!/bin/bash
# Fails when a module's instrumented tests ran none. Gradle's connected tests succeed when the
# test process crashes before its first test (AGP 9.4's test runner reports that as no tests),
# and for a week every CI run ran none unnoticed. Counts the tests in the JUnit XML results the
# connected tests left for each module given; when one ran none, prints the device's crash log.
# Usage: scripts/checkInstrumentedTestsRan.sh shared/data/core [more/module ...]
set -u
cd "$(dirname "$0")/.." || exit 1
fail=0

for module in "$@"; do
    tests=$(find "$module/build/outputs/androidTest-results/connected" -maxdepth 2 -name "TEST-*.xml" -print0 2>/dev/null \
        | xargs -0 -r cat | grep -o "<testcase " | wc -l)
    if [ "$tests" -eq 0 ]; then
        echo "::error::$module: no instrumented test ran"
        fail=1
    else
        echo "$module: instrumented tests run: $tests"
    fi
done

if [ "$fail" -ne 0 ] && command -v adb >/dev/null; then
    echo "Crash log:"
    adb logcat -d -b crash | tail -80
fi

exit $fail
