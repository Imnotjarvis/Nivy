#!/bin/bash
# Lists every external library the app ships (direct and transitive) and fails if any of
# them is Firebase, Google Play services, an ads/billing SDK or a crash/analytics reporter.
# verifyOffline.sh only reads our own build files; this checks what Gradle actually resolves.
# Usage: scripts/auditRuntimeDependencies.sh [configuration]   (default: releaseRuntimeClasspath)
set -euo pipefail
cd "$(dirname "$0")/.." || exit 1

CONFIG="${1:-releaseRuntimeClasspath}"
FORBIDDEN='^(com\.google\.firebase|com\.google\.android\.gms|com\.google\.android\.play|com\.google\.android\.datatransport|com\.google\.android\.ump|com\.google\.ads|com\.android\.billingclient|com\.crashlytics|io\.sentry|com\.bugsnag|com\.facebook|com\.mixpanel|com\.amplitude|com\.appsflyer|com\.adjust|io\.branch)[.:]'

tree=$(./gradlew -q :app:dependencies --configuration "$CONFIG")
# "+--- group:artifact:version" -> "group:artifact" (project modules have no colon-pair)
libs=$(echo "$tree" | { grep -oE -- '--- [^ :]+:[^ :]+' || true; } | sed 's/^--- //' | sort -u)

if [ -z "$libs" ]; then
    echo "FAIL could not read the $CONFIG dependency tree"
    exit 1
fi

echo "$(echo "$libs" | wc -l) external libraries in :app $CONFIG, by group:"
echo "$libs" | cut -d: -f1 | sort | uniq -c | sort -k2

bad=$(echo "$libs" | grep -E "$FORBIDDEN" || true)
if [ -n "$bad" ]; then
    echo "FAIL forbidden libraries:"
    echo "$bad" | sed 's/^/       /'
    exit 1
fi
echo "ok   no Firebase, Play services, ads, billing or tracking libraries"
