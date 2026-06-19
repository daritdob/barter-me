package com.example.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS app_notifications (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                message TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                isRead INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS wallet_transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                amount INTEGER NOT NULL,
                type TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS trade_states (
                listingId INTEGER NOT NULL PRIMARY KEY,
                state TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_preferences (
                id INTEGER NOT NULL PRIMARY KEY,
                isDarkMode INTEGER NOT NULL DEFAULT 1,
                maxDistanceFilter REAL,
                searchQuery TEXT NOT NULL DEFAULT '',
                selectedCategory TEXT,
                walletBalance INTEGER NOT NULL DEFAULT 4200
            )
            """.trimIndent()
        )

        val now = System.currentTimeMillis()
        db.execSQL(
            """
            INSERT INTO app_notifications (id, title, message, timestamp, isRead)
            VALUES ('notif_welcome', 'Welcome back, Alex!', 'New portrait exchange opportunity is open nearby in SoHo, NY.', ${now - 300000}, 0)
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO user_preferences (id, isDarkMode, maxDistanceFilter, searchQuery, selectedCategory, walletBalance)
            VALUES (1, 1, NULL, '', NULL, 4200)
            """.trimIndent()
        )
        val threeDaysAgo = now - 3600000L * 24 * 3
        val oneDayAgo = now - 3600000L * 24
        db.execSQL(
            """
            INSERT INTO wallet_transactions (title, amount, type, timestamp)
            VALUES ('Complete Brand Redesign Package (with Sarah Jenkins)', 500, 'earned', $threeDaysAgo)
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO wallet_transactions (title, amount, type, timestamp)
            VALUES ('Office Bookkeeping Consultation (with David Klay)', 200, 'spent', $oneDayAgo)
            """.trimIndent()
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE listings ADD COLUMN listingStatus TEXT NOT NULL DEFAULT 'APPROVED'
            """.trimIndent()
        )
        db.execSQL(
            """
            ALTER TABLE listings ADD COLUMN rejectionReason TEXT
            """.trimIndent()
        )
        db.execSQL(
            """
            ALTER TABLE listings ADD COLUMN submittedAt INTEGER NOT NULL DEFAULT 0
            """.trimIndent()
        )
        db.execSQL(
            """
            UPDATE listings SET submittedAt = timestamp WHERE submittedAt = 0
            """.trimIndent()
        )
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE trade_states ADD COLUMN signedSelfValue INTEGER NOT NULL DEFAULT 0
            """.trimIndent()
        )
        db.execSQL(
            """
            ALTER TABLE trade_states ADD COLUMN signedCounterpartyValue INTEGER NOT NULL DEFAULT 0
            """.trimIndent()
        )
    }
}
