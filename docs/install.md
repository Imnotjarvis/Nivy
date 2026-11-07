# Installing Nivy

## The APK to install

CI builds it on every push to `main`, every night with new commits, and on a manual run: the
**demo** build. It's the app as it ships, R8-minified and not debuggable, named Nivy, and the same
run installs it on an emulator and goes through onboarding to Home. Nothing is published anywhere.

1. On GitHub, open Actions > Build, and the latest run on `main`.
2. On its Summary page, under Artifacts, download **apk-demo**: a zip, kept for 3 days.
3. On the phone, unzip it in the files app and open `app-demo.apk`. Android asks once to allow
   installing from the files app.

No recent run? Actions > Build > Run workflow, on `main`, with "build".

It's signed with the debug key committed to the repository (`debug.jks`), as the debug build is,
so both install as the same app, `com.ivy.wallet.offline.debug`, and update each other. That key
is public: someone else could build an APK that installs over yours, so only install APKs from this
repository's runs.

## Versions

An APK's version comes from the commit it's built from (`app/build.gradle.kts`): the version name
is the commit's date and time, and the version code the minutes since 2024-01-01. A later commit's
APK therefore always installs over an earlier one's, wherever either was built.

## Building it yourself

`./gradlew :app:assembleDemo` builds the same APK into `app/build/outputs/apk/demo/`.

The `release` build type is still there for an APK signed with a key of your own. It reads
`keystore.properties` at the repository root; it and `release.jks` are in `.gitignore`:

```properties
storeFile=release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

`./gradlew :app:assembleRelease` then builds it into `app/build/outputs/apk/release/`. It installs as
`com.ivy.wallet.offline`, a separate app from the demo build: move your data with a backup.
