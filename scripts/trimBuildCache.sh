#!/usr/bin/env bash
# Keeps the newest 750 MB of Gradle's build cache in $GRADLE_BUILD_CACHE_DIR, before a CI job saves
# it for the next runs.
#
# GitHub keeps 10 GB of Actions caches per repository and drops the least recently used beyond that.
# Four jobs (build, demo, test, lint) each save a build cache per commit. Kept for a week, each had
# grown to about 3.3 GB, so a run saved more than the whole allowance and pushed out what it was
# still using: the Gradle caches (Gradle itself, the dependencies) and the emulator's system image.
# The jobs that started after then downloaded them again, from servers that sometimes time out. At
# 750 MB each, what a run restores and saves (6 GB at most) leaves room for both.
#
# Gradle writes an entry when it stores it and doesn't touch it when it reuses it, so the newest are
# the ones stored last. A run adds tens of MB, so the cap holds many runs' worth; an entry still in
# use that drops out is stored again by the next build that needs it.
set -euo pipefail

dir="${GRADLE_BUILD_CACHE_DIR:?GRADLE_BUILD_CACHE_DIR is not set}"
limit_bytes=$((750 * 1024 * 1024))

[ -d "$dir" ] || exit 0
cd "$dir"
find . -maxdepth 1 -type f -printf '%T@ %s %f\n' \
    | sort -rn \
    | awk -v limit="$limit_bytes" '{ total += $2 } total > limit { print $3 }' \
    | xargs -r rm -f --
