# Conventions

How the code is laid out, and the rules the checks enforce or that keep numbers consistent.

## Modules

`app` → `feature/*` → `widget/*` → `shared:ui:components` → `shared:ui:navigation` →
`shared:ui:core` → `shared:domain` → `shared:data:core` → `shared:data:model` → `shared:base`.
A module only depends on those to its right, and features don't depend on each other, with two
exceptions:
- `feature:main` shows `feature:home` and `feature:accounts` as its two tabs.
- `feature:import-data` uses `feature:onboarding`, its view model and toolbar: an import made
  during onboarding finishes it, or is skipped.

The rules are in the root `build.gradle.kts`, checked by `./gradlew :assertModuleGraph`, and
allow only these two links.

## Where code goes

- **Data access:** repositories in `shared:data:core` (`com.nivy.data.repository`), over Room's
  DAOs.
- **Logic:** use cases, `<Name>UseCase` classes with named suspend functions, in
  `com.nivy.domain.usecase.<area>` (shared) or in a feature's own `usecase` package. A use case
  takes `DispatchersProvider` rather than using `Dispatchers.IO` itself, so tests run it on
  `TestDispatchersProvider`. The older `*Act` classes are gone; don't bring the pattern back.
- **Writes:** a change of several rows is one database transaction, `DatabaseTransaction.run { }`,
  in the use case or repository that makes it: cut short, none of it stays.
- **Screens:** a `ComposeViewModel` per screen, its state and events next to it.
- **UI pieces:** a dialog or component that one feature uses lives in that feature;
  `shared:ui:components` holds what two or more use.

## Names that stay Ivy Wallet's

The code is in `com.nivy`, but what a phone stores by name keeps the name Ivy Wallet gave it, or
an update would lose it: the app ID (`com.ivy.wallet.offline`), the database, DataStore and
preferences file names, the add-transaction intent action, and the six classes the system finds by
class name (`RootActivity`, `PaymentTileService`, the three widget receivers and
`TransactionReminderWorker`). The manifest writes those classes' names in full.

## One rule per number

- **No exchange rate:** a total leaves out an amount it can't convert, counting it as 0. Never
  add amounts in different currencies (`ExchangeUseCase`'s documentation).
- **Loan progress:** `Loan.progress(records)`, for the loans list and the loan's page alike.
  Interest records stay out of the loan's total.
- **An account's balance:** the database sums it (`TransactionDao.balance`). Its copy in
  `FakeTransactionDao` passes the same `BalanceCases` as the query.
- **Many amounts in the base currency:** `BaseCurrencyRates`, which reads each currency's rate
  once and leaves out what has no rate, as above.
- **Spending stats:** which purchases a habit tile counts, what's fixed, and which days are
  tracked or without spending are in `Spending.kt` (`shared:domain`). The net worth screen's month
  and Trends both count by them; `MonthAndTrendsAgreeTest`, in `app`, puts one month through both.

## Layout and copy

- **Edges:** blocks (cards, list rows, bars) keep 16 dp from the screen's sides. Page text and the
  round buttons at a header's ends keep 24 dp, and so does everything in a dialog: its title,
  labels, fields and controls.
- **Spacing:** on the 4 dp grid, except a value that centers or aligns something exactly.
- **Corners:** one radius, the corner radius setting (`AppCornerRadius`), which every shape reads.
  A block close inside another one's corners, like a switch's selected half, takes
  `nestedShape(inset)`: the radius less the gap, so the corners stay concentric.
- **Amounts:** the currency code after the number, everywhere: `amountAndCode` writes both, and
  `formatAmount` the number alone. A small label over an amount is in regular weight and the
  secondary grey, so the amount leads.
- **Copy:** sentence case, no emoji or exclamation marks, and no final period on a label, a
  caption or a one-sentence hint.

## Files

Keep a file under about 600 lines; none is over 800. Split a screen by part, in the same
package: the entry point and layout, its sections, its dialogs, and its previews
(`...Previews.kt`). What another file of the split uses becomes `internal`.

## Errors

- Detekt flags a swallowed exception and a catch of `Exception` or `Throwable`. Catch what can
  be thrown. Where any failure has to end the same way (a result, a message), keep the catch-all
  with the reason beside it, and rethrow `CancellationException` in coroutines.
- A failure of something the user did (a save, an import, a download) is shown, not only
  logged. A view model writes what the person asked for through `UserWrites.run(message) { }`:
  when the database refuses, it shows the message, and the screen stays as it was.

## Checks

- Kotlin compiler warnings fail the build, detekt allows no issue, and lint fails on errors.
- Unit tests: JUnit 4 with kotest matchers and MockK, named in backticks, with `// given`,
  `// when` and `// then`. Fake DAOs live in `shared:data:core-testing`.
- Screenshot tests (Paparazzi) cover light, dark and minimal colors. After a UI change, record
  their reference images on CI (Actions > Build > Run workflow > `record-screenshots`, on your
  branch). The run's `screenshots` artifact holds the changes as a patch: apply it at the
  repository root with `git apply screenshots.patch`, then commit and push. CI never pushes
  itself: a pull request only counts the checks of a push like yours.
- Instrumented tests (`src/androidTest` in `shared:data:core` and `shared:domain`) cover what
  needs a real database or real files: migrations, balances, backups.
- `main` takes changes through pull requests only, squash-merged, once these checks pass:
  `checks`, `build`, `test`, `demo`, `device`, `lint`, `Analyze (java-kotlin)`, `submit` and
  `review`.
