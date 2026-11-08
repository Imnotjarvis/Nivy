#!/usr/bin/env bash
# Gets Gradle for the wrapper before a CI job's first build, trying again when the download fails.
#
# The wrapper downloads Gradle from GitHub's release servers, which sometimes fail a request (a 504,
# or no data for a minute) and serve the next one, and it tries only once. When the job restored
# the Gradle caches, Gradle is already there and this returns at once; it downloads when they
# didn't, as on the first run after a build file changes.
set -uo pipefail

for attempt in 1 2 3 4 5; do
    if ./gradlew --version > /dev/null; then
        exit 0
    fi
    echo "::warning::Gradle's download failed (attempt $attempt of 5)."
    if [ "$attempt" -lt 5 ]; then
        sleep $((attempt * 20))
    fi
done
exit 1
