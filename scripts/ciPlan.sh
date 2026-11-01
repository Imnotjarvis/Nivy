#!/bin/bash
# Decides which CI jobs a run needs, so a push only pays for what its changes can affect.
# Prints "run=", "test=", "device=" and "lint=" lines (for $GITHUB_OUTPUT) and a reason on stderr.
#   run     checks and build (false only for maintenance actions and idle nightlies)
#   test    unit and screenshot tests
#   device  the R8-minified demo APK and the emulator job (startup crashes, R8 rules, Room, Hilt)
#   lint    Android Lint
# A push that only changes docs runs nothing. Branch pushes skip the device work when they only
# change code or resources of features a fresh install never opens (everything but onboarding)
# or widgets, or unit tests.
# Pushes to claude/experimental-* branches (long refactors that push often) run only checks and
# the build, which then also compiles the unit tests; a manual run on the branch runs everything.
# Tags, pushes to the default branch, nightly and manual runs always run everything.
# Env (set by build.yml): EVENT, ACTION, REF, DEFAULT_BRANCH, BEFORE, GITHUB_SHA
set -uo pipefail
cd "$(dirname "$0")/.." || exit 1

run=true test=true device=true lint=true reason=""
DOCS='\.md$|^docs/|^LICENSE$'

# Changed paths on stdin -> the ones that can affect the emulator job.
device_relevant() {
    local changed
    changed=$(cat)
    {
        echo "$changed" \
            | grep -vE '(^|/)src/test/|\.md$|^docs/|^LICENSE$|^config/detekt/' \
            | grep -vE '^(feature|widget)/[^/]+/src/main/(java|kotlin|res)/'
        echo "$changed" | grep -E '^feature/onboarding/src/main/'
    } | grep . | sort -u
}

case "$EVENT" in
    schedule)
        if [ "$(git log -1 --format=%ct)" -lt $(( $(date +%s) - 26 * 3600 )) ]; then
            run=false test=false device=false lint=false reason="nightly: no commits in the last day, nothing to do"
        else
            reason="nightly: everything"
        fi
        ;;
    workflow_dispatch)
        case "${ACTION:-build}" in
            build | release) reason="manual run: everything" ;;
            *) run=false test=false device=false lint=false reason="maintenance action '$ACTION' only" ;;
        esac
        ;;
    push)
        # The changed paths, or "everything" for a new branch or a force-push.
        changed=$(scripts/changedPaths.sh || echo everything)
        if [[ "$REF" == refs/tags/* ]]; then
            reason="tag: everything"
        elif [ "$changed" != everything ] && ! grep -qvE "$DOCS" <<< "$changed"; then
            run=false test=false device=false lint=false
            reason="push: only docs changed, nothing to build"
        elif [ "$REF" = "refs/heads/$DEFAULT_BRANCH" ]; then
            reason="push to $DEFAULT_BRANCH: everything"
        elif [[ "$REF" == refs/heads/claude/experimental-* ]]; then
            test=false device=false lint=false
            reason="experimental branch push: checks and build only (a manual run runs everything)"
        else
            lint=false
            if [ "$changed" = everything ]; then
                reason="branch push: previous commit unknown (new branch?), so device tests run"
            else
                relevant=$(device_relevant <<< "$changed")
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
echo "test=$test"
echo "device=$device"
echo "lint=$lint"
echo "$reason (run=$run test=$test device=$device lint=$lint)" >&2
