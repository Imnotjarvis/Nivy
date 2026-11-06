# The backup format

Backups are the only way to move data between phones: the app opts out of Android's own backups
(`allowBackup="false"`). The code is `BackupDataUseCase` (`shared/data/core/.../data/backup/`).

## The file

Settings > Backup data writes a zip holding one JSON file, encoded as UTF-16. Importing takes that
zip or the JSON file itself; a zip has to hold exactly one `.json` file and unzip to at most
256 MB.

The JSON is one object, `IvyWalletCompleteData`. Every key is optional and an absent one reads as
empty, so a backup from an older version imports, and an older version can import a newer backup
(it ignores the keys it doesn't know).

| Key | Holds |
|---|---|
| `accounts`, `categories`, `transactions`, `budgets`, `loans`, `loanRecords`, `plannedPaymentRules`, `tags`, `tagAssociations`, `creditCards`, `accountValues` | The database's rows, field for field (the entities' `@SerialName`s). |
| `settings` | One entry: `theme`, `currency` (the base currency), `bufferAmount` (the savings goal) and `name`. |
| `sharedPrefs` | A few display settings as strings, under their old SharedPreferences names: notifications, hiding the balance, transfers as income and expense, and the app lock. |

In the rows, IDs are UUID strings and times (`Instant` and `LocalDateTime` alike) are epoch
milliseconds. Tags keep Ivy Wallet's key names, `creationTime` and `lastSyncTime`.

## Importing

- Every write happens in one database transaction: an import that fails leaves the wallet as it
  was.
- Rows an older version marked as deleted (`isDeleted`, for its cloud sync) are left out.
- An account or category with the same name as one already in the app (and, for an account, the
  same currency) is merged into it.
- A backup from before credit cards had their own table gets one card per credit-card account, as
  the database migration does (`LegacyCreditCards.kt`).
- The settings are applied once the data is in. The app lock never is: a backup made by someone
  else, or one without the key, mustn't switch it off.

## Changing the format

- Give every new field a default value, so older backups still decode, and never change an
  existing `@SerialName`.
- Add the new data to the test backup `backups/nivy-2026-11.json`, which `shared/data/core` keeps
  twice (in `src/test/resources` and `src/androidTest/assets`), with its expected effect in
  `BackupDataUseCaseTest`. That test
  imports the file, exports it again and compares both, field by field;
  `BackupDataUseCaseAndroidTest` does the same on a device, through a real database and real
  files.
