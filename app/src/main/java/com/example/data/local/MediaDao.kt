package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.AdConfigEntity
import com.example.data.model.AdminAnnouncementEntity
import com.example.data.model.AdminSecurityEntity
import com.example.data.model.ContinueWatchingEntity
import com.example.data.model.CustomMediaEntity
import com.example.data.model.TmdbSearchHistoryEntity
import com.example.data.model.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    // Watchlist
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getAllWatchlist(): Flow<List<WatchlistEntity>>

    @Query("SELECT COUNT(*) > 0 FROM watchlist WHERE id = :id")
    fun isWatchlisted(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWatchlist(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE id = :id")
    suspend fun removeFromWatchlist(id: String)

    // Continue Watching
    @Query("SELECT * FROM continue_watching ORDER BY lastWatchedAt DESC")
    fun getAllContinueWatching(): Flow<List<ContinueWatchingEntity>>

    @Query("SELECT * FROM continue_watching WHERE id = :id LIMIT 1")
    suspend fun getContinueWatchingById(id: String): ContinueWatchingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContinueWatching(item: ContinueWatchingEntity)

    @Query("DELETE FROM continue_watching WHERE id = :id")
    suspend fun removeContinueWatching(id: String)

    // Custom Media (Admin)
    @Query("SELECT * FROM custom_media ORDER BY createdAt DESC")
    fun getAllCustomMedia(): Flow<List<CustomMediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomMedia(item: CustomMediaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCustomMedia(items: List<CustomMediaEntity>)

    @Update
    suspend fun updateCustomMedia(item: CustomMediaEntity)

    @Query("DELETE FROM custom_media WHERE id = :id")
    suspend fun deleteCustomMedia(id: String)

    @Query("DELETE FROM custom_media")
    suspend fun clearAllCustomMedia()

    @Query("DELETE FROM custom_media WHERE id NOT IN (:ids)")
    suspend fun deleteCustomMediaNotIn(ids: List<String>)

    @Transaction
    suspend fun replaceAllCustomMedia(items: List<CustomMediaEntity>) {
        if (items.isEmpty()) {
            clearAllCustomMedia()
        } else {
            insertAllCustomMedia(items)
            deleteCustomMediaNotIn(items.map { it.id })
        }
    }

    // Announcements
    @Query("SELECT * FROM announcements ORDER BY createdAt DESC")
    fun getAllAnnouncements(): Flow<List<AdminAnnouncementEntity>>

    @Query("SELECT * FROM announcements WHERE active = 1 ORDER BY createdAt DESC")
    fun getActiveAnnouncements(): Flow<List<AdminAnnouncementEntity>>

    @Query("SELECT * FROM announcements WHERE id = :id LIMIT 1")
    suspend fun getAnnouncementById(id: Int): AdminAnnouncementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(item: AdminAnnouncementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAnnouncements(items: List<AdminAnnouncementEntity>)

    @Update
    suspend fun updateAnnouncement(item: AdminAnnouncementEntity)

    @Query("DELETE FROM announcements WHERE id = :id")
    suspend fun deleteAnnouncement(id: Int)

    @Query("DELETE FROM announcements")
    suspend fun clearAllAnnouncements()

    @Query("DELETE FROM announcements WHERE id NOT IN (:ids)")
    suspend fun deleteAnnouncementsNotIn(ids: List<Int>)

    @Transaction
    suspend fun replaceAllAnnouncements(items: List<AdminAnnouncementEntity>) {
        if (items.isEmpty()) {
            clearAllAnnouncements()
        } else {
            insertAllAnnouncements(items)
            deleteAnnouncementsNotIn(items.map { it.id })
        }
    }

    @Query("UPDATE announcements SET active = :active WHERE id = :id")
    suspend fun setAnnouncementActive(id: Int, active: Boolean)

    // TMDb Search History (Last 5 searched movie titles)
    @Query("SELECT * FROM tmdb_search_history ORDER BY timestamp DESC LIMIT 5")
    fun getRecentTmdbSearches(): Flow<List<TmdbSearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTmdbSearch(item: TmdbSearchHistoryEntity)

    @Query("DELETE FROM tmdb_search_history WHERE query = :query")
    suspend fun deleteTmdbSearch(query: String)

    @Query("DELETE FROM tmdb_search_history")
    suspend fun clearTmdbSearchHistory()

    @Query("DELETE FROM tmdb_search_history WHERE query NOT IN (SELECT query FROM tmdb_search_history ORDER BY timestamp DESC LIMIT 5)")
    suspend fun trimTmdbSearchHistory()

    // Ad Gate Configuration (Adsterra Pop-under & 6-Hour Cooldown)
    @Query("SELECT * FROM ad_config WHERE id = 1 LIMIT 1")
    fun getAdConfig(): Flow<AdConfigEntity?>

    @Query("SELECT * FROM ad_config WHERE id = 1 LIMIT 1")
    suspend fun getAdConfigOnce(): AdConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAdConfig(config: AdConfigEntity)

    @Query("UPDATE ad_config SET lastCompletedAtMillis = :timestamp, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateAdLastCompleted(timestamp: Long, updatedAt: Long = System.currentTimeMillis())

    // Admin Panel Security (Password)
    @Query("SELECT * FROM admin_security WHERE id = 1 LIMIT 1")
    fun getAdminSecurity(): Flow<AdminSecurityEntity?>

    @Query("SELECT * FROM admin_security WHERE id = 1 LIMIT 1")
    suspend fun getAdminSecurityOnce(): AdminSecurityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAdminSecurity(security: AdminSecurityEntity)

    // Issue Reports (User <-> Admin Problem Tracking)
    @Query("SELECT * FROM issue_reports ORDER BY createdAt DESC")
    fun getAllIssueReports(): Flow<List<com.example.data.model.IssueReportEntity>>

    @Query("SELECT * FROM issue_reports WHERE id = :id LIMIT 1")
    suspend fun getIssueReportById(id: String): com.example.data.model.IssueReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIssueReport(report: com.example.data.model.IssueReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllIssueReports(reports: List<com.example.data.model.IssueReportEntity>)

    @Query("DELETE FROM issue_reports WHERE id = :id")
    suspend fun deleteIssueReport(id: String)

    @Query("DELETE FROM issue_reports")
    suspend fun clearAllIssueReports()

    @Query("DELETE FROM issue_reports WHERE id NOT IN (:ids)")
    suspend fun deleteIssueReportsNotIn(ids: List<String>)

    @Transaction
    suspend fun replaceAllIssueReports(items: List<com.example.data.model.IssueReportEntity>) {
        if (items.isEmpty()) {
            clearAllIssueReports()
        } else {
            insertAllIssueReports(items)
            deleteIssueReportsNotIn(items.map { it.id })
        }
    }

    // Tracked Notification Releases
    @Query("SELECT COUNT(*) > 0 FROM tracked_releases WHERE releaseKey = :releaseKey")
    suspend fun hasTrackedRelease(releaseKey: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackedRelease(release: com.example.data.model.TrackedReleaseEntity)

    // Downloads (MovieBox API & Offline Library)
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<com.example.data.model.DownloadEntity>>

    @Query("SELECT * FROM downloads")
    suspend fun getAllDownloadsOnce(): List<com.example.data.model.DownloadEntity>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getDownloadById(id: String): com.example.data.model.DownloadEntity?

    @Query("SELECT * FROM downloads WHERE systemDownloadId = :systemDownloadId LIMIT 1")
    suspend fun getDownloadBySystemId(systemDownloadId: Long): com.example.data.model.DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDownload(download: com.example.data.model.DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: String)

    @Query("DELETE FROM downloads")
    suspend fun clearAllDownloads()
}
