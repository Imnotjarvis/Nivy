#!/bin/bash
# Fails when Android Lint found an issue of severity Error or Fatal that app/lint-baseline.xml
# doesn't cover (lint leaves baseline issues out of its report). New warnings are printed too,
# as annotations, but aren't fatal.
# The build itself never fails on lint (abortOnError = false), so CI checks the report here.
# Usage: scripts/checkLintReport.sh [report.xml]   (default: the :app:lintDebug XML report)
set -euo pipefail
cd "$(dirname "$0")/.." || exit 1

report=${1:-}
if [ -z "$report" ]; then
    for candidate in app/build/reports/lint-results-debug.xml build/reports/lint/lint.xml; do
        [ -f "$candidate" ] && report=$candidate && break
    done
fi
if [ -z "$report" ] || [ ! -f "$report" ]; then
    echo "FAIL no lint XML report found (did :app:lintDebug run?)"
    exit 1
fi

python3 -I - "$report" "${GITHUB_STEP_SUMMARY:-/dev/null}" <<'EOF'
import collections
import sys
import xml.etree.ElementTree as ET

report, summary = sys.argv[1], sys.argv[2]
issues = ET.parse(report).getroot().findall("issue")
counts = collections.Counter((i.get("severity"), i.get("id")) for i in issues)
blocking = [i for i in issues if i.get("severity") in ("Error", "Fatal")]

with open(summary, "a") as out:
    out.write("### Android Lint (issues not in app/lint-baseline.xml)\n\n")
    if counts:
        out.write("| Severity | Check | Count |\n|---|---|---|\n")
        for (severity, check), n in sorted(counts.items()):
            out.write(f"| {severity} | {check} | {n} |\n")
    else:
        out.write("No new issues.\n")

for i in blocking:
    loc = i.find("location")
    file = loc.get("file", "") if loc is not None else ""
    line = loc.get("line", "1") if loc is not None else "1"
    msg = (i.get("message") or "").replace("%", "%25").replace("\n", " ")
    print(f"::error file={file},line={line}::[{i.get('id')}] {msg}")

for i in issues:
    if i in blocking:
        continue
    loc = i.find("location")
    file = loc.get("file", "") if loc is not None else ""
    line = loc.get("line", "1") if loc is not None else "1"
    msg = (i.get("message") or "").replace("%", "%25").replace("\n", " ")
    print(f"::warning file={file},line={line}::[{i.get('id')}] {msg}")

others = len(issues) - len(blocking)
if blocking:
    print(f"FAIL {len(blocking)} new lint error(s); {others} other new issue(s). "
          "Fix them, or refresh the baseline (Actions > Build > Run workflow > update-lint-baseline).")
    sys.exit(1)
print(f"ok   no new lint errors ({others} new warning/info issue(s), see the run summary)")
EOF
