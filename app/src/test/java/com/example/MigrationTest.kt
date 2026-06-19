package com.example

import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteDatabase
import com.example.data.MIGRATION_5_6
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MigrationTest {

    @Test
    fun migration5To6_hasExpectedVersions() {
        assertEquals(5, MIGRATION_5_6.startVersion)
        assertEquals(6, MIGRATION_5_6.endVersion)
    }

    @Test
    fun migrate5To6_createsNewTablesAndSeedData() {
        val sqliteDb = SQLiteDatabase.createInMemory(null)
        sqliteDb.version = 5
        val db: SupportSQLiteDatabase = FrameworkSQLiteDatabase.wrap(sqliteDb)

        MIGRATION_5_6.migrate(db)

        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='app_notifications'").use { cursor ->
            assertTrue(cursor.moveToFirst())
        }
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='wallet_transactions'").use { cursor ->
            assertTrue(cursor.moveToFirst())
        }
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='trade_states'").use { cursor ->
            assertTrue(cursor.moveToFirst())
        }
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='user_preferences'").use { cursor ->
            assertTrue(cursor.moveToFirst())
        }
        db.query("SELECT COUNT(*) FROM user_preferences WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.getInt(0) >= 1)
        }
        db.query("SELECT COUNT(*) FROM app_notifications").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.getInt(0) >= 1)
        }

        db.close()
    }
}
