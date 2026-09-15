package org.appdevncsu.foodfinder.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CachedPayload::class, FavoriteItem::class],
    version = 2,
    exportSchema = true,
)
abstract class FoodFinderDatabase : RoomDatabase() {
    abstract fun payloadDao(): PayloadDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        /** Adds the favorites table. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favorites` (" +
                        "`normalizedName` TEXT NOT NULL, " +
                        "`displayName` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`normalizedName`))"
                )
            }
        }
    }
}
