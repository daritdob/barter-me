package com.example

import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteDatabase
import com.example.data.MIGRATION_5_6
import com.example.data.MIGRATION_6_7
import com.example.data.MIGRATION_7_8
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

    @Test
    fun migration6To7_addsListingStatusColumns() {
        val sqliteDb = SQLiteDatabase.createInMemory(null)
        sqliteDb.version = 6
        sqliteDb.execSQL(
            """
            CREATE TABLE listings (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ownerId TEXT, ownerName TEXT, ownerAvatar TEXT,
                ownerRating REAL, ownerRatingCount INTEGER, isOwnerVerified INTEGER,
                haveItem TEXT, needItem TEXT, categoryHave TEXT, categoryNeed TEXT,
                description TEXT, locationName TEXT, latitude REAL, longitude REAL,
                timestamp INTEGER, isSaved INTEGER, haveType TEXT, needType TEXT,
                deliveryMode TEXT, countryRestricted TEXT, photoUri TEXT
            )
            """.trimIndent()
        )
        val db: SupportSQLiteDatabase = FrameworkSQLiteDatabase.wrap(sqliteDb)
        MIGRATION_6_7.migrate(db)
        db.query("SELECT listingStatus FROM listings LIMIT 0").use { cursor ->
            assertTrue(cursor.columnCount >= 1)
        }
        db.close()
    }

    @Test
    fun migration7To8_hasExpectedVersions() {
        assertEquals(7, MIGRATION_7_8.startVersion)
        assertEquals(8, MIGRATION_7_8.endVersion)
    }

    @Test
    fun migration7To8_addsSignedValuationColumns() {
        val sqliteDb = SQLiteDatabase.createInMemory(null)
        sqliteDb.version = 7
        sqliteDb.execSQL(
            """
            CREATE TABLE trade_states (
                listingId INTEGER NOT NULL PRIMARY KEY,
                state TEXT NOT NULL
            )
            """.trimIndent()
        )
        sqliteDb.execSQL(
            """
            INSERT INTO trade_states (listingId, state) VALUES (1, 'AGREEMENT_SIGNED')
            """.trimIndent()
        )
        val db: SupportSQLiteDatabase = FrameworkSQLiteDatabase.wrap(sqliteDb)

        MIGRATION_7_8.migrate(db)

        db.query("SELECT signedSelfValue, signedCounterpartyValue FROM trade_states LIMIT 0").use { cursor ->
            assertTrue(cursor.columnCount >= 2)
        }
        // Existing rows get the safe default of 0 for the new columns.
        db.query("SELECT signedSelfValue, signedCounterpartyValue FROM trade_states WHERE listingId = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
            assertEquals(0, cursor.getInt(1))
        }
        db.close()
    }
}
