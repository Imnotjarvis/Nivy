#!/bin/bash
# Prints the paths a workflow run's commits changed, one per line, so a job can decide whether
# the change can affect it. Prints "everything" when they can't be known: a new branch or a
# force-push (no previous head), a schedule or a manual run.
# Pull requests are read through GitHub (gh needs GH_TOKEN); pushes are diffed against the
# previous head. Env, set by the workflow: EVENT, PR, BEFORE, GITHUB_SHA, GH_TOKEN.
set -euo pipefail

case "${EVENT:-}" in
    pull_request)
        gh pr diff "$PR" --name-only
        ;;
    push)
        zero=0000000000000000000000000000000000000000
        if [ -z "${BEFORE:-}" ] || [ "$BEFORE" = "$zero" ] \
            || ! git fetch -q --depth=1 origin "$BEFORE" 2>/dev/null; then
            echo everything
        else
            git diff --name-only "$BEFORE" "$GITHUB_SHA"
        fi
        ;;
    *)
        echo everything
        ;;
esac
