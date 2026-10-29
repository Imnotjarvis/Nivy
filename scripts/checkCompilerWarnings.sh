#!/bin/bash
# Fails when the Kotlin compiler reported a warning that config/compiler-warnings-baseline.txt
# doesn't list, so new warnings can't slip in unnoticed. Deprecation warnings are not checked:
# the legacy modules use deprecated APIs on purpose (about 2,300 of them).
# Only modules Gradle actually compiled appear in the log; modules taken from the build cache
# print nothing and can't fail this check.
# Usage: scripts/checkCompilerWarnings.sh path/to/gradle-output.log
set -euo pipefail
LOG=$(realpath "$1")
cd "$(dirname "$0")/.." || exit 1
BASELINE=config/compiler-warnings-baseline.txt

# "w: file:///abs/repo/dir/File.kt:12:5 message" -> "dir/File.kt:12:5 message"
warnings=$(grep -E '^w: file://' "$LOG" | sed "s#^w: file://$PWD/##" \
    | grep -vE 'is deprecated|which is deprecated|overrides a deprecated member' | sort -u || true)

new=0
while IFS= read -r line; do
    [ -z "$line" ] && continue
    location=${line%% *}
    message=${line#* }
    file=${location%%:*}
    # The baseline ignores line numbers, so unrelated edits don't invalidate it.
    if ! grep -qxF "$file $message" "$BASELINE"; then
        position=${location#"$file":}
        echo "::error file=$file,line=${position%%:*},col=${position#*:}::${message//%/%25}"
        new=$((new + 1))
    fi
done <<< "$warnings"

checked=$(echo "$warnings" | grep -c . || true)
if [ "$new" -gt 0 ]; then
    echo "FAIL $new new compiler warning(s). Fix them, or add the lines above (as \"path message\") to $BASELINE."
    exit 1
fi
echo "ok   $checked compiler warning(s), all already in $BASELINE (deprecations not checked)"
