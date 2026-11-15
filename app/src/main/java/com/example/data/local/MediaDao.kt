package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AdminAnnouncementEntity
import com.example.data.model.ContinueWatchingEntity
import com.example.data.model.CustomMediaEntity
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

    @Update
    suspend fun updateCustomMedia(item: CustomMediaEntity)

    @Query("DELETE FROM custom_media WHERE id = :id")
    suspend fun deleteCustomMedia(id: String)

    // Announcements
    @Query("SELECT * FROM announcements ORDER BY createdAt DESC")
    fun getAllAnnouncements(): Flow<List<AdminAnnouncementEntity>>

    @Query("SELECT * FROM announcements WHERE active = 1 ORDER BY createdAt DESC")
    fun getActiveAnnouncements(): Flow<List<AdminAnnouncementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(item: AdminAnnouncementEntity)

    @Update
    suspend fun updateAnnouncement(item: AdminAnnouncementEntity)

    @Query("DELETE FROM announcements WHERE id = :id")
    suspend fun deleteAnnouncement(id: Int)

    @Query("UPDATE announcements SET active = :active WHERE id = :id")
    suspend fun setAnnouncementActive(id: Int, active: Boolean)

    // TMDb Search History (Last 5 searched movie titles)
    @Query("SELECT * FROM tmdb_search_history ORDER BY timestamp DESC LIMIT 5")
    fun getRecentTmdbSearches(): Flow<List<com.example.data.model.TmdbSearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTmdbSearch(item: com.example.data.model.TmdbSearchHistoryEntity)

    @Query("DELETE FROM tmdb_search_history WHERE query = :query")
    suspend fun deleteTmdbSearch(query: String)

    @Query("DELETE FROM tmdb_search_history")
    suspend fun clearTmdbSearchHistory()

    @Query("DELETE FROM tmdb_search_history WHERE query NOT IN (SELECT query FROM tmdb_search_history ORDER BY timestamp DESC LIMIT 5)")
    suspend fun trimTmdbSearchHistory()

    // Ad Gate Configuration (Adsterra Pop-under & 6-Hour Cooldown)
    @Query("SELECT * FROM ad_config WHERE id = 1 LIMIT 1")
    fun getAdConfig(): Flow<com.example.data.model.AdConfigEntity?>

    @Query("SELECT * FROM ad_config WHERE id = 1 LIMIT 1")
    suspend fun getAdConfigOnce(): com.example.data.model.AdConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAdConfig(config: com.example.data.model.AdConfigEntity)

    @Query("UPDATE ad_config SET lastCompletedAtMillis = :timestamp, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateAdLastCompleted(timestamp: Long, updatedAt: Long = System.currentTimeMillis())

    // Admin Panel Security (Password)
    @Query("SELECT * FROM admin_security WHERE id = 1 LIMIT 1")
    fun getAdminSecurity(): Flow<com.example.data.model.AdminSecurityEntity?>

    @Query("SELECT * FROM admin_security WHERE id = 1 LIMIT 1")
    suspend fun getAdminSecurityOnce(): com.example.data.model.AdminSecurityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAdminSecurity(security: com.example.data.model.AdminSecurityEntity)
}

