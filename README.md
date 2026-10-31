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
docs/         plans for the changes made to the original code
.github/      CI, CodeQL and dependency workflows, Dependabot
```

## Build

```bash
./gradlew :app:assembleDebug
```
