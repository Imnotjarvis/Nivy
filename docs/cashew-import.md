# Importing from Cashew

Cashew keeps everything in one SQLite database. Its Settings > Backups > "Export Cashew Data File"
writes that database as `cashew-<date>.sql`, and Import > Cashew reads it. Its CSV export holds
far less (no transfers' pairs, nothing planned, no loans or budgets): the manual CSV import reads
it, as any CSV.

The code is in `feature/import-data`: `CashewReader.kt` reads the tables, `CashewPlanner.kt` (with
`CashewLoans.kt` and `CashewBudgets.kt`) decides what comes over, and `ImportCashewUseCase` writes
it in one database transaction: an import that fails leaves the wallet as it was.

## What comes over

| Cashew | Nivy |
|---|---|
| Wallets | Accounts, with their currency, color and order. One named as an account of the same currency lands on it. |
| Categories | Categories, one per name: Cashew's income and expense categories of the same name become one. |
| Subcategories | A tag named after the subcategory, on the main category's transactions. |
| Paid transactions | Incomes and expenses, with their time, title and note. |
| Transfers (two balance corrections, paired) | One transfer, with what left and what arrived, so between currencies too. |
| Balance corrections, starting balances | Incomes and expenses titled "Adjust balance", as the app's own adjustment. |
| Subscriptions and repeating payments | A planned payment from the next occurrence to pay; the paid ones stay linked to it. |
| Upcoming payments | One-time planned payments; paid ones stay linked as a paid planned payment's. |
| Lent and borrowed amounts | Loans, with the transaction that moved the money. Collected or settled in full: a loan paid back by a record with no transaction, as Cashew keeps neither when nor where. |
| Long-term loans | Loans: the first amount lent or borrowed, then records for more, and for what was paid back. |
| Goals | A tag named after the goal, on its transactions. |
| Monthly spending budgets | Budgets, with their categories and wallets, in the base currency at Cashew's rates. |
| Exchange rates set by hand | Rates entered by hand, for the accounts' currencies, unless one is there already. |

Into a wallet without accounts, the import also takes Cashew's main currency as the base currency,
as restoring a backup takes the backup's.

## What doesn't, and is listed on the result page

- Transactions without an amount, not paid, or of a wallet that isn't in the file.
- Planned transfers: Nivy plans incomes and expenses.
- Planned payments with a custom repeat. Those that end in Cashew come over, repeating until deleted.
- Budgets other than monthly spending ones, their limits per category, and budgets in a currency
  without a rate. A loan payment in such a currency comes over, counted without a rate.

Not imported, and not listed: shared budgets, scanner templates, the titles Cashew links to
categories, and its settings. Icons come over when Nivy has one of the same name or for the same
thing (`CashewLooks.kt`); emoji don't.

## Tests

The tests build real SQLite files with Cashew's tables (`CashewFile`, from the table definitions
in `src/test/resources/cashew/schema-*.sql`, written from Cashew's own schema dumps) and read them
with JDBC, as the app reads them with Android's SQLite. A file from an older Cashew reads without
the columns it didn't have yet. The files write rows as Cashew does: its `skip_paid` is set on every
ordinary transaction, and only means skipped on a planned one that isn't paid.

When Cashew's schema changes, add its tables as `schema-<version>.sql` and read a file of it in
`CashewReaderTest`.
