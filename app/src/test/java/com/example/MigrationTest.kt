package com.example

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.example.data.MIGRATION_5_6
import com.example.data.MIGRATION_6_7
import com.example.data.MIGRATION_7_8
import com.example.data.MIGRATION_8_9
import com.example.data.MIGRATION_9_10
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MigrationTest {

    private fun createInMemoryDatabase(): SupportSQLiteDatabase {
        val config = SupportSQLiteOpenHelper.Configuration.builder(RuntimeEnvironment.getApplication())
            .name(null) // null name creates in-memory database
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase
    }

    @Test
    fun migration5To6_hasExpectedVersions() {
        assertEquals(5, MIGRATION_5_6.startVersion)
        assertEquals(6, MIGRATION_5_6.endVersion)
    }

    @Test
    fun migrate5To6_createsNewTablesAndSeedData() {
        val db = createInMemoryDatabase()
        db.version = 5

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
        val db = createInMemoryDatabase()
        db.version = 6
        db.execSQL(
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
        val db = createInMemoryDatabase()
        db.version = 7
        db.execSQL(
            """
            CREATE TABLE trade_states (
                listingId INTEGER NOT NULL PRIMARY KEY,
                state TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO trade_states (listingId, state) VALUES (1, 'AGREEMENT_SIGNED')
            """.trimIndent()
        )

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

    @Test
    fun migration8To9_hasExpectedVersions() {
        assertEquals(8, MIGRATION_8_9.startVersion)
        assertEquals(9, MIGRATION_8_9.endVersion)
    }

    @Test
    fun migrate8To9_createsBlockedUsersAndTradeReportsTables() {
        val db = createInMemoryDatabase()
        db.version = 8

        MIGRATION_8_9.migrate(db)

        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='blocked_users'").use { cursor ->
            assertTrue(cursor.moveToFirst())
        }
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='trade_reports'").use { cursor ->
            assertTrue(cursor.moveToFirst())
        }

        // New tables accept inserts with the expected schema.
        db.execSQL("INSERT INTO blocked_users (userId, blockedName, timestamp) VALUES ('user_sarah', 'Sarah Jenkins', 123)")
        db.query("SELECT blockedName FROM blocked_users WHERE userId = 'user_sarah'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Sarah Jenkins", cursor.getString(0))
        }
        db.execSQL("INSERT INTO trade_reports (listingId, reportedUserId, reportedUserName, reason, timestamp) VALUES (1, 'user_dave', 'David Klay', 'Did not deliver', 456)")
        db.query("SELECT COUNT(*) FROM trade_reports").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }

        db.close()
    }

    @Test
    fun migration9To10_hasExpectedVersions() {
        assertEquals(9, MIGRATION_9_10.startVersion)
        assertEquals(10, MIGRATION_9_10.endVersion)
    }

    @Test
    fun migrate9To10_addsSubscriptionFieldsToUserPreferences() {
        val db = createInMemoryDatabase()
        db.version = 9
        db.execSQL(
            """
            CREATE TABLE user_preferences (
                id INTEGER NOT NULL PRIMARY KEY,
                isDarkMode INTEGER NOT NULL DEFAULT 1,
                maxDistanceFilter REAL,
                searchQuery TEXT NOT NULL DEFAULT '',
                selectedCategory TEXT,
                walletBalance INTEGER NOT NULL DEFAULT 4200
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO user_preferences (id, isDarkMode, maxDistanceFilter, searchQuery, selectedCategory, walletBalance)
            VALUES (1, 1, NULL, '', NULL, 4200)
            """.trimIndent()
        )

        MIGRATION_9_10.migrate(db)

        db.query("SELECT subscriptionType, subscriptionExpiryTimestamp, totalOffersCreated FROM user_preferences LIMIT 0").use { cursor ->
            assertTrue(cursor.columnCount >= 3)
        }
        // Existing rows get default values for new columns.
        db.query("SELECT subscriptionType, subscriptionExpiryTimestamp, totalOffersCreated FROM user_preferences WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("FREE", cursor.getString(0))
            assertEquals(0L, cursor.getLong(1))
            assertEquals(0, cursor.getInt(2))
        }

        db.close()
    }
}
