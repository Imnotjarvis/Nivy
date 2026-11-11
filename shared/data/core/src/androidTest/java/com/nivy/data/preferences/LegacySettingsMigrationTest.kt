package com.nivy.data.preferences

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.datastore.preferences.core.emptyPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nivy.data.db.NivyRoomDatabase
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The copy of the old settings table into DataStore, read from a real database file. */
@RunWith(AndroidJUnit4::class)
class LegacySettingsMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val migration = LegacySettingsMigration(context)

    @Before
    @After
    fun deleteDatabase() {
        context.deleteDatabase(NivyRoomDatabase.DB_NAME)
    }

    @Test
    fun theOldSettingsAreCopiedAndMarkedAsCopied(): Unit = runBlocking {
        // given: a database file with the old settings table
        database { db ->
            db.execSQL(
                "CREATE TABLE settings (theme TEXT NOT NULL, currency TEXT NOT NULL, " +
                    "bufferAmount REAL NOT NULL, name TEXT NOT NULL)"
            )
            db.execSQL("INSERT INTO settings VALUES ('DARK', 'EUR', 2500.0, 'Ada')")
        }

        // when
        val settings = migration.migrate(emptyPreferences())

        // then
        settings[AppPreferences.ThemeName] shouldBe "DARK"
        settings[AppPreferences.BaseCurrency] shouldBe "EUR"
        settings[AppPreferences.BufferAmount] shouldBe 2500.0
        settings[AppPreferences.UserName] shouldBe "Ada"
        migration.shouldMigrate(settings) shouldBe false
    }

    @Test
    fun withoutADatabaseNothingIsCopied(): Unit = runBlocking {
        // when: a fresh install
        val settings = migration.migrate(emptyPreferences())

        // then
        settings.asMap() shouldBe mapOf(Pair(LegacySettingsMigration.Migrated, true))
    }

    @Test
    fun aDatabaseWithoutTheTableCopiesNothing(): Unit = runBlocking {
        // given: the table dropped, or never there
        database { db -> db.execSQL("CREATE TABLE accounts (id TEXT)") }

        // when
        val settings = migration.migrate(emptyPreferences())

        // then
        settings.asMap() shouldBe mapOf(Pair(LegacySettingsMigration.Migrated, true))
    }

    private fun database(fill: (SQLiteDatabase) -> Unit) {
        val file = context.getDatabasePath(NivyRoomDatabase.DB_NAME)
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use(fill)
    }
}
