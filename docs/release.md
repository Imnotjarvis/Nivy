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

## A signed APK from CI

Nothing is published: no GitHub release, no store. CI builds the signed APK, tests that very file,
and leaves it to download.

1. Once, add the four repository secrets described at the top of `.github/workflows/build.yml`
   (Settings > Secrets and variables > Actions > Secrets): `RELEASE_KEYSTORE_BASE64` (`base64 -w0
   release.jks`), `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS` and `RELEASE_KEY_PASSWORD`. Use
   the same `release.jks` as your local builds, so CI's APKs and yours update each other.
2. Once, add the variable `RELEASE_CERT_SHA256` (the same page, Variables): your key's certificate
   SHA-256, which `keytool -list -v -keystore release.jks` prints. The first signed run without it
   prints it in its summary too. From then on, an APK signed with any other key stops the run:
   Android wouldn't let it update the installed app.
3. Run Actions > Build > Run workflow > `release`, or push a tag:

   ```bash
   git tag v2026.11.01 && git push origin v2026.11.01
   ```

   The `demo` job builds the APK with the release key and checks the key, the `device` job installs
   that file on the emulator and goes through onboarding to Home, and the `release` job keeps it
   as the run's `Nivy-<version>` artifact for a day. The run summary gives its SHA-256.
4. On the phone, open the run on GitHub, download the artifact (a zip), unzip it in the files app
   and open the APK. Android asks once to allow installing from the files app.

## Checking the signature

```bash
apksigner verify --print-certs Nivy-2026.11.01.apk
keytool -list -v -keystore release.jks
```

The SHA-256 digests the two print must match. CI does this itself, against
`RELEASE_CERT_SHA256` (`scripts/checkReleaseApk.sh`).
