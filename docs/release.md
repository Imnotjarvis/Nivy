# Releasing

## Versions

An APK's version comes from the commit it's built from (`app/build.gradle.kts`): the version
name is the commit's date and time, and the version code the minutes since 2024-01-01. A later
commit's APK therefore always installs over an earlier one's, wherever either was built.

## A signed APK on your machine

Release signing reads `keystore.properties` at the repository root. It and `release.jks` are in
`.gitignore`, so neither can be committed.

```properties
storeFile=release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

```bash
./gradlew :app:assembleRelease
```

The APK is in `app/build/outputs/apk/release/`. The `demo` build type is the same R8-minified
build signed with the committed debug key (`./gradlew :app:assembleDemo`), for trying a release
build without the real key.

## A release on GitHub

1. Add the four repository secrets once (Settings > Secrets and variables > Actions), described
   at the top of `.github/workflows/build.yml`: `RELEASE_KEYSTORE_BASE64` (`base64 -w0
   release.jks`), `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD`. Use
   the same `release.jks` as your local builds, so CI's APKs and yours update each other.
2. Tag the commit and push the tag:

   ```bash
   git tag v2026.11.01 && git push origin v2026.11.01
   ```

   The Build workflow runs every job on the tag, then its `release` job builds the signed APK
   and publishes a GitHub release with it and generated notes. Actions > Build > Run workflow >
   `release` builds the signed APK as an artifact only, without a GitHub release.

## Checking the signature

```bash
apksigner verify --print-certs app-release.apk
keytool -list -v -keystore release.jks
```

The SHA-256 digests the two print must match. Android refuses to update an installed app with an
APK signed by another key, so a mismatch shows up as a failed update.

The `release` job builds the APK again from the tagged commit; it isn't the very file the
`device` job ran on the emulator.
