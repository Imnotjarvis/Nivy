# Material3 and data-layer migration (experimental)

Branch: `claude/experimental-m3-migration`, started from `main` at b16dc23. It is not meant to be merged without review.

## Goal

Finish the migration the upstream project started and never completed:

- Every screen and component uses Material3 and the design system in `shared/ui/core` (`com.ivy.design.system`), not the old design system in `temp:old-design`.
- Every feature uses the data models in `shared/data/model` and the repositories in `shared/data/core`, not the legacy models in `temp:legacy-code`.
- The two `temp` modules are gone, and their remaining code lives in proper modules.
- The build has **zero compiler warnings** (Kotlin and Java, generated code included), and any new warning fails the build.

## Definition of done

1. `./gradlew assembleDebug` prints no `w:` line, with `allWarningsAsErrors` on in every module.
2. CI is green on a full manual run: checks, build, unit tests, screenshot tests, lint, emulator smoke test and instrumented tests.
3. The screens look the same as before. The screenshot tests' reference images are re-recorded only after a side-by-side review of old and new images.
4. Existing installs keep their data: the database schema stays compatible, and backups exported from `main` still import.

## Design language

The Ivy look is carried into Material3, not replaced by Material's defaults:

| Old (`temp:old-design`) | New (`shared/ui/core`) |
|---|---|
| `UI.colors.pure` / `pureInverse` / `gray` / `medium` / `primary` | `MaterialTheme.colorScheme` roles, with the same color values |
| Color constants (`Ivy`, `Green`, `Red`, ...), used for screens and stored as user-picked account and category colors | `IvyColors` palette constants, with the same values |
| `UI.typo.h1` ... `c` (Raleway), `nH1` ... `nC` (Open Sans, for numbers) | `MaterialTheme.typography` roles in Raleway, plus number styles in Open Sans |
| `UI.shapes.r1` ... `r4`, `rFull` | `MaterialTheme.shapes`, plus the Ivy corner radii |
| `IvyButton`, `IvyModal`, `IvyIcon`, text fields, switches, ... | Components rebuilt on Material3 (`Button`, `Surface`, `Icon`, `TextField`, `Switch`, ...), styled to match |

## Data layer

New models exist for accounts, categories, transactions, tags and exchange rates. Budgets, loans, loan records, planned-payment rules and settings have only legacy models, so they get new models and repositories first. Room entities and the database schema don't change, except where a change comes with a tested Room migration.

## Phases

1. **CI for the branch.** Pushes run checks and the build only; the build also compiles unit tests and lists every compiler error and warning at the end of its log. Manual runs run everything.
2. **Third-party deprecations.** About 19 uses of deprecated Android and Compose APIs.
3. **Design system.** Port the tokens, then rebuild the components on Material3, then move every call site, then delete `temp:old-design`.
4. **Legacy helpers.** Time functions to the injected `TimeProvider` and `TimeConverter`, `SharedPrefs` to DataStore (keeping stored values), `IvyWalletCtx`, `stringRes`, the FRP helpers, `TestIdlingResource`.
5. **Data layer.** New models and repositories, then every feature off the legacy models.
6. **Dissolve `temp:legacy-code`.** Turn on `allWarningsAsErrors`.
7. **Review.** Full CI run, old and new screenshots side by side, write-up.

## Progress

Warning counts are from the build job's "List compiler errors and warnings" step. It only shows the modules a build recompiled; the 0 was confirmed by the build that turned `allWarningsAsErrors` on, which recompiled everything.

| Phase | State | Warnings after |
|---|---|---|
| 1. CI for the branch | Done | about 4,900 before any fix |
| 2. Third-party deprecations | Done | |
| 3. Design system | Done: tokens, components and call sites on Material3; `temp:old-design` deleted | 2,563 |
| 4. Legacy helpers | Done: `SharedPrefs` → `AppPreferences` (DataStore, keeps stored values), `stringRes` and the global `appContext`, the FRP `Action`/`then` library, `IvyWalletCtx` (split into `ThemeState`, `PeriodState`, `MainScreenState`, `LocalScreenSize` and `RootScreen` methods), the legacy clock and date helpers, `LogoutLogic` → `DeleteAllDataUseCase` | 996 before the last two items |
| 5. Data layer | Done: domain models and repositories for budgets, loans, loan records and planned-payment rules (schemas 131 and 132, with tested migrations); every feature on the domain `Account`, `Budget`, `Loan`, `PlannedPaymentRule` and `Transaction`; `ExchangeUseCase`, `TransactionAmountsUseCase`, `DueTransactionsUseCase`, `LoanTransactionsUseCase` and `PlannedPaymentsGenerator` replace the legacy logic classes; the legacy models and their mappers are deleted | 0 |
| 6. Dissolve `temp:legacy-code` | `allWarningsAsErrors` is on for every Kotlin compile, so a warning fails the build; that first build recompiled all 31 modules and their unit tests with none. The warning-baseline script is gone. Moving the module's code is not started | 0 |
| 7. Review | Not started | |

Behaviour changes found on the way, each a fix of an upstream bug:

- Home's "set currency" built its update but never ran it; it now runs.
- The legacy CSV importer shifted dates parsed with a custom format by the UTC offset twice.
- "Today", the current month and the overdue/upcoming split now use the device's time zone, like the date ranges they are compared with. They used UTC before.
- Loan dates were formatted as if they were UTC, though they are stored in local time.
- `AndroidResourceProvider` passed format arguments as a single array.
- A transfer's income in Reports and the pie chart was the amount that left the source account, in its currency; it's now what arrived.
- The CSV import's failed rows were added to an immutable list and dropped; they're reported again.
- Changing a loan's account to one in another currency left its records' converted amounts in the old currency.
- Planned payments of deleted accounts were only flagged as deleted, so they stayed in the database; deleting an account now deletes them, and migration 131→132 and backup import remove flagged rows.
- The planned payment editor crashed when the rule's account had been deleted.
- The transaction card drew its background over the click ripple.
- Category and account-transfer histories now show tags, like an account's history.
