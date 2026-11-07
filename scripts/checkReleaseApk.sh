#!/bin/bash
# Checks the signed release APK before anything uses it: its signature, and which key made it,
# against the certificate the repository expects (the RELEASE_CERT_SHA256 variable, given as
# $EXPECTED). An APK signed with another key can't update the installed app, so a different
# certificate fails. With no expected certificate set yet, it only warns and names the one it
# found, for the variable.
# Prints version=, sha256= and cert= lines, for $GITHUB_OUTPUT.
# Usage: EXPECTED=<certificate SHA-256> scripts/checkReleaseApk.sh path/to/app-release.apk
set -euo pipefail

apk=$1
tools=$(ls -d "${ANDROID_HOME:?}"/build-tools/*/ | sort -V | tail -1)

# Fails when the APK isn't signed, or its signature doesn't match its contents.
"$tools/apksigner" verify "$apk" >&2
cert=$("$tools/apksigner" verify --print-certs "$apk" | sed -n 's/.*certificate SHA-256 digest: //p' | head -1)
version=$("$tools/aapt2" dump badging "$apk" | sed -n "s/.*versionName='\([^']*\)'.*/\1/p")

# keytool prints the digest as AB:CD:..., apksigner as abcd...
normalized() { tr -d ': ' | tr 'A-F' 'a-f'; }
if [ -z "${EXPECTED:-}" ]; then
    echo "::warning::RELEASE_CERT_SHA256 isn't set, so the signing key wasn't checked. If $cert is your release key's certificate, set the variable to it." >&2
elif [ "$(normalized <<< "$EXPECTED")" != "$(normalized <<< "$cert")" ]; then
    echo "::error::The APK is signed with certificate $cert, not $EXPECTED: it couldn't update the installed app." >&2
    exit 1
fi

echo "version=$version"
echo "sha256=$(sha256sum "$apk" | cut -d ' ' -f 1)"
echo "cert=$cert"
