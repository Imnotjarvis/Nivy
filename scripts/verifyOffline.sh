#!/bin/bash
# Fails if Firebase / Google Play / GitHub API / CI / JitPack wiring is still in the build.
# The exchange-rate API (currency-api) is the only network dependency the app may keep, so
# network access (INTERNET permission, Ktor) may only appear in shared:data:core.
set -u
cd "$(dirname "$0")/.." || exit 1

SRC_DIRS="app buildSrc config/detekt/config.yml feature shared widget gradle settings.gradle.kts build.gradle.kts"
fail=0

# check LABEL PATTERN [ALLOWED]: fails on any match, except in paths matching the ALLOWED regex.
check() {
    local label="$1" pattern="$2" allowed="${3:-^$}"
    local hits
    # shellcheck disable=SC2086
    hits=$(grep -rnIiE "$pattern" $SRC_DIRS \
        --include='*.kt' --include='*.kts' --include='*.toml' --include='*.xml' \
        --include='*.json' --include='*.yml' --include='*.pro' \
        2>/dev/null | grep -v '/build/' | grep -vE "$allowed")
    if [ -n "$hits" ]; then
        echo "FAIL [$label]"
        echo "$hits" | head -5 | sed 's/^/       /'
        fail=1
    else
        echo "ok   [$label]"
    fi
}

absent() {
    local label="$1"; shift
    local present=""
    for p in "$@"; do [ -e "$p" ] && present="$present $p"; done
    if [ -n "$present" ]; then
        echo "FAIL [$label] still present:$present"
        fail=1
    else
        echo "ok   [$label]"
    fi
}

check  "firebase"   'firebase|crashlytics|google-services|google\.services|com\.google\.gms'
check  "play-core"  'com\.google\.android\.play|play-services'
check  "github-api" 'api\.github\.com'
check  "jitpack"    'jitpack|detekt-explicit|ivy[.-]explicit|IvyExplicit'
check  "ci-wiring"  'ci-actions|wrapperUpgrade|wrapper-upgrade'
check  "network-boundary" 'android\.permission\.INTERNET|^\s*(api|implementation)\(libs\.bundles\.ktor|import io\.ktor\.' \
    '^(shared/data/core/|[^:]*/src/(test|androidTest)/)'
absent "removed-paths" app/google-services.json ci-actions fastlane Gemfile Gemfile.lock \
    feature/contributors feature/releases feature/attributions \
    buildSrc/src/main/kotlin/ivy.script.gradle.kts

exit $fail
