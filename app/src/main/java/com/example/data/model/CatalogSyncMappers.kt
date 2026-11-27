package com.example.data.model

import kotlin.math.abs

data class CloudSyncStatus(
    val isAvailable: Boolean = false,
    val stateLabel: String = "INITIALIZING...",
    val lastSyncedAt: Long = 0L,
    val lastError: String? = null
)

object CatalogSyncMappers {
    const val COLLECTION_CUSTOM_MEDIA = "custom_media"
    const val COLLECTION_ANNOUNCEMENTS = "announcements"
    const val COLLECTION_AD_CONFIG = "ad_config"
    const val COLLECTION_ISSUE_REPORTS = "issue_reports"
    const val AD_CONFIG_DOC_ID = "global"

    fun announcementDocId(localId: Int): String = "ann_$localId"

    fun generateStableAnnouncementId(nowMillis: Long = System.currentTimeMillis()): Int {
        val raw = (nowMillis and 0x3FFFFFFFL).toInt()
        return if (raw <= 0) 1 else raw
    }

    fun parseAnnouncementId(documentId: String, data: Map<String, Any?>): Int {
        val explicitId = (data["id"] as? Number)?.toInt()
        if (explicitId != null && explicitId > 0) return explicitId
        val fromSuffix = documentId.removePrefix("ann_").toIntOrNull()
        if (fromSuffix != null && fromSuffix > 0) return fromSuffix
        val hashed = abs(documentId.hashCode())
        return if (hashed == 0) 1 else hashed
    }

    fun customMediaToMap(entity: CustomMediaEntity): Map<String, Any?> {
        return mapOf(
            "id" to entity.id,
            "title" to entity.title,
            "overview" to entity.overview,
            "posterPath" to entity.posterPath,
            "backdropPath" to entity.backdropPath,
            "mediaType" to entity.mediaType,
            "releaseDate" to entity.releaseDate,
            "voteAverage" to entity.voteAverage,
            "genres" to entity.genres,
            "certification" to entity.certification,
            "vidlyId" to entity.vidlyId,
            "tmdbId" to entity.tmdbId,
            "trailerYoutubeKey" to entity.trailerYoutubeKey,
            "numberOfSeasons" to entity.numberOfSeasons,
            "episodesPerSeason" to entity.episodesPerSeason,
            "directVideoUrl" to entity.directVideoUrl,
            "downloadUrl" to entity.downloadUrl,
            "episodeLinksJson" to entity.episodeLinksJson,
            "subtitleUrl" to entity.subtitleUrl,
            "subtitleLabel" to entity.subtitleLabel,
            "createdAt" to entity.createdAt
        )
    }

