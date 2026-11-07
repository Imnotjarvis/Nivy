# Conventions

How the code is laid out, and the rules the checks enforce or that keep numbers consistent.

## Modules

`app` → `feature/*` → `widget/*` → `shared:ui:components` → `shared:ui:navigation` →
`shared:ui:core` → `shared:domain` → `shared:data:core` → `shared:data:model` → `shared:base`.
A module only depends on those to its right, and features never depend on each other. The rules
are in the root `build.gradle.kts`, checked by `./gradlew :assertModuleGraph`.

## Where code goes

- **Data access:** repositories in `shared:data:core` (`com.ivy.data.repository`), over Room's
  DAOs.
- **Logic:** use cases, `<Name>UseCase` classes with named suspend functions, in
  `com.ivy.domain.usecase.<area>` (shared) or in a feature's own `usecase` package. A use case
  takes `DispatchersProvider` rather than using `Dispatchers.IO` itself, so tests run it on
  `TestDispatchersProvider`. The older `*Act` classes are gone; don't bring the pattern back.
- **Screens:** a `ComposeViewModel` per screen, its state and events next to it.

## One rule per number

- **No exchange rate:** a total leaves out an amount it can't convert, counting it as 0. Never
  add amounts in different currencies (`ExchangeUseCase`'s documentation).
- **Loan progress:** `Loan.progress(records)`, for the loans list and the loan's page alike.
  Interest records stay out of the loan's total.
- **An account's balance:** the database sums it (`TransactionDao.balance`). Its copy in
  `FakeTransactionDao` passes the same `BalanceCases` as the query.

## Files

Keep a file under about 600 lines; none is over 800. Split a screen by part, in the same
package: the entry point and layout, its sections, its dialogs, and its previews
(`...Previews.kt`). What another file of the split uses becomes `internal`.

## Errors

- Detekt flags a swallowed exception and a catch of `Exception` or `Throwable`. Catch what can
  be thrown. Where any failure has to end the same way (a result, a message), keep the catch-all
  with the reason beside it, and rethrow `CancellationException` in coroutines.
- A failure of something the user did (a save, an import, a download) is shown, not only
  logged.

## Checks

- Kotlin compiler warnings fail the build, detekt allows no issue, and lint fails on errors.
- Unit tests: JUnit 4 with kotest matchers and MockK, named in backticks, with `// given`,
  `// when` and `// then`. Fake DAOs live in `shared:data:core-testing`.
- Screenshot tests (Paparazzi) cover light, dark and minimal colors. After a UI change, record
  their reference images on CI (Actions > Build > Run workflow > `record-screenshots`, on your
  branch, not on `main`), which commits them and builds the result.
- Instrumented tests (`src/androidTest` in `shared:data:core` and `shared:domain`) cover what
  needs a real database or real files: migrations, balances, backups.
- `main` takes changes through pull requests only, squash-merged, once these checks pass:
  `checks`, `build`, `test`, `demo`, `device`, `lint`, `Analyze (java-kotlin)`, `submit` and
  `review`.
