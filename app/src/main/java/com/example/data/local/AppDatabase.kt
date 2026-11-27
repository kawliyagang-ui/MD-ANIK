package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AdConfigEntity
import com.example.data.model.AdminAnnouncementEntity
import com.example.data.model.AdminSecurityEntity
import com.example.data.model.ContinueWatchingEntity
import com.example.data.model.CustomMediaEntity
import com.example.data.model.DownloadEntity
import com.example.data.model.IssueReportEntity
import com.example.data.model.PlaybackPositionEntity
import com.example.data.model.TmdbSearchHistoryEntity
import com.example.data.model.TrackedReleaseEntity
import com.example.data.model.WatchlistEntity

@Database(
    entities = [
        WatchlistEntity::class,
        ContinueWatchingEntity::class,
        CustomMediaEntity::class,
        AdminAnnouncementEntity::class,
        TmdbSearchHistoryEntity::class,
        AdConfigEntity::class,
        AdminSecurityEntity::class,
        IssueReportEntity::class,
        TrackedReleaseEntity::class,
        DownloadEntity::class,
        PlaybackPositionEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE custom_media ADD COLUMN directVideoUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE custom_media ADD COLUMN downloadUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE custom_media ADD COLUMN episodeLinksJson TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE downloads ADD COLUMN refererHeader TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE downloads ADD COLUMN userAgentHeader TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE downloads ADD COLUMN cookieHeader TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE downloads ADD COLUMN wifiOnly INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE downloads ADD COLUMN completedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE downloads SET status = 'RUNNING' WHERE status = 'DOWNLOADING'")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS playback_positions (
                        `key` TEXT NOT NULL PRIMARY KEY,
                        mediaId TEXT NOT NULL,
                        season INTEGER NOT NULL,
                        episode INTEGER NOT NULL,
                        positionMs INTEGER NOT NULL,
                        durationMs INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE custom_media ADD COLUMN subtitleUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE custom_media ADD COLUMN subtitleLabel TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cinestream_database"
                )
                    .addMigrations(MIGRATION_8_9, MIGRATION_9_10)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
