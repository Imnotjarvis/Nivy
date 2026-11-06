# Nivy

A very low effort spiritual successor to [Ivy Wallet](https://github.com/Ivy-Apps/ivy-wallet).
It is the same Android money manager, made to run fully offline: no account, no Firebase and no
analytics. Its only network call fetches exchange rates.

Licensed under GPL-3.0, like the original (see [LICENSE](LICENSE)).

## Project structure

```
app/          the application
feature/      one module per screen (accounts, budgets, transactions, settings, ...)
widget/       home screen widgets
shared/
  base/       common utilities
  data/       Room database, repositories and data models
  domain/     business logic
  ui/         design system, navigation and shared components
buildSrc/     Gradle convention plugins
gradle/       version catalog and wrapper
config/       detekt rules
scripts/      CI helper scripts
docs/         notes: releasing, the database, the backup format, conventions
.github/      CI, CodeQL and dependency workflows, Dependabot
```

## Build

```bash
./gradlew :app:assembleDebug
```

Two more builds: `./gradlew :app:assembleDemo` is the R8-minified build signed with the committed
debug key, and `./gradlew :app:assembleRelease` is signed with your own key
([docs/release.md](docs/release.md)).

## Checks

```bash
./gradlew testDebugUnitTest
./gradlew :detekt :assertModuleGraph --no-configuration-cache
./gradlew :app:lintDebug
```

The instrumented tests need a device or an emulator:
`./gradlew :shared:data:core:connectedDebugAndroidTest :shared:domain:connectedDebugAndroidTest`.
CI runs all of these, and the top of [.github/workflows/build.yml](.github/workflows/build.yml)
says what runs when.

## Notes

- [Releasing](docs/release.md): versions, signing, publishing a release.
- [The database](docs/database.md): changing the schema, migrations and their tests.
- [The backup format](docs/backup-format.md): what a backup holds and how it imports.
- [Conventions](docs/conventions.md): where code goes, one rule per number, errors, tests.
