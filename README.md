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
docs/         notes: installing, the database, the backup format, conventions
.github/      CI, CodeQL and dependency workflows, Dependabot
```

## Build

```bash
./gradlew :app:assembleDebug
```

`./gradlew :app:assembleDemo` builds the APK to install: R8-minified and signed with the committed
debug key. CI builds and tests it on every push to `main` ([docs/install.md](docs/install.md)).

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

- [Installing](docs/install.md): where to get the APK, versions, building it yourself.
- [The database](docs/database.md): changing the schema, migrations and their tests.
- [The backup format](docs/backup-format.md): what a backup holds and how it imports.
- [Conventions](docs/conventions.md): where code goes, one rule per number, errors, tests.
