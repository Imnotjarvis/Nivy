package com.nivy.data.preferences

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nivy.base.TestDispatchersProvider
import com.nivy.data.db.NivyRoomDatabase
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The old settings table is dropped from a real database, and dropping it again does no harm. */
@RunWith(AndroidJUnit4::class)
class LegacySettingsTableTest {
    private lateinit var db: NivyRoomDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NivyRoomDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun theTableGoesAndDroppingItAgainDoesNoHarm(): Unit = runBlocking {
        // given: the table as an old database has it, outside the schema
        db.openHelper.writableDatabase.execSQL("CREATE TABLE settings (theme TEXT, currency TEXT)")
        db.openHelper.writableDatabase.execSQL("INSERT INTO settings VALUES ('DARK', 'EUR')")
        settingsTables() shouldBe 1
        val table = LegacySettingsTable(db, TestDispatchersProvider)

        // when
        table.drop()

        // then
        settingsTables() shouldBe 0

        // when: again, as a data migration may run twice
        table.drop()

        // then
        settingsTables() shouldBe 0
    }

    private fun settingsTables(): Int = db.openHelper.readableDatabase
        .query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'settings'")
        .use { it.count }
}
