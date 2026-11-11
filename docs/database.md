# The database

Nivy keeps everything in one Room database, `ivywallet.db`, defined by `NivyRoomDatabase`
(`shared/data/core/src/main/java/com/nivy/data/db/`). It's at version 137. Versions up to 132 come
from Ivy Wallet; Nivy's own start at 133.

The oldest database the migrations upgrade is version 106: Ivy Wallet's schemas before it don't
match what its migration from 105 assumed, so there was never a working path from them, and
`migrateAll` in `NivyRoomDatabaseMigrationTest` starts there.

## Changing the schema

1. Change the entities and raise `version` in `@Database`.
2. Prefer an `AutoMigration(from = N, to = N + 1)` in `autoMigrations`, with a comment saying
   what the version adds. Room works it out from the two exported schemas.
3. When Room can't (a rename, data moved between tables), write a `MigrationNtoM_What` class in
   `db/migration/` and add it to `NivyRoomDatabase.migrations()`.
4. Build once: Room exports the new schema to `shared/data/core/schemas/`. Commit that file. CI's
   "Room schemas are committed" step fails when a build changes or adds one, and the migration
   tests read them from there.
5. Extend `Migration132to137DataTest` (in `src/androidTest`, renamed for the new version). It
   writes real rows at version 132, runs every migration and checks the rows and the balances on
   the other side. Add rows for what the new version touches.
6. A new table or column usually belongs in backups too: see [backup-format.md](backup-format.md).

The app's database is opened with every migration (`addMigrations(*NivyRoomDatabase.migrations())`),
so a missing one fails the instrumented tests rather than wiping anyone's data.

## Data migrations

Changes that aren't about the schema (a setting to rewrite, files to tidy) go in
`app/src/main/java/com/nivy/wallet/migrations/`: a `Migration` with a key, listed in
`MigrationsManager`. They run two seconds after launch. Each is marked done in the preferences
only once it succeeds, so one that fails runs again on the next launch. Write each one so that
running it twice does no harm: deleting all data clears the preferences, and the marks with them,
so the data migrations run again on the next launch.

One of them drops the `settings` table, where versions before DataStore kept the settings. The
schema no longer has it, but a database that had it kept it, for `LegacySettingsMigration`,
DataStore's own, to copy its values on the first read; the data migrations start after that read.
That the values were copied is the one mark deleting all data keeps: they aren't to come back.
