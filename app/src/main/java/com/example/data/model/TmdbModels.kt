package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TmdbPageResponse(
    val page: Int = 1,
    val results: List<TmdbItemDto> = emptyList(),
    @param:Json(name = "total_pages") val totalPages: Int = 1,
    @param:Json(name = "total_results") val totalResults: Int = 0
)

@JsonClass(generateAdapter = true)
data class TmdbItemDto(
    val id: Long,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    @param:Json(name = "poster_path") val posterPath: String? = null,
    @param:Json(name = "backdrop_path") val backdropPath: String? = null,
    @param:Json(name = "media_type") val mediaType: String? = null,
    @param:Json(name = "release_date") val releaseDate: String? = null,
    @param:Json(name = "first_air_date") val firstAirDate: String? = null,
    @param:Json(name = "vote_average") val voteAverage: Double? = 0.0,
    @param:Json(name = "genre_ids") val genreIds: List<Int>? = emptyList(),
    @param:Json(name = "number_of_seasons") val numberOfSeasons: Int? = null,
    @param:Json(name = "number_of_episodes") val numberOfEpisodes: Int? = null
)

@JsonClass(generateAdapter = true)
data class TmdbGenreDto(
    val id: Int,
    val name: String
)

@JsonClass(generateAdapter = true)
data class TmdbSeasonSummaryDto(
    val id: Long? = null,
    val name: String? = null,
    val overview: String? = null,
    @param:Json(name = "air_date") val airDate: String? = null,
    @param:Json(name = "episode_count") val episodeCount: Int? = 0,
    @param:Json(name = "poster_path") val posterPath: String? = null,
    @param:Json(name = "season_number") val seasonNumber: Int? = 0
)

@JsonClass(generateAdapter = true)
data class TmdbTvDetailsDto(
    val id: Long,
    val name: String? = null,
    val overview: String? = null,
    @param:Json(name = "poster_path") val posterPath: String? = null,
    @param:Json(name = "backdrop_path") val backdropPath: String? = null,
    @param:Json(name = "first_air_date") val firstAirDate: String? = null,
    @param:Json(name = "vote_average") val voteAverage: Double? = 0.0,
    val genres: List<TmdbGenreDto>? = emptyList(),
    @param:Json(name = "number_of_seasons") val numberOfSeasons: Int? = null,
    @param:Json(name = "number_of_episodes") val numberOfEpisodes: Int? = null,
    val seasons: List<TmdbSeasonSummaryDto>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class TmdbEpisodeDto(
    val id: Long? = null,
    @param:Json(name = "episode_number") val episodeNumber: Int? = 1,
    @param:Json(name = "season_number") val seasonNumber: Int? = 1,
    val name: String? = null,
    val overview: String? = null,
    @param:Json(name = "air_date") val airDate: String? = null,
    val runtime: Int? = null,
    @param:Json(name = "still_path") val stillPath: String? = null,
    @param:Json(name = "vote_average") val voteAverage: Double? = 0.0
)

@JsonClass(generateAdapter = true)
data class TmdbSeasonDetailsDto(
    val id: Long? = null,
    val name: String? = null,
    val overview: String? = null,
    @param:Json(name = "air_date") val airDate: String? = null,
    @param:Json(name = "season_number") val seasonNumber: Int? = 1,
    val episodes: List<TmdbEpisodeDto>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class TmdbVideosResponse(
    val id: Long,
    val results: List<TmdbVideoDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TmdbVideoDto(
    val id: String,
    val key: String,
    val name: String,
    val site: String,
    val type: String
)

@JsonClass(generateAdapter = true)
data class TmdbReleaseDateEntryDto(
    val certification: String? = null,
    val type: Int? = null
)

@JsonClass(generateAdapter = true)
data class TmdbCountryReleaseDatesDto(
    @param:Json(name = "iso_3166_1") val iso31661: String = "",
    @param:Json(name = "release_dates") val releaseDates: List<TmdbReleaseDateEntryDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TmdbMovieReleaseDatesResponse(
    val id: Long = 0L,
    val results: List<TmdbCountryReleaseDatesDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TmdbTvRatingEntryDto(
    @param:Json(name = "iso_3166_1") val iso31661: String = "",
    val rating: String? = null
)

@JsonClass(generateAdapter = true)
data class TmdbTvContentRatingsResponse(
    val id: Long = 0L,
    val results: List<TmdbTvRatingEntryDto> = emptyList()
)

fun TmdbMovieReleaseDatesResponse.extractBestCertification(): String {
    val preferredCountries = listOf("US", "GB", "CA", "AU", "IN")
    for (country in preferredCountries) {
        val cert = results
            .firstOrNull { it.iso31661.equals(country, ignoreCase = true) }
            ?.releaseDates
            ?.firstOrNull { !it.certification.isNullOrBlank() }
            ?.certification
            ?.trim()
        if (!cert.isNullOrBlank()) return cert
    }
    return results
        .asSequence()
        .flatMap { it.releaseDates.asSequence() }
        .mapNotNull { it.certification?.trim() }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
}

fun TmdbTvContentRatingsResponse.extractBestCertification(): String {
    val preferredCountries = listOf("US", "GB", "CA", "AU", "IN")
    for (country in preferredCountries) {
        val rating = results
            .firstOrNull { it.iso31661.equals(country, ignoreCase = true) }
            ?.rating
            ?.trim()
        if (!rating.isNullOrBlank()) return rating
    }
    return results
        .asSequence()
        .mapNotNull { it.rating?.trim() }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
}
