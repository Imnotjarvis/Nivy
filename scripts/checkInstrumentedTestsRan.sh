#!/bin/bash
# Fails when a module's instrumented tests ran none, or when any of them failed. Gradle's
# connected tests succeed when the test process crashes before its first test (AGP 9.4's test
# runner reports that as no tests), and for a week every CI run ran none unnoticed. Reads the JUnit
# XML results the connected tests left for each module given: counts the tests, names each one
# that failed with its message and the top of its stack trace, so the log says what failed without
# the device-results artifact, and when a module ran none, prints the device's crash log.
# Usage: scripts/checkInstrumentedTestsRan.sh shared/data/core [more/module ...]
set -u
cd "$(dirname "$0")/.." || exit 1
fail=0

for module in "$@"; do
    results="$module/build/outputs/androidTest-results/connected"
    tests=$(find "$results" -maxdepth 2 -name "TEST-*.xml" -print0 2>/dev/null \
        | xargs -0 -r cat | grep -o "<testcase " | wc -l)
    if [ "$tests" -eq 0 ]; then
        echo "::error::$module: no instrumented test ran"
        fail=1
        continue
    fi
    echo "$module: instrumented tests run: $tests"
    python3 -I - "$module" "$results" <<'PY' || fail=1
import glob
import sys
import xml.etree.ElementTree as ET

module, results = sys.argv[1], sys.argv[2]
failed = 0
for path in sorted(glob.glob(f"{results}/**/TEST-*.xml", recursive=True)):
    for case in ET.parse(path).getroot().iter("testcase"):
        for problem in list(case.findall("failure")) + list(case.findall("error")):
            failed += 1
            message = (problem.get("message") or "").strip().splitlines()
            print(f"::error::{module}: {case.get('classname')}.{case.get('name')} failed: "
                  f"{message[0] if message else problem.tag}")
            for line in (problem.text or "").strip().splitlines()[:15]:
                print(f"    {line}")
sys.exit(1 if failed else 0)
PY
done

if [ "$fail" -ne 0 ] && command -v adb >/dev/null; then
    echo "Crash log:"
    adb logcat -d -b crash | tail -80
fi

exit $fail
