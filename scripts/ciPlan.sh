#!/bin/bash
# Decides which CI jobs a run needs, so a push only pays for what its changes can affect.
# Prints "run=", "device=" and "lint=" lines (for $GITHUB_OUTPUT) and a reason on stderr.
#   run     checks, build and unit tests (false only for maintenance actions and idle nightlies)
#   device  the R8-minified demo APK and the emulator job (startup crashes, R8 rules, Room, Hilt)
#   lint    Android Lint
# Branch pushes skip the device work when they only change code or resources of features a fresh
# install never opens (everything but onboarding) or widgets, unit tests, or docs.
# Tags, pushes to the default branch, nightly and manual runs always run everything.
# Env (set by build.yml): EVENT, ACTION, REF, DEFAULT_BRANCH, BEFORE, GITHUB_SHA
set -uo pipefail
cd "$(dirname "$0")/.." || exit 1

run=true device=true lint=true reason=""

# Changed paths on stdin -> the ones that can affect the emulator job.
device_relevant() {
    local changed
    changed=$(cat)
    {
        echo "$changed" \
            | grep -vE '(^|/)src/test/|\.md$|^docs/|^LICENSE$|^config/detekt/|^config/compiler-warnings-baseline\.txt$' \
            | grep -vE '^(feature|widget)/[^/]+/src/main/(java|kotlin|res)/'
        echo "$changed" | grep -E '^feature/onboarding/src/main/'
    } | grep . | sort -u
}

case "$EVENT" in
    schedule)
        if [ "$(git log -1 --format=%ct)" -lt $(( $(date +%s) - 26 * 3600 )) ]; then
            run=false device=false lint=false reason="nightly: no commits in the last day, nothing to do"
        else
            reason="nightly: everything"
        fi
        ;;
    workflow_dispatch)
        case "${ACTION:-build}" in
            build | release) reason="manual run: everything" ;;
            *) run=false device=false lint=false reason="maintenance action '$ACTION' only" ;;
        esac
        ;;
    push)
        if [[ "$REF" == refs/tags/* ]]; then
            reason="tag: everything"
        elif [ "$REF" = "refs/heads/$DEFAULT_BRANCH" ]; then
            reason="push to $DEFAULT_BRANCH: everything"
        else
            lint=false
            zero=0000000000000000000000000000000000000000
            if [ -z "${BEFORE:-}" ] || [ "$BEFORE" = "$zero" ] \
                || ! git fetch -q --depth=1 origin "$BEFORE" 2>/dev/null; then
                reason="branch push: previous commit unknown (new branch?), so device tests run"
            else
                relevant=$(git diff --name-only "$BEFORE" "$GITHUB_SHA" | device_relevant)
                if [ -n "$relevant" ]; then
                    reason="branch push: device tests run for $(echo "$relevant" | head -3 | paste -sd ' ')"
                else
                    device=false
                    reason="branch push: nothing a fresh install or the APK build depends on changed"
                fi
            fi
        fi
        ;;
    *) reason="$EVENT: everything" ;;
esac

echo "run=$run"
echo "device=$device"
echo "lint=$lint"
echo "$reason (run=$run device=$device lint=$lint)" >&2