    fun customMediaFromMap(documentId: String, data: Map<String, Any?>?): CustomMediaEntity? {
        if (data == null) return null
        val id = (data["id"] as? String)?.takeIf { it.isNotBlank() } ?: documentId
        val title = (data["title"] as? String)?.trim().orEmpty()
        val vidlyId = (data["vidlyId"] as? String)?.trim().orEmpty()
        if (id.isBlank() || title.isBlank()) return null

        return CustomMediaEntity(
            id = id,
            title = title,
            overview = (data["overview"] as? String).orEmpty(),
            posterPath = (data["posterPath"] as? String).orEmpty(),
            backdropPath = (data["backdropPath"] as? String).orEmpty(),
            mediaType = ((data["mediaType"] as? String)?.takeIf { it == "tv" || it == "movie" }) ?: "movie",
            releaseDate = (data["releaseDate"] as? String).orEmpty(),
            voteAverage = (data["voteAverage"] as? Number)?.toDouble() ?: 0.0,
            genres = (data["genres"] as? String).orEmpty(),
            certification = (data["certification"] as? String)?.trim().orEmpty(),
            vidlyId = vidlyId,
            tmdbId = (data["tmdbId"] as? Number)?.toLong(),
            trailerYoutubeKey = (data["trailerYoutubeKey"] as? String)?.takeIf { it.isNotBlank() },
            numberOfSeasons = ((data["numberOfSeasons"] as? Number)?.toInt() ?: 1).coerceAtLeast(1),
            episodesPerSeason = ((data["episodesPerSeason"] as? Number)?.toInt() ?: 10).coerceAtLeast(1),
            directVideoUrl = (data["directVideoUrl"] as? String)?.trim().orEmpty(),
            downloadUrl = (data["downloadUrl"] as? String)?.trim().orEmpty(),
            episodeLinksJson = (data["episodeLinksJson"] as? String)?.trim().orEmpty(),
            subtitleUrl = (data["subtitleUrl"] as? String)?.trim().orEmpty(),
            subtitleLabel = (data["subtitleLabel"] as? String)?.trim().orEmpty(),
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    fun announcementToMap(entity: AdminAnnouncementEntity): Map<String, Any?> {
        return mapOf(
            "id" to entity.id,
            "title" to entity.title,
            "message" to entity.message,
            "type" to entity.type,
            "active" to entity.active,
            "createdAt" to entity.createdAt
        )
    }

    fun announcementFromMap(documentId: String, data: Map<String, Any?>?): AdminAnnouncementEntity? {
        if (data == null) return null
        val title = (data["title"] as? String)?.trim().orEmpty()
        val message = (data["message"] as? String)?.trim().orEmpty()
        if (title.isBlank() && message.isBlank()) return null

        return AdminAnnouncementEntity(
            id = parseAnnouncementId(documentId, data),
            title = title,
            message = message,
            type = ((data["type"] as? String)?.uppercase()?.takeIf {
                it == "INFO" || it == "WARNING" || it == "UPDATE"
            }) ?: "INFO",
            active = (data["active"] as? Boolean) ?: true,
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    fun adConfigToMap(entity: AdConfigEntity): Map<String, Any?> {
        return mapOf(
            "id" to 1,
            "isEnabled" to entity.isEnabled,
            "adNetwork" to entity.adNetwork,
            "adFormat" to entity.adFormat,
            "adUrl" to entity.adUrl,
            "popUnderScript" to entity.popUnderScript,
            "headline" to entity.headline,
            "description" to entity.description,
            "ctaText" to entity.ctaText,
            "requiredStaySeconds" to entity.requiredStaySeconds,
            "cooldownHours" to entity.cooldownHours,
            "cooldownResetAtMillis" to entity.cooldownResetAtMillis,
            "updatedAt" to entity.updatedAt
        )
    }

    fun adConfigFromMap(
        data: Map<String, Any?>?,
        localExisting: AdConfigEntity?
    ): AdConfigEntity? {
        if (data == null) return null
        val remoteResetAt = (data["cooldownResetAtMillis"] as? Number)?.toLong() ?: 0L
        val localCompletedAt = localExisting?.lastCompletedAtMillis ?: 0L
        val resolvedLastCompletedAt = if (remoteResetAt > localCompletedAt) {
            0L
        } else {
            localCompletedAt
        }

        val defaults = AdConfigEntity()
        return AdConfigEntity(
            id = 1,
            isEnabled = (data["isEnabled"] as? Boolean) ?: defaults.isEnabled,
            adNetwork = (data["adNetwork"] as? String)?.takeIf { it.isNotBlank() } ?: defaults.adNetwork,
            adFormat = (data["adFormat"] as? String)?.takeIf { it.isNotBlank() } ?: defaults.adFormat,
            adUrl = (data["adUrl"] as? String).orEmpty(),
            popUnderScript = (data["popUnderScript"] as? String).orEmpty(),
            headline = (data["headline"] as? String)?.takeIf { it.isNotBlank() } ?: defaults.headline,
            description = (data["description"] as? String)?.takeIf { it.isNotBlank() } ?: defaults.description,
            ctaText = (data["ctaText"] as? String)?.takeIf { it.isNotBlank() } ?: defaults.ctaText,
            requiredStaySeconds = ((data["requiredStaySeconds"] as? Number)?.toInt() ?: defaults.requiredStaySeconds)
                .coerceIn(5, 300),
            cooldownHours = ((data["cooldownHours"] as? Number)?.toInt() ?: defaults.cooldownHours)
                .coerceIn(1, 168),
            lastCompletedAtMillis = resolvedLastCompletedAt,
            cooldownResetAtMillis = maxOf(remoteResetAt, localExisting?.cooldownResetAtMillis ?: 0L),
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    fun issueReportToMap(entity: IssueReportEntity): Map<String, Any?> {
        return mapOf(
            "id" to entity.id,
            "category" to entity.category,
            "description" to entity.description,
            "mediaId" to entity.mediaId,
            "mediaTitle" to entity.mediaTitle,
            "mediaType" to entity.mediaType,
            "serverName" to entity.serverName,
            "episodeLabel" to entity.episodeLabel,
            "screenshotPreviewBase64" to entity.screenshotPreviewBase64,
            "status" to entity.status,
            "adminNotes" to entity.adminNotes,
            "createdAt" to entity.createdAt,
            "updatedAt" to entity.updatedAt
        )
    }

    fun issueReportFromMap(documentId: String, data: Map<String, Any?>?): IssueReportEntity? {
        if (data == null) return null
        val id = (data["id"] as? String)?.takeIf { it.isNotBlank() } ?: documentId
        val description = (data["description"] as? String)?.trim().orEmpty()
        val category = IssueReportCategory.fromKey(data["category"] as? String).storageKey
        if (id.isBlank() || description.isBlank()) return null

        return IssueReportEntity(
            id = id,
            category = category,
            description = description,
            mediaId = (data["mediaId"] as? String).orEmpty(),
            mediaTitle = (data["mediaTitle"] as? String).orEmpty(),
            mediaType = (data["mediaType"] as? String).orEmpty(),
            serverName = (data["serverName"] as? String).orEmpty(),
            episodeLabel = (data["episodeLabel"] as? String).orEmpty(),
            screenshotPreviewBase64 = (data["screenshotPreviewBase64"] as? String).orEmpty(),
            status = IssueReportStatus.fromKey(data["status"] as? String).storageKey,
            adminNotes = (data["adminNotes"] as? String).orEmpty(),
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
}

object CatalogOverridePolicy {
    fun matchesOverride(catalogItem: MediaItem, customItem: MediaItem): Boolean {
        if (catalogItem.id == customItem.id) return true
        val catalogTmdb = catalogItem.resolvedTmdbId() ?: return false
        val customTmdb = customItem.tmdbId ?: customItem.vidlyId.toLongOrNull() ?: return false
        return catalogTmdb == customTmdb && catalogItem.mediaType == customItem.mediaType
    }

    fun applyOverride(catalogItem: MediaItem, customOverride: MediaItem): MediaItem {
        return catalogItem.copy(
            tmdbId = catalogItem.tmdbId ?: customOverride.tmdbId,
            title = customOverride.title.ifBlank { catalogItem.title },
            overview = customOverride.overview.ifBlank { catalogItem.overview },
            posterPath = customOverride.posterPath.ifBlank { catalogItem.posterPath },
            backdropPath = customOverride.backdropPath.ifBlank { catalogItem.backdropPath },
            releaseDate = customOverride.releaseDate.ifBlank { catalogItem.releaseDate },
            voteAverage = if (customOverride.voteAverage > 0.0) customOverride.voteAverage else catalogItem.voteAverage,
            genres = customOverride.genres.ifEmpty { catalogItem.genres },
            certification = customOverride.certification.ifBlank { catalogItem.certification },
            vidlyId = customOverride.vidlyId.ifBlank { catalogItem.vidlyId },
            isCustom = catalogItem.isCustom || customOverride.isCustom,
            trailerYoutubeKey = customOverride.trailerYoutubeKey ?: catalogItem.trailerYoutubeKey,
            numberOfSeasons = if (customOverride.numberOfSeasons > 1 || catalogItem.seasons.isEmpty()) {
                maxOf(customOverride.numberOfSeasons, catalogItem.numberOfSeasons)
            } else {
                catalogItem.numberOfSeasons
            },
            episodesPerSeason = if (catalogItem.seasons.isEmpty() && customOverride.episodesPerSeason > 0) {
                customOverride.episodesPerSeason
            } else {
                catalogItem.episodesPerSeason
            },
            directVideoUrl = customOverride.directVideoUrl.ifBlank { catalogItem.directVideoUrl },
            downloadUrl = customOverride.downloadUrl.ifBlank { catalogItem.downloadUrl },
            episodeLinksJson = customOverride.episodeLinksJson.ifBlank { catalogItem.episodeLinksJson },
            subtitleUrl = customOverride.subtitleUrl.ifBlank { catalogItem.subtitleUrl },
            subtitleLabel = customOverride.subtitleLabel.ifBlank { catalogItem.subtitleLabel }
        )
    }

    fun applyAdminOverrides(
        items: List<MediaItem>,
        customMedia: List<MediaItem>
    ): List<MediaItem> {
        if (items.isEmpty() || customMedia.isEmpty()) return items
        return items.map { item -> resolveItemWithOverrides(item, customMedia) }
    }

    fun resolveItemWithOverrides(
        item: MediaItem,
        customMedia: List<MediaItem>
    ): MediaItem {
        val matchingOverride = customMedia.firstOrNull { matchesOverride(item, it) } ?: return item
        return applyOverride(item, matchingOverride)
    }
}
