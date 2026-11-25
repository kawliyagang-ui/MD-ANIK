package com.example

import com.example.data.model.*
import com.example.data.model.PlayerServer
import com.example.data.remote.MovieBoxApiClient
import com.example.data.repository.applyToMediaItem
import com.example.data.repository.toEpisodeInfoList
import com.example.ui.theme.DarkThemePalette
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemeTogglePolicy
import com.example.ui.theme.buildMaterialColorScheme
import com.example.ui.theme.paletteColorsFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testMovieStreamUrlGeneration() {
        val movie = MediaItem(
            id = "693134",
            title = "Dune: Part Two",
            overview = "Desert epic",
            posterPath = "/poster.jpg",
            mediaType = "movie",
            vidlyId = "693134"
        )

        // Vidly Pro movie stream url
        val vidlyUrl = movie.getStreamUrl(PlayerServer.VIDLY)
        assertEquals("https://vidly.pro/movie/693134", vidlyUrl)

        // VidSrc backup player url
        val vidSrcUrl = movie.getStreamUrl(PlayerServer.VIDSRC)
        assertEquals("https://vidsrc.to/embed/movie/693134", vidSrcUrl)

        // AutoEmbed backup player url
        val autoEmbedUrl = movie.getStreamUrl(PlayerServer.AUTOEMBED)
        assertEquals("https://player.autoembed.cc/embed/movie/693134", autoEmbedUrl)

        // MultiEmbed server url
        val multiEmbedUrl = movie.getStreamUrl(PlayerServer.MULTIEMBED)
        assertEquals("https://multiembed.mov/?video_id=693134&tmdb=1", multiEmbedUrl)
    }

    @Test
    fun testTvSeriesStreamUrlGeneration() {
        val series = MediaItem(
            id = "1399",
            title = "Game of Thrones",
            overview = "Fantasy epic",
            posterPath = "/got.jpg",
            mediaType = "tv",
            vidlyId = "1399",
            numberOfSeasons = 8,
            episodesPerSeason = 10
        )

        val vidlyTvUrl = series.getStreamUrl(PlayerServer.VIDLY, season = 3, episode = 9)
        assertEquals("https://vidly.pro/tv/1399/3/9", vidlyTvUrl)

        val vidSrcTvUrl = series.getStreamUrl(PlayerServer.VIDSRC, season = 1, episode = 1)
        assertEquals("https://vidsrc.to/embed/tv/1399/1/1", vidSrcTvUrl)

        val autoEmbedTvUrl = series.getStreamUrl(PlayerServer.AUTOEMBED, season = 2, episode = 5)
        assertEquals("https://player.autoembed.cc/embed/tv/1399/2/5", autoEmbedTvUrl)
    }

    @Test
    fun testPosterAndBackdropUrlReturnsEmptyWhenBlank() {
        val itemWithEmptyImages = MediaItem(
            id = "test",
            title = "Test Movie",
            overview = "Test",
            posterPath = "",
            backdropPath = ""
        )
        assertEquals("", itemWithEmptyImages.fullPosterUrl)
        assertEquals("", itemWithEmptyImages.fullBackdropUrl)
    }

    @Test
    fun testTmdbSearchHistoryEntityCreation() {
        val entity = com.example.data.model.TmdbSearchHistoryEntity(
            query = "Gladiator II",
            timestamp = 1700000000000L
        )
        assertEquals("Gladiator II", entity.query)
        assertEquals(1700000000000L, entity.timestamp)
    }

    @Test
    fun testTmdbSearchHistoryLastFivePreservation() {
        val initialSearches = listOf(
            com.example.data.model.TmdbSearchHistoryEntity("Inception", 1000L),
            com.example.data.model.TmdbSearchHistoryEntity("Interstellar", 2000L),
            com.example.data.model.TmdbSearchHistoryEntity("Dune: Part Two", 3000L),
            com.example.data.model.TmdbSearchHistoryEntity("Oppenheimer", 4000L),
            com.example.data.model.TmdbSearchHistoryEntity("Arcane", 5000L)
        )

        // Adding 6th search query
        val newSearch = com.example.data.model.TmdbSearchHistoryEntity("Gladiator II", 6000L)
        val combined = (initialSearches + newSearch)
            .sortedByDescending { it.timestamp }
            .take(5)

        assertEquals(5, combined.size)
        assertEquals("Gladiator II", combined.first().query)
        assertEquals(listOf("Gladiator II", "Arcane", "Oppenheimer", "Dune: Part Two", "Interstellar"), combined.map { it.query })
    }

    @Test
    fun testReleaseDateFormatting() {
        assertEquals("Nov 22, 2024", com.example.ui.components.formatReleaseDate("2024-11-22"))
        assertEquals("2024", com.example.ui.components.formatReleaseDate("2024"))
        assertEquals("TBA", com.example.ui.components.formatReleaseDate(""))
    }

    private fun sampleGenreCatalog(): List<MediaItem> = listOf(
        MediaItem(
            id = "1",
            title = "Dune: Part Two",
            overview = "Desert epic",
            posterPath = "/dune.jpg",
            genres = listOf("Sci-Fi", "Adventure")
        ),
        MediaItem(
            id = "2",
            title = "Deadpool & Wolverine",
            overview = "Action comedy",
            posterPath = "/dpw.jpg",
            genres = listOf("Action", "Comedy", "Sci-Fi")
        ),
        MediaItem(
            id = "3",
            title = "Gladiator II",
            overview = "Historical action",
            posterPath = "/glad2.jpg",
            genres = listOf("Action", "Drama")
        ),
        MediaItem(
            id = "4",
            title = "Alien: Romulus",
            overview = "Space horror",
            posterPath = "/alien.jpg",
            genres = listOf("Horror", "Sci-Fi")
        )
    )

    @Test
    fun testHomeGenreCatalogFiltering() {
        val sampleTrending = sampleGenreCatalog()

        val all = com.example.ui.components.HomeGenreCatalog.filterByGenre(sampleTrending, "All")
        assertEquals(sampleTrending.size, all.size)

        val action = com.example.ui.components.HomeGenreCatalog.filterByGenre(sampleTrending, "Action")
        assertEquals(listOf("Deadpool & Wolverine", "Gladiator II"), action.map { it.title })

        val comedy = com.example.ui.components.HomeGenreCatalog.filterByGenre(sampleTrending, "Comedy")
        assertEquals(listOf("Deadpool & Wolverine"), comedy.map { it.title })

        val sciFi = com.example.ui.components.HomeGenreCatalog.filterByGenre(sampleTrending, "sci-fi")
        assertEquals(listOf("Dune: Part Two", "Deadpool & Wolverine", "Alien: Romulus"), sciFi.map { it.title })

        val none = com.example.ui.components.HomeGenreCatalog.filterByGenre(sampleTrending, "Animation")
        assertTrue(none.isEmpty())
    }

    @Test
    fun testHomeGenreCatalogAvailableGenresIncludesCoreAndExtras() {
        val itemsWithExtra = sampleGenreCatalog() + MediaItem(
            id = "extra1",
            title = "Jazz Night",
            overview = "Musical drama",
            posterPath = "/p.jpg",
            genres = listOf("Music", "Cinema")
        )
        val genres = com.example.ui.components.HomeGenreCatalog.availableGenres(itemsWithExtra)
        assertTrue(genres.containsAll(listOf("All", "Action", "Comedy", "Sci-Fi", "Music")))
        assertTrue(!genres.contains("Cinema"))
    }

    @Test
    fun testAdGatePolicySixHourCooldown() {
        val now = 1_700_000_000_000L
        val sixHoursMillis = 6L * 3_600_000L

        // Disabled -> should not show
        val disabled = com.example.data.model.AdConfigEntity(
            isEnabled = false,
            adUrl = "https://example.com/ad"
        )
        assertTrue(!com.example.data.model.AdGatePolicy.shouldShowAdGate(disabled, now))

        // Enabled but empty URL & script -> should not lock user out
        val empty = com.example.data.model.AdConfigEntity(
            isEnabled = true,
            adUrl = "",
            popUnderScript = ""
        )
        assertTrue(!com.example.data.model.AdGatePolicy.shouldShowAdGate(empty, now))

        // Enabled and never completed -> should show
        val firstTime = com.example.data.model.AdConfigEntity(
            isEnabled = true,
            adUrl = "https://example.com/ad",
            requiredStaySeconds = 30,
            cooldownHours = 6,
            lastCompletedAtMillis = 0L
        )
        assertTrue(com.example.data.model.AdGatePolicy.shouldShowAdGate(firstTime, now))

        // Completed 2 hours ago -> still unlocked
        val withinCooldown = firstTime.copy(lastCompletedAtMillis = now - (2L * 3_600_000L))
        assertTrue(!com.example.data.model.AdGatePolicy.shouldShowAdGate(withinCooldown, now))
        assertEquals(
            "4h 0m remaining",
            com.example.data.model.AdGatePolicy.formatRemainingCooldown(
                com.example.data.model.AdGatePolicy.remainingCooldownMillis(withinCooldown, now)
            )
        )

        // Completed 6 hours ago -> should show again
        val expiredCooldown = firstTime.copy(lastCompletedAtMillis = now - sixHoursMillis)
        assertTrue(com.example.data.model.AdGatePolicy.shouldShowAdGate(expiredCooldown, now))
    }

    @Test
    fun testAdsterraUrlAndPopUnderScriptFormatting() {
        assertEquals(
            "https://pl1234.effectivegatecpm.com/ad.js",
            com.example.data.model.AdGatePolicy.normalizeUrl("//pl1234.effectivegatecpm.com/ad.js")
        )
        assertEquals(
            "https://example.com/direct",
            com.example.data.model.AdGatePolicy.normalizeUrl("example.com/direct")
        )

        val htmlFromScriptTag = com.example.data.model.AdGatePolicy.buildAdsterraPopUnderHtml(
            popUnderScript = "<script type='text/javascript' src='//pl999.effectivegatecpm.com/ab/cd/invoke.js'></script>",
            adUrl = "https://adsterra.example.com/pop"
        )
        assertTrue(htmlFromScriptTag.contains("https://pl999.effectivegatecpm.com/ab/cd/invoke.js"))
        assertTrue(htmlFromScriptTag.contains("https://adsterra.example.com/pop"))

        val htmlFromRawScriptUrl = com.example.data.model.AdGatePolicy.buildAdsterraPopUnderHtml(
            popUnderScript = "//pl999.effectivegatecpm.com/ab/cd/invoke.js",
            adUrl = ""
        )
        assertTrue(htmlFromRawScriptUrl.contains("<script type=\"text/javascript\" src=\"https://pl999.effectivegatecpm.com/ab/cd/invoke.js\"></script>"))
    }

    @Test
    fun testAdminSecurityPolicyPasswordChange() {
        val defaultHash = com.example.data.model.AdminSecurityPolicy.hashPasscode("admin123")
        assertTrue(com.example.data.model.AdminSecurityPolicy.verifyPasscode("admin123", defaultHash))
        assertTrue(!com.example.data.model.AdminSecurityPolicy.verifyPasscode("wrong", defaultHash))

        // Wrong current password
        val wrongCurrent = com.example.data.model.AdminSecurityPolicy.validatePasswordChange(
            currentInput = "wrongpass",
            newPassword = "newpass99",
            confirmPassword = "newpass99",
            storedHash = defaultHash
        )
        assertTrue(wrongCurrent is com.example.data.model.PasswordChangeValidation.Invalid)

        // Too short new password
        val tooShort = com.example.data.model.AdminSecurityPolicy.validatePasswordChange(
            currentInput = "admin123",
            newPassword = "123",
            confirmPassword = "123",
            storedHash = defaultHash
        )
        assertTrue(tooShort is com.example.data.model.PasswordChangeValidation.Invalid)

        // Mismatched confirmation
        val mismatch = com.example.data.model.AdminSecurityPolicy.validatePasswordChange(
            currentInput = "admin123",
            newPassword = "newpass99",
            confirmPassword = "different99",
            storedHash = defaultHash
        )
        assertTrue(mismatch is com.example.data.model.PasswordChangeValidation.Invalid)

        // Valid change -> old password no longer verifies, new password verifies
        val valid = com.example.data.model.AdminSecurityPolicy.validatePasswordChange(
            currentInput = "admin123",
            newPassword = "cinetraSecure2026",
            confirmPassword = "cinetraSecure2026",
            storedHash = defaultHash
        )
        assertTrue(valid is com.example.data.model.PasswordChangeValidation.Valid)
        val newHash = (valid as com.example.data.model.PasswordChangeValidation.Valid).newHash
        assertTrue(com.example.data.model.AdminSecurityPolicy.verifyPasscode("cinetraSecure2026", newHash))
        assertTrue(!com.example.data.model.AdminSecurityPolicy.verifyPasscode("admin123", newHash))
    }

    @Test
    fun testPlayerControlsPolicyHelpers() {
        assertEquals("+10s", com.example.ui.components.PlayerControlsPolicy.seekDeltaLabel(10))
        assertEquals("-10s", com.example.ui.components.PlayerControlsPolicy.seekDeltaLabel(-10))

        // Volume fraction <-> AudioManager stream index mapping
        assertEquals(0, com.example.ui.components.PlayerControlsPolicy.volumeFractionToStreamIndex(0f, 15))
        assertEquals(8, com.example.ui.components.PlayerControlsPolicy.volumeFractionToStreamIndex(0.5f, 15))
        assertEquals(15, com.example.ui.components.PlayerControlsPolicy.volumeFractionToStreamIndex(1.4f, 15))
        assertEquals(0f, com.example.ui.components.PlayerControlsPolicy.streamIndexToVolumeFraction(5, 0), 0.001f)
        assertEquals(0.5f, com.example.ui.components.PlayerControlsPolicy.streamIndexToVolumeFraction(5, 10), 0.001f)

        // Volume label formatting
        assertEquals("Muted", com.example.ui.components.PlayerControlsPolicy.volumePercentLabel(0f, isMuted = false))
        assertEquals("Muted", com.example.ui.components.PlayerControlsPolicy.volumePercentLabel(0.7f, isMuted = true))
        assertEquals("75%", com.example.ui.components.PlayerControlsPolicy.volumePercentLabel(0.75f, isMuted = false))

        // JS script & HTML verification
        val seekFwdScript = com.example.ui.components.PlayerControlsPolicy.buildSeekScript(10)
        assertTrue(seekFwdScript.contains("var delta = 10;"))
        assertTrue(seekFwdScript.contains("ArrowRight"))

        val seekBackScript = com.example.ui.components.PlayerControlsPolicy.buildSeekScript(-10)
        assertTrue(seekBackScript.contains("var delta = -10;"))
        assertTrue(seekBackScript.contains("ArrowLeft"))

        val volScript = com.example.ui.components.PlayerControlsPolicy.buildVolumeScript(0.65f, isMuted = false)
        assertTrue(volScript.contains("var vol = 0.65;"))
        assertTrue(volScript.contains("var muted = false;"))

        val iframeHtml = com.example.ui.components.PlayerControlsPolicy.buildIframeHtml("https://vidly.pro/movie/123")
        assertTrue(iframeHtml.contains("src=\"https://vidly.pro/movie/123\""))
        assertTrue(iframeHtml.contains("allowfullscreen=\"true\""))
    }

    @Test
    fun testTmdbTvDetailsApplyToMediaItemMultiSeasonAnime() {
        val base = MediaItem(
            id = "37854",
            tmdbId = 37854L,
            title = "Attack on Titan",
            overview = "Humanity fights Titans.",
            posterPath = "/aot.jpg",
            mediaType = "tv",
            vidlyId = "37854",
            numberOfSeasons = 1,
            episodesPerSeason = 10
        )

        val dto = com.example.data.model.TmdbTvDetailsDto(
            id = 37854L,
            name = "Attack on Titan",
            numberOfSeasons = 4,
            numberOfEpisodes = 89,
            genres = listOf(
                com.example.data.model.TmdbGenreDto(16, "Animation"),
                com.example.data.model.TmdbGenreDto(10759, "Action & Adventure")
            ),
            seasons = listOf(
                com.example.data.model.TmdbSeasonSummaryDto(
                    seasonNumber = 0,
                    name = "Specials",
                    episodeCount = 8
                ),
                com.example.data.model.TmdbSeasonSummaryDto(
                    seasonNumber = 2,
                    name = "Season 2",
                    episodeCount = 12
                ),
                com.example.data.model.TmdbSeasonSummaryDto(
                    seasonNumber = 1,
                    name = "Season 1",
                    episodeCount = 25
                ),
                com.example.data.model.TmdbSeasonSummaryDto(
                    seasonNumber = 3,
                    name = "Season 3",
                    episodeCount = 22
                ),
                com.example.data.model.TmdbSeasonSummaryDto(
                    seasonNumber = 4,
                    name = "The Final Season",
                    episodeCount = 30
                ),
                com.example.data.model.TmdbSeasonSummaryDto(
                    seasonNumber = 5,
                    name = "Placeholder",
                    episodeCount = 0
                )
            )
        )

        val enriched = dto.applyToMediaItem(base)

        assertEquals(4, enriched.numberOfSeasons)
        assertEquals(25, enriched.episodesPerSeason)
        assertEquals(89, enriched.totalEpisodeCount())
        assertEquals(listOf("Animation", "Action & Adventure"), enriched.genres)

        // Regular seasons sorted 1..4 followed by Specials (0), zero-episode placeholder filtered out
        assertEquals(listOf(1, 2, 3, 4, 0), enriched.resolvedSeasonList().map { it.seasonNumber })
        assertEquals(1, enriched.defaultSeasonNumber())
        assertEquals(25, enriched.episodeCountForSeason(1))
        assertEquals(12, enriched.episodeCountForSeason(2))
        assertEquals(22, enriched.episodeCountForSeason(3))
        assertEquals(30, enriched.episodeCountForSeason(4))
        assertEquals(8, enriched.episodeCountForSeason(0))
    }

    @Test
    fun testTmdbSeasonDetailsToEpisodeInfoList() {
        val seasonDto = com.example.data.model.TmdbSeasonDetailsDto(
            seasonNumber = 2,
            episodes = listOf(
                com.example.data.model.TmdbEpisodeDto(
                    episodeNumber = 3,
                    seasonNumber = 2,
                    name = "Could Be Worse",
                    overview = "Third episode overview",
                    runtime = 24,
                    stillPath = "/still3.jpg",
                    airDate = "2017-04-15"
                ),
                com.example.data.model.TmdbEpisodeDto(
                    episodeNumber = 1,
                    seasonNumber = null,
                    name = "",
                    runtime = 0
                )
            )
        )

        val episodes = seasonDto.toEpisodeInfoList(fallbackSeasonNumber = 2)

        assertEquals(2, episodes.size)
        assertEquals(1, episodes[0].episodeNumber)
        assertEquals(2, episodes[0].seasonNumber)
        assertEquals("Episode 1", episodes[0].name)
        assertEquals(null, episodes[0].runtimeMinutes)
        assertEquals("", episodes[0].fullStillUrl)

        assertEquals(3, episodes[1].episodeNumber)
        assertEquals("Could Be Worse", episodes[1].name)
        assertEquals(24, episodes[1].runtimeMinutes)
        assertEquals("https://image.tmdb.org/t/p/w500/still3.jpg", episodes[1].fullStillUrl)
    }

    @Test
    fun testMediaItemSeasonFallbackWithoutTmdbDetails() {
        val fallbackItem = MediaItem(
            id = "custom_1",
            title = "Custom Series",
            overview = "Desc",
            posterPath = "/p.jpg",
            mediaType = "tv",
            vidlyId = "999",
            numberOfSeasons = 3,
            episodesPerSeason = 12
        )

        assertEquals(999L, fallbackItem.resolvedTmdbId())
        assertEquals(listOf(1, 2, 3), fallbackItem.resolvedSeasonList().map { it.seasonNumber })
        assertEquals(12, fallbackItem.episodeCountForSeason(2))
        assertEquals(36, fallbackItem.totalEpisodeCount())
    }

    @Test
    fun testDarkThemePaletteMappingAndTogglePolicy() {
        // Storage key roundtrip
        assertEquals(DarkThemePalette.VELVET_CINEMA, DarkThemePalette.fromKey(null))
        assertEquals(DarkThemePalette.VELVET_CINEMA, DarkThemePalette.fromKey("unknown"))
        assertEquals(DarkThemePalette.CRISP_SLATE, DarkThemePalette.fromKey("crisp_slate"))
        assertEquals(DarkThemePalette.WARM_AMBER_NIGHT, DarkThemePalette.fromKey("warm_amber_night"))

        // Readability flags
        assertFalse(DarkThemePalette.VELVET_CINEMA.isHighReadability)
        assertTrue(DarkThemePalette.CRISP_SLATE.isHighReadability)
        assertTrue(DarkThemePalette.WARM_AMBER_NIGHT.isHighReadability)

        // Toggle switches from default -> preferred high-readability palette
        val toggledOn = ThemeTogglePolicy.toggleHighReadability(
            current = DarkThemePalette.VELVET_CINEMA,
            preferredHighReadability = DarkThemePalette.WARM_AMBER_NIGHT
        )
        assertEquals(DarkThemePalette.WARM_AMBER_NIGHT, toggledOn)

        // Toggling from a non-readable preferred fallback defaults safely to CRISP_SLATE
        val toggledFallback = ThemeTogglePolicy.toggleHighReadability(
            current = DarkThemePalette.VELVET_CINEMA,
            preferredHighReadability = DarkThemePalette.VELVET_CINEMA
        )
        assertEquals(DarkThemePalette.CRISP_SLATE, toggledFallback)

        // Toggling off from any high-readability palette returns to VELVET_CINEMA
        assertEquals(
            DarkThemePalette.VELVET_CINEMA,
            ThemeTogglePolicy.toggleHighReadability(DarkThemePalette.CRISP_SLATE)
        )
        assertEquals(
            DarkThemePalette.VELVET_CINEMA,
            ThemeTogglePolicy.toggleHighReadability(DarkThemePalette.WARM_AMBER_NIGHT)
        )

        // Material ColorScheme & palette tokens are distinct per palette
        val velvet = paletteColorsFor(DarkThemePalette.VELVET_CINEMA)
        val slate = paletteColorsFor(DarkThemePalette.CRISP_SLATE)
        val amber = paletteColorsFor(DarkThemePalette.WARM_AMBER_NIGHT)

        assertNotEquals(velvet.background, slate.background)
        assertNotEquals(slate.background, amber.background)
        assertNotEquals(velvet.textSecondary, slate.textSecondary)

        val slateScheme = buildMaterialColorScheme(slate)
        assertEquals(slate.background, slateScheme.background)
        assertEquals(slate.textPrimary, slateScheme.onBackground)
        assertEquals(slate.primary, slateScheme.primary)
    }

    @Test
    fun testContinueWatchingPolicyAndMetadataPreservation() {
        // Season/episode normalization
        assertEquals(1 to 1, com.example.data.model.ContinueWatchingPolicy.normalizeSeasonEpisode("movie", 3, 7))
        assertEquals(2 to 5, com.example.data.model.ContinueWatchingPolicy.normalizeSeasonEpisode("tv", 2, 5))
        assertEquals(0 to 1, com.example.data.model.ContinueWatchingPolicy.normalizeSeasonEpisode("tv", 0, 0))

        // Progress calculation from real seconds
        assertEquals(0, com.example.data.model.ContinueWatchingPolicy.computeProgressPercent(0L, 0L))
        assertTrue(com.example.data.model.ContinueWatchingPolicy.computeProgressPercent(120L, 0L) > 0)
        assertEquals(50, com.example.data.model.ContinueWatchingPolicy.computeProgressPercent(1800L, 3600L))
        assertEquals(100, com.example.data.model.ContinueWatchingPolicy.computeProgressPercent(4000L, 3600L))

        // Duration formatting
        assertEquals("", com.example.data.model.ContinueWatchingPolicy.formatWatchedDuration(0L))
        assertEquals("45s", com.example.data.model.ContinueWatchingPolicy.formatWatchedDuration(45L))
        assertEquals("14m", com.example.data.model.ContinueWatchingPolicy.formatWatchedDuration(845L))
        assertEquals("1h 30m", com.example.data.model.ContinueWatchingPolicy.formatWatchedDuration(5400L))

        // Entity mapping preserves full metadata
        val entity = com.example.data.model.ContinueWatchingEntity(
            id = "custom_anime_1",
            tmdbId = 37854L,
            title = "Attack on Titan",
            overview = "Humanity fights Titans",
            posterPath = "/aot.jpg",
            backdropPath = "/aot_bg.jpg",
            mediaType = "tv",
            releaseDate = "2013-04-07",
            voteAverage = 8.7,
            genres = "Animation, Action",
            numberOfSeasons = 4,
            episodesPerSeason = 25,
            season = 3,
            episode = 12,
            progressPercent = 40,
            watchedSeconds = 600L,
            durationSeconds = 1500L,
            lastWatchedAt = 1_700_000_000_000L,
            vidlyId = "37854"
        )

        val restored = entity.toMediaItem()
        assertEquals(37854L, restored.tmdbId)
        assertEquals("2013-04-07", restored.releaseDate)
        assertEquals(8.7, restored.voteAverage, 0.01)
        assertEquals(listOf("Animation", "Action"), restored.genres)
        assertEquals(4, restored.numberOfSeasons)
        assertEquals(25, restored.episodesPerSeason)
        assertTrue(com.example.data.model.ContinueWatchingPolicy.shouldPreserveProgress(entity, "tv", 3, 12))
        assertFalse(com.example.data.model.ContinueWatchingPolicy.shouldPreserveProgress(entity, "tv", 3, 13))
        assertTrue(com.example.data.model.ContinueWatchingPolicy.shouldPreserveProgress(entity, "movie", 1, 1))
    }

    @Test
    fun testEmbedNavigationAllowlistAndDirectMediaDetection() {
        val primaryUrl = "https://vidsrc.to/embed/movie/693134"

        // Same host & legitimate mirror redirects in main frame allowed
        assertTrue(
            com.example.ui.components.PlayerControlsPolicy.shouldAllowNavigation(
                url = "https://vidsrc.to/embed/movie/693134",
                currentStreamUrl = primaryUrl,
                isForMainFrame = true,
                isRedirect = false
            )
        )
        assertTrue(
            com.example.ui.components.PlayerControlsPolicy.shouldAllowNavigation(
                url = "https://vidsrc.xyz/embed/movie/693134",
                currentStreamUrl = primaryUrl,
                isForMainFrame = true,
                isRedirect = true
            )
        )

        // Subframe mirror video players (e.g. cloudnestra, megacloud, cdn) allowed
        assertTrue(
            com.example.ui.components.PlayerControlsPolicy.shouldAllowNavigation(
                url = "https://cloudnestra.com/rcp/abc123",
                currentStreamUrl = primaryUrl,
                isForMainFrame = false,
                isRedirect = false
            )
        )
        assertTrue(
            com.example.ui.components.PlayerControlsPolicy.shouldAllowNavigation(
                url = "https://megacloud.blog/embed-2/e-1/xyz",
                currentStreamUrl = primaryUrl,
                isForMainFrame = false,
                isRedirect = false
            )
        )

        // Intent / market / ad-network popups are blocked
        assertFalse(
            com.example.ui.components.PlayerControlsPolicy.shouldAllowNavigation(
                url = "intent://promo.example.com/#Intent;scheme=https;end",
                currentStreamUrl = primaryUrl,
                isForMainFrame = true,
                isRedirect = false
            )
        )
        assertFalse(
            com.example.ui.components.PlayerControlsPolicy.shouldAllowNavigation(
                url = "https://pl1234.effectivegatecpm.com/ad.js",
                currentStreamUrl = primaryUrl,
                isForMainFrame = false,
                isRedirect = false
            )
        )
        assertFalse(
            com.example.ui.components.PlayerControlsPolicy.shouldAllowNavigation(
                url = "https://spam-casino-popup.example.com/offer",
                currentStreamUrl = primaryUrl,
                isForMainFrame = true,
                isRedirect = false
            )
        )

        // Direct MP4 / HLS detection & mobile fullscreen iframe attributes
        assertTrue(com.example.ui.components.PlayerControlsPolicy.isDirectMediaFileUrl("https://cdn.example.com/video.mp4?token=abc"))
        assertTrue(com.example.ui.components.PlayerControlsPolicy.isDirectMediaFileUrl("https://cdn.example.com/master.m3u8"))
        assertFalse(com.example.ui.components.PlayerControlsPolicy.isDirectMediaFileUrl(primaryUrl))

        val directHtml = com.example.ui.components.PlayerControlsPolicy.buildDirectVideoHtml("https://cdn.example.com/video.mp4")
        assertTrue(directHtml.contains("<video src=\"https://cdn.example.com/video.mp4\""))
        assertTrue(directHtml.contains("viewport-fit=cover"))

        val updatedIframe = com.example.ui.components.PlayerControlsPolicy.buildIframeHtml(primaryUrl)
        assertTrue(updatedIframe.contains("fullscreen *"))
        assertTrue(updatedIframe.contains("viewport-fit=cover"))

        // Custom full URL for TV series does not get concatenated onto https://vidly.pro/tv/...
        val customSeries = MediaItem(
            id = "custom_tv",
            tmdbId = 1399L,
            title = "Custom Show",
            overview = "Desc",
            posterPath = "/p.jpg",
            mediaType = "tv",
            vidlyId = "https://custom.player.example.com/watch/show-1"
        )
        assertEquals(
            "https://custom.player.example.com/watch/show-1",
            customSeries.getStreamUrl(PlayerServer.VIDLY, season = 2, episode = 4)
        )
        assertEquals(
            "https://vidsrc.to/embed/tv/1399/2/4",
            customSeries.getStreamUrl(PlayerServer.VIDSRC, season = 2, episode = 4)
        )
    }

    @Test
    fun testCatalogBatchPolicyPaginationAndTotals() {
        assertEquals(50, CatalogBatchPolicy.DEFAULT_BATCH_SIZE)
        assertEquals(25, CatalogBatchPolicy.normalizeBatchSize(25))
        assertEquals(50, CatalogBatchPolicy.normalizeBatchSize(50))
        assertEquals(25, CatalogBatchPolicy.nextBatchSize(50))
        assertEquals(50, CatalogBatchPolicy.nextBatchSize(25))

        // TMDB uses 20 items per page -> 25 needs 2 pages, 50 needs 3 pages
        assertEquals(2, CatalogBatchPolicy.pagesCountForNeededItems(25))
        assertEquals(3, CatalogBatchPolicy.pagesCountForNeededItems(50))
        assertEquals(1..3, CatalogBatchPolicy.pageRangeToFetch(startPage = 1, neededCount = 50, totalPages = 500))
        assertEquals(4..5, CatalogBatchPolicy.pageRangeToFetch(startPage = 4, neededCount = 25, totalPages = 500))
        assertEquals(499..500, CatalogBatchPolicy.pageRangeToFetch(startPage = 499, neededCount = 50, totalPages = 900))
        assertTrue(CatalogBatchPolicy.pageRangeToFetch(startPage = 501, neededCount = 50, totalPages = 500).isEmpty())

        // Formatted totals & availability labels
        assertEquals("948,210", CatalogBatchPolicy.formatCount(948210))
        assertEquals(
            "Showing 50 of 948,210 available",
            CatalogBatchPolicy.formatAvailabilitySummary(shownCount = 50, totalAvailable = 948210)
        )
        assertEquals(
            "Showing all 18 titles",
            CatalogBatchPolicy.formatAvailabilitySummary(shownCount = 18, totalAvailable = 18)
        )
        assertEquals(
            "50 / 20,000",
            CatalogBatchPolicy.formatCompactHeaderTotal(shownCount = 50, totalAvailable = 20000)
        )

        val customItems = listOf(
            MediaItem(id = "c1", title = "Custom Movie", overview = "", posterPath = "/1.jpg", mediaType = "movie"),
            MediaItem(id = "c2", title = "Custom Series", overview = "", posterPath = "/2.jpg", mediaType = "tv")
        )
        assertEquals(
            501,
            CatalogBatchPolicy.resolveExploreTotalCount(
                selectedCategory = "movie",
                moviesTotalResults = 500,
                tvTotalResults = 300,
                trendingTotalResults = 1000,
                customMedia = customItems,
                shownCount = 50
            )
        )
        assertEquals(
            802,
            CatalogBatchPolicy.resolveExploreTotalCount(
                selectedCategory = "All",
                moviesTotalResults = 500,
                tvTotalResults = 300,
                trendingTotalResults = 1000,
                customMedia = customItems,
                shownCount = 100
            )
        )
    }

    @Test
    fun testFirestoreSyncMappersRoundtripAndAdCooldownReset() {
        val custom = CustomMediaEntity(
            id = "custom_123",
            title = "Inception",
            overview = "Dream heist",
            posterPath = "https://image.tmdb.org/t/p/w500/inception.jpg",
            backdropPath = "https://image.tmdb.org/t/p/w1280/inception_bg.jpg",
            mediaType = "movie",
            releaseDate = "2010",
            voteAverage = 8.8,
            genres = "Action, Sci-Fi",
            certification = "PG-13",
            vidlyId = "27205",
            tmdbId = 27205L,
            numberOfSeasons = 1,
            episodesPerSeason = 1,
            createdAt = 1_700_000_000_000L
        )
        val customMap = CatalogSyncMappers.customMediaToMap(custom)
        val decodedCustom = CatalogSyncMappers.customMediaFromMap("custom_123", customMap)!!
        assertEquals("Inception", decodedCustom.title)
        assertEquals("PG-13", decodedCustom.certification)
        assertEquals(27205L, decodedCustom.tmdbId)
        assertEquals(8.8, decodedCustom.voteAverage, 0.01)

        val announcement = AdminAnnouncementEntity(
            id = 4242,
            title = "Server Upgrade",
            message = "All 4K mirrors active",
            type = "UPDATE",
            active = true,
            createdAt = 1_700_000_001_000L
        )
        val annMap = CatalogSyncMappers.announcementToMap(announcement)
        val decodedAnn = CatalogSyncMappers.announcementFromMap("ann_4242", annMap)!!
        assertEquals(4242, decodedAnn.id)
        assertEquals("Server Upgrade", decodedAnn.title)
        assertEquals("UPDATE", decodedAnn.type)
        assertTrue(decodedAnn.active)

        // Remote AdConfig preserves user's local completion timestamp unless admin reset is newer
        val localAd = AdConfigEntity(
            id = 1,
            isEnabled = true,
            adUrl = "https://ad.example.com",
            lastCompletedAtMillis = 2_000L,
            cooldownResetAtMillis = 1_000L
        )
        val remoteConfigWithoutReset = CatalogSyncMappers.adConfigToMap(
            AdConfigEntity(
                isEnabled = true,
                adUrl = "https://ad.new.example.com",
                headline = "Updated Headline",
                cooldownResetAtMillis = 1_000L,
                updatedAt = 3_000L
            )
        )
        val mergedPreserved = CatalogSyncMappers.adConfigFromMap(remoteConfigWithoutReset, localAd)!!
        assertEquals("Updated Headline", mergedPreserved.headline)
        assertEquals(2_000L, mergedPreserved.lastCompletedAtMillis)

        // When admin triggers Reset 6h Timer (cooldownResetAtMillis > local lastCompletedAtMillis), local timer resets to 0
        val remoteConfigWithReset = CatalogSyncMappers.adConfigToMap(
            AdConfigEntity(
                isEnabled = true,
                adUrl = "https://ad.new.example.com",
                cooldownResetAtMillis = 5_000L,
                updatedAt = 5_000L
            )
        )
        val mergedReset = CatalogSyncMappers.adConfigFromMap(remoteConfigWithReset, localAd)!!
        assertEquals(0L, mergedReset.lastCompletedAtMillis)
    }

    @Test
    fun testCatalogOverridePolicyAppliesCertificationAndEditsToMatchingTmdbItems() {
        val tmdbTrendingItem = MediaItem(
            id = "27205",
            tmdbId = 27205L,
            title = "Inception",
            overview = "Original TMDB overview",
            posterPath = "/orig.jpg",
            mediaType = "movie",
            releaseDate = "2010-07-16",
            voteAverage = 8.4,
            genres = listOf("Action", "Sci-Fi"),
            certification = "",
            vidlyId = "27205"
        )
        val adminEditedOverride = CustomMediaEntity(
            id = "custom_inception",
            tmdbId = 27205L,
            title = "Inception (Director's Cut)",
            overview = "Edited in Admin Gateway",
            posterPath = "https://image.tmdb.org/t/p/w500/orig.jpg",
            mediaType = "movie",
            releaseDate = "2010",
            voteAverage = 9.2,
            genres = "Action, Sci-Fi, Mystery",
            certification = "PG-13",
            vidlyId = "27205"
        ).toMediaItem()

        val overlaid = CatalogOverridePolicy.applyAdminOverrides(
            items = listOf(tmdbTrendingItem),
            customMedia = listOf(adminEditedOverride)
        )
        assertEquals(1, overlaid.size)
        assertEquals("Inception (Director's Cut)", overlaid.first().title)
        assertEquals("PG-13", overlaid.first().certification)
        assertEquals(9.2, overlaid.first().voteAverage, 0.01)
        assertTrue(overlaid.first().genres.contains("Mystery"))
    }

    @Test
    fun testTmdbCertificationExtractionPrefersUsFallback() {
        val movieResp = TmdbMovieReleaseDatesResponse(
            id = 27205L,
            results = listOf(
                TmdbCountryReleaseDatesDto(
                    iso31661 = "GB",
                    releaseDates = listOf(TmdbReleaseDateEntryDto(certification = "12A"))
                ),
                TmdbCountryReleaseDatesDto(
                    iso31661 = "US",
                    releaseDates = listOf(TmdbReleaseDateEntryDto(certification = "PG-13"))
                )
            )
        )
        assertEquals("PG-13", movieResp.extractBestCertification())

        val tvResp = TmdbTvContentRatingsResponse(
            id = 1399L,
            results = listOf(
                TmdbTvRatingEntryDto(iso31661 = "DE", rating = "16"),
                TmdbTvRatingEntryDto(iso31661 = "US", rating = "TV-MA")
            )
        )
        assertEquals("TV-MA", tvResp.extractBestCertification())
    }

    @Test
    fun testContentFeedHeaderTotalsUnderFiltersAndSearch() {
        // Home database total aggregates movies + TV + admin custom items
        assertEquals(
            12505,
            CatalogBatchPolicy.resolveHomeDatabaseTotal(
                moviesTotalResults = 8000,
                tvTotalResults = 4500,
                trendingTotalResults = 1000,
                customMediaCount = 5,
                loadedUniqueCount = 120
            )
        )
        assertEquals(
            10,
            CatalogBatchPolicy.resolveHomeDatabaseTotal(
                moviesTotalResults = 0,
                tvTotalResults = 0,
                trendingTotalResults = 0,
                customMediaCount = 3,
                loadedUniqueCount = 10
            )
        )

        // Explore unfiltered catalog uses full database total
        assertEquals(
            501,
            CatalogBatchPolicy.resolveExploreFilteredTotal(
                searchQuery = "",
                selectedCategory = "movie",
                selectedGenre = "All",
                catalogTotalAvailable = 501,
                searchTotalResults = 0,
                filteredShownCount = 50
            )
        )
        // Applying a genre filter switches to filtered count
        assertEquals(
            14,
            CatalogBatchPolicy.resolveExploreFilteredTotal(
                searchQuery = "",
                selectedCategory = "All",
                selectedGenre = "Sci-Fi",
                catalogTotalAvailable = 90000,
                searchTotalResults = 0,
                filteredShownCount = 14
            )
        )
        // Applying a search query uses TMDB search total when type/genre filters are All
        assertEquals(
            148,
            CatalogBatchPolicy.resolveExploreFilteredTotal(
                searchQuery = "inception",
                selectedCategory = "All",
                selectedGenre = "All",
                catalogTotalAvailable = 90000,
                searchTotalResults = 148,
                filteredShownCount = 50
            )
        )
        // Narrowing search results by category filter updates to filtered shown count
        assertEquals(
            9,
            CatalogBatchPolicy.resolveExploreFilteredTotal(
                searchQuery = "inception",
                selectedCategory = "tv",
                selectedGenre = "All",
                catalogTotalAvailable = 90000,
                searchTotalResults = 148,
                filteredShownCount = 9
            )
        )

        assertEquals("12,505 in database", CatalogBatchPolicy.formatTotalChipText(12505, isFiltered = false))
        assertEquals("14 matching", CatalogBatchPolicy.formatTotalChipText(14, isFiltered = true))
        assertEquals("Syncing...", CatalogBatchPolicy.formatTotalChipText(14, isFiltered = true, isLoading = true))
    }

    @Test
    fun testDiscoverFiltersMatchingSortingAndTmdbParams() {
        val sample = listOf(
            MediaItem(
                id = "m1",
                title = "Old Boy",
                overview = "Mystery thriller",
                posterPath = "/1.jpg",
                mediaType = "movie",
                releaseDate = "2003-11-21",
                voteAverage = 8.3,
                genres = listOf("Thriller", "Mystery"),
                originalLanguage = "ko",
                originCountry = listOf("KR")
            ),
            MediaItem(
                id = "m2",
                title = "New Sci-Fi Hit",
                overview = "Space adventure",
                posterPath = "/2.jpg",
                mediaType = "movie",
                releaseDate = "2025-05-10",
                voteAverage = 7.4,
                genres = listOf("Sci-Fi", "Action"),
                originalLanguage = "en",
                originCountry = listOf("US")
            ),
            MediaItem(
                id = "t1",
                title = "Seoul Series",
                overview = "Drama show",
                posterPath = "/3.jpg",
                mediaType = "tv",
                releaseDate = "2024-02-01",
                voteAverage = 9.1,
                genres = listOf("Drama", "Mystery"),
                originalLanguage = "ko",
                originCountry = listOf("KR")
            )
        )

        val defaultFilters = DiscoverFilters()
        assertFalse(defaultFilters.hasAdvancedFilters())
        assertEquals(0, defaultFilters.activeFilterCount())

        val koreanMystery = DiscoverFilters(
            mediaType = "tv",
            selectedGenres = setOf("Mystery"),
            releaseYear = 2024,
            minRating = 8.0,
            originalLanguage = "ko",
            originCountry = "KR",
            sortBy = "rating"
        )
        assertTrue(koreanMystery.hasAdvancedFilters())
        assertEquals(7, koreanMystery.activeFilterCount())
        assertEquals("9648", koreanMystery.tvGenreIdsQuery())
        assertEquals("vote_average.desc", koreanMystery.toTmdbTvSortParam())

        val matched = sample.filter { koreanMystery.matchesItem(it) }
        assertEquals(listOf("t1"), matched.map { it.id })

        val movieSortFilters = DiscoverFilters(
            mediaType = "movie",
            selectedGenres = setOf("Sci-Fi", "Action"),
            sortBy = "newest"
        )
        assertEquals("primary_release_date.desc", movieSortFilters.toTmdbMovieSortParam())
        assertEquals("878,28", movieSortFilters.movieGenreIdsQuery())
        val sortedByRating = koreanMystery.sortItems(sample)
        assertEquals(listOf("t1", "m1", "m2"), sortedByRating.map { it.id })
        val sortedByNewest = movieSortFilters.sortItems(sample)
        assertEquals(listOf("m2", "t1", "m1"), sortedByNewest.map { it.id })
    }

    @Test
    fun testRecommendationPolicyRankingAndFallback() {
        val pool = listOf(
            MediaItem(id = "1", title = "Already Watched", overview = "", posterPath = "/1.jpg", voteAverage = 9.5, genres = listOf("Sci-Fi")),
            MediaItem(id = "2", title = "Top Sci-Fi Pick", overview = "", posterPath = "/2.jpg", voteAverage = 8.2, genres = listOf("Sci-Fi", "Action")),
            MediaItem(id = "3", title = "Pure Comedy", overview = "", posterPath = "/3.jpg", voteAverage = 8.9, genres = listOf("Comedy")),
            MediaItem(id = "4", title = "Action Hit", overview = "", posterPath = "/4.jpg", voteAverage = 7.9, genres = listOf("Action"))
        )

        // Empty history fallback ranks by rating
        val emptyHistoryRanked = RecommendationPolicy.rankRecommendedForYou(
            candidatePool = pool,
            continueWatching = emptyList(),
            watchlist = emptyList()
        )
        assertEquals(listOf("1", "3", "2", "4"), emptyHistoryRanked.map { it.id })
        assertEquals("Action", RecommendationPolicy.resolveTopPreferredGenre(emptyList(), emptyList()))
        assertEquals(null, RecommendationPolicy.resolveBecauseYouWatchedAnchor(emptyList(), emptyList()))

        // With ContinueWatching + Watchlist history, unseen titles matching preferred genres rise & watched is excluded
        val cw = listOf(
            ContinueWatchingEntity(
                id = "1",
                tmdbId = 101L,
                title = "Already Watched",
                overview = "",
                posterPath = "/1.jpg",
                mediaType = "movie",
                genres = "Sci-Fi, Action",
                progressPercent = 65
            )
        )
        val wl = listOf(
            WatchlistEntity(
                id = "2",
                tmdbId = 102L,
                title = "Top Sci-Fi Pick",
                overview = "",
                posterPath = "/2.jpg",
                mediaType = "movie",
                genres = "Sci-Fi"
            )
        )
        assertEquals("Sci-Fi", RecommendationPolicy.resolveTopPreferredGenre(cw, wl))
        assertEquals("Already Watched", RecommendationPolicy.resolveBecauseYouWatchedAnchor(cw, wl)?.title)

        val personalized = RecommendationPolicy.rankRecommendedForYou(pool, cw, wl)
        assertFalse(personalized.any { it.id == "1" })
        assertEquals("2", personalized.first().id)
    }

    @Test
    fun testIssueReportEnumsAndFirestoreMapperRoundtrip() {
        assertEquals(8, IssueReportCategory.entries.size)
        assertEquals(IssueReportCategory.VIDEO_NOT_WORKING, IssueReportCategory.fromKey("video_not_working"))
        assertEquals(IssueReportCategory.BROKEN_STREAM_LINK, IssueReportCategory.fromKey("Broken Streaming Link"))
        assertEquals(IssueReportCategory.OTHER, IssueReportCategory.fromKey("nonexistent"))

        assertEquals(IssueReportStatus.IN_REVIEW, IssueReportStatus.fromKey("IN_REVIEW"))
        assertEquals(IssueReportStatus.RESOLVED, IssueReportStatus.fromKey("resolved"))
        assertEquals(IssueReportStatus.OPEN, IssueReportStatus.fromKey(null))

        val report = IssueReportEntity(
            id = "rep_abc123",
            category = IssueReportCategory.WRONG_SUBTITLE.storageKey,
            description = "English subtitles are out of sync by 4 seconds.",
            mediaId = "693134",
            mediaTitle = "Dune: Part Two",
            mediaType = "movie",
            serverName = "Vidly Pro",
            episodeLabel = "",
            screenshotPreviewBase64 = "ZmFrZUJhc2U2NA==",
            status = IssueReportStatus.IN_REVIEW.storageKey,
            adminNotes = "Switching subtitle mirror.",
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_500_000L
        )
        val map = CatalogSyncMappers.issueReportToMap(report)
        val decoded = CatalogSyncMappers.issueReportFromMap("rep_abc123", map)!!

        assertEquals(report.id, decoded.id)
        assertEquals(report.category, decoded.category)
        assertEquals(report.description, decoded.description)
        assertEquals(report.mediaTitle, decoded.mediaTitle)
        assertEquals(report.serverName, decoded.serverName)
        assertEquals(report.screenshotPreviewBase64, decoded.screenshotPreviewBase64)
        assertEquals("IN_REVIEW", decoded.status)
        assertEquals("Switching subtitle mirror.", decoded.adminNotes)
    }

    @Test
    fun testUpgradeThemesAccentColorsAndLanguages() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromKey("light"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey("system"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromKey("invalid"))

        assertEquals(AppLanguageOption.JAPANESE, AppLanguageOption.fromCode("ja-JP"))
        assertEquals(AppLanguageOption.ENGLISH, AppLanguageOption.fromCode(null))

        val lightCobalt = paletteColorsFor(
            palette = DarkThemePalette.VELVET_CINEMA,
            isLight = true,
            accentOption = com.example.ui.theme.AccentColorOption.ELECTRIC_BLUE
        )
        assertTrue(lightCobalt.isLight)
        assertEquals(com.example.ui.theme.LightCinemaColors.background, lightCobalt.background)
        assertEquals(com.example.ui.theme.AccentColorOption.ELECTRIC_BLUE.primaryColor, lightCobalt.primary)

        val darkCrimson = paletteColorsFor(
            palette = DarkThemePalette.CRISP_SLATE,
            isLight = false,
            accentOption = com.example.ui.theme.AccentColorOption.CRIMSON_RED
        )
        assertFalse(darkCrimson.isLight)
        assertEquals(com.example.ui.theme.AccentColorOption.CRIMSON_RED.primaryColor, darkCrimson.primary)
        val lightScheme = buildMaterialColorScheme(lightCobalt)
        assertEquals(lightCobalt.background, lightScheme.background)
        assertEquals(lightCobalt.primary, lightScheme.primary)
    }

    @Test
    fun testUpcomingReleasePolicyStrictFutureMoviesAndTvHighPopularity() {
        val today = "2026-11-16"
        val candidates = listOf(
            // Past movie (already released) -> MUST BE EXCLUDED
            MediaItem(
                id = "past_movie",
                title = "Already Out Movie",
                overview = "",
                posterPath = "/1.jpg",
                mediaType = "movie",
                releaseDate = "2026-11-15",
                popularity = 420.0
            ),
            // Released TODAY -> MUST BE EXCLUDED from Upcoming
            MediaItem(
                id = "today_tv",
                title = "Released Today Series",
                overview = "",
                posterPath = "/2.jpg",
                mediaType = "tv",
                releaseDate = "2026-11-16",
                popularity = 510.0
            ),
            // Unparseable year-only / blank date -> MUST BE EXCLUDED
            MediaItem(
                id = "year_only",
                title = "Vague Date",
                overview = "",
                posterPath = "/3.jpg",
                mediaType = "movie",
                releaseDate = "2027",
                popularity = 300.0
            ),
            // Future date, but low popularity -> MUST BE EXCLUDED
            MediaItem(
                id = "future_low_pop",
                title = "Obscure Future Short",
                overview = "",
                posterPath = "/4.jpg",
                mediaType = "movie",
                releaseDate = "2026-12-01",
                popularity = 2.4
            ),
            // Future high-popularity TV series -> INCLUDED (#2 by popularity)
            MediaItem(
                id = "future_tv_hit",
                title = "Dune: Prophecy S2",
                overview = "",
                posterPath = "/5.jpg",
                mediaType = "tv",
                releaseDate = "2026-12-10",
                popularity = 180.5
            ),
            // Future high-popularity movie -> INCLUDED (#1 by popularity)
            MediaItem(
                id = "future_movie_hit",
                title = "Avatar: Fire & Ash",
                overview = "",
                posterPath = "/6.jpg",
                mediaType = "movie",
                releaseDate = "2026-12-19",
                popularity = 395.2
            )
        )

        val upcoming = UpcomingReleasePolicy.filterStrictUpcoming(
            items = candidates,
            todayIso = today
        )

        assertEquals(listOf("future_movie_hit", "future_tv_hit"), upcoming.map { it.id })
        assertTrue(upcoming.all { UpcomingReleasePolicy.isStrictlyFutureRelease(it.releaseDate, today) })
        assertTrue(upcoming.all { it.popularity >= UpcomingReleasePolicy.MIN_UPCOMING_POPULARITY })
    }

    @Test
    fun testNewReleasePolicyDailyUpdatesAndNoFutureLeakage() {
        val nowMillis = 1_794_844_800_000L // fixed epoch timestamp
        val today = UpcomingReleasePolicy.todayIsoDate(nowMillis)
        val yesterday = UpcomingReleasePolicy.daysAgoIsoDate(1, nowMillis)
        val tomorrow = UpcomingReleasePolicy.tomorrowIsoDate(nowMillis)

        val tmdbPool = listOf(
            // Future release must NOT appear in New Releases
            MediaItem(
                id = "future_item",
                title = "Future Movie",
                overview = "",
                posterPath = "/f.jpg",
                mediaType = "movie",
                releaseDate = tomorrow,
                popularity = 500.0
            ),
            // Released yesterday (movie)
            MediaItem(
                id = "yesterday_movie",
                title = "Yesterday Drop",
                overview = "",
                posterPath = "/y.jpg",
                mediaType = "movie",
                releaseDate = yesterday,
                popularity = 60.0
            ),
            // Released today (TV series)
            MediaItem(
                id = "today_tv_drop",
                title = "Daily Series Drop",
                overview = "",
                posterPath = "/t.jpg",
                mediaType = "tv",
                releaseDate = today,
                popularity = 42.0
            )
        )

        val adminUploadedToday = CustomMediaEntity(
            id = "custom_daily_upload",
            title = "Admin Daily Upload",
            overview = "Uploaded today",
            posterPath = "https://example.com/p.jpg",
            mediaType = "movie",
            releaseDate = today,
            voteAverage = 8.5,
            createdAt = nowMillis
        )

        val merged = NewReleasePolicy.mergeDailyNewReleases(
            dailyTmdbItems = tmdbPool,
            customMediaEntities = listOf(adminUploadedToday),
            todayIso = today,
            nowMillis = nowMillis
        )

        assertFalse(merged.any { it.id == "future_item" })
        assertEquals(
            listOf("custom_daily_upload", "today_tv_drop", "yesterday_movie"),
            merged.map { it.id }
        )
    }

    @Test
    fun testBrowseLanguagesCatalogAndDiscoveryFiltering() {
        val languages = TmdbFilterCatalog.browseLanguages
        assertTrue(languages.size >= 20)
        assertEquals(languages.size, languages.distinctBy { it.code }.size)
        assertTrue(languages.all { it.code.isNotBlank() && it.name.isNotBlank() && it.nativeName.isNotBlank() })

        // Verify originalLanguages in Explore stays in sync with browseLanguages
        val expectedCodes = listOf("") + languages.map { it.code }
        assertEquals(expectedCodes, TmdbFilterCatalog.originalLanguages.map { it.code })

        // Lookup by code (case-insensitive)
        val hindi = TmdbFilterCatalog.browseLanguageByCode("HI")
        assertEquals("Hindi", hindi?.name)
        assertEquals("HI", hindi?.badgeCode)

        val tamil = TmdbFilterCatalog.browseLanguageByCode("ta")
        assertEquals("Tamil", tamil?.name)

        // DiscoverFilters filters items cleanly for a specific language
        val catalog = listOf(
            MediaItem(id = "hi_1", title = "Hindi Film", overview = "", posterPath = "/1.jpg", mediaType = "movie", originalLanguage = "hi", voteAverage = 8.4),
            MediaItem(id = "ta_1", title = "Tamil Film", overview = "", posterPath = "/2.jpg", mediaType = "movie", originalLanguage = "ta", voteAverage = 8.1),
            MediaItem(id = "ko_1", title = "K-Drama", overview = "", posterPath = "/3.jpg", mediaType = "tv", originalLanguage = "ko", voteAverage = 9.0)
        )
        val hindiFilter = DiscoverFilters(originalLanguage = "hi")
        assertEquals(listOf("hi_1"), catalog.filter { hindiFilter.matchesItem(it) }.map { it.id })

        val koreanTvFilter = DiscoverFilters(mediaType = "tv", originalLanguage = "ko")
        assertEquals(listOf("ko_1"), catalog.filter { koreanTvFilter.matchesItem(it) }.map { it.id })
    }

    @Test
    fun testSearchSuggestionPolicyRankingAndTypeFiltering() {
        val tmdbCandidates = listOf(
            MediaItem(
                id = "1",
                title = "The Dune Chronicles",
                overview = "",
                posterPath = "/1.jpg",
                mediaType = "tv",
                popularity = 95.0,
                voteAverage = 7.9
            ),
            MediaItem(
                id = "2",
                title = "Dune: Part Two",
                overview = "",
                posterPath = "/2.jpg",
                mediaType = "movie",
                popularity = 410.0,
                voteAverage = 8.4
            ),
            MediaItem(
                id = "3",
                title = "Dune",
                overview = "",
                posterPath = "/3.jpg",
                mediaType = "movie",
                popularity = 210.0,
                voteAverage = 7.8
            ),
            MediaItem(
                id = "4",
                title = "Dune Messenger",
                overview = "",
                posterPath = "",
                mediaType = "movie",
                popularity = 350.0,
                voteAverage = 7.5
            )
        )
        val customCandidates = listOf(
            MediaItem(
                id = "custom_dune",
                title = "Dune Special Cut",
                overview = "",
                posterPath = "/c.jpg",
                mediaType = "movie",
                popularity = 120.0,
                voteAverage = 8.9
            )
        )

        assertTrue(SearchSuggestionPolicy.rankSuggestions(tmdbCandidates, customCandidates, "   ").isEmpty())

        val ranked = SearchSuggestionPolicy.rankSuggestions(
            tmdbItems = tmdbCandidates,
            customMedia = customCandidates,
            query = "dune"
        )
        // Exact match "Dune" comes first; prefix matches prioritize poster+popularity; substring "The Dune Chronicles" last
        assertEquals("3", ranked.first().id)
        assertEquals("1", ranked.last().id)
        assertTrue(ranked.any { it.id == "custom_dune" })

        val onlyTv = SearchSuggestionPolicy.filterByMediaType(ranked, "tv")
        assertEquals(listOf("1"), onlyTv.map { it.id })

        val onlyMovies = SearchSuggestionPolicy.filterByMediaType(ranked, "movie")
        assertFalse(onlyMovies.any { it.mediaType == "tv" })
        assertEquals(4, onlyMovies.size)
    }

    @Test
    fun testWebTorrentPolicyAndStreamResolution() {
        val sampleMagnet = "magnet:?xt=urn:btih:08ada5a7a6183aae1e09d831df6748d566095a10&dn=Sintel"
        val torrentUrl = "https://webtorrent.io/torrents/sintel.torrent"
        val hexHash = "08ada5a7a6183aae1e09d831df6748d566095a10"
        val base32Hash = "BCVKLJ5GDA5K4HQJ3AZ56Z6NKS3ASWQW"

        assertTrue(com.example.ui.components.WebTorrentPolicy.isMagnetUri(sampleMagnet))
        assertTrue(com.example.ui.components.WebTorrentPolicy.isTorrentFileUrl(torrentUrl))
        assertTrue(com.example.ui.components.WebTorrentPolicy.isInfoHash(hexHash))
        assertTrue(com.example.ui.components.WebTorrentPolicy.isInfoHash(base32Hash))
        assertFalse(com.example.ui.components.WebTorrentPolicy.isWebTorrentSource("27205"))
        assertFalse(com.example.ui.components.WebTorrentPolicy.isWebTorrentSource("https://vidly.pro/movie/27205"))

        val normalizedMagnet = com.example.ui.components.WebTorrentPolicy.normalizeTorrentId(sampleMagnet, "Sintel")
        assertTrue(normalizedMagnet.startsWith(sampleMagnet))
        assertTrue(normalizedMagnet.contains("openwebtorrent"))

        val normalizedFromHash = com.example.ui.components.WebTorrentPolicy.normalizeTorrentId(hexHash, "Sintel")
        assertTrue(normalizedFromHash.startsWith("magnet:?xt=urn:btih:$hexHash"))
        assertTrue(normalizedFromHash.contains("dn=Sintel"))
        assertTrue(normalizedFromHash.contains("openwebtorrent"))

        assertEquals("0 B/s", com.example.ui.components.WebTorrentPolicy.formatSpeed(0L))
        assertEquals("512 B/s", com.example.ui.components.WebTorrentPolicy.formatSpeed(512L))
        assertEquals("1.5 KB/s", com.example.ui.components.WebTorrentPolicy.formatSpeed(1536L))
        assertEquals("2.00 MB/s", com.example.ui.components.WebTorrentPolicy.formatSpeed(2L * 1024L * 1024L))

        assertEquals("1.5 MB", com.example.ui.components.WebTorrentPolicy.formatBytes(1536L * 1024L))
        assertEquals("1.00 GB", com.example.ui.components.WebTorrentPolicy.formatBytes(1024L * 1024L * 1024L))

        val html = com.example.ui.components.WebTorrentPolicy.buildWebTorrentPlayerHtml(
            torrentId = sampleMagnet,
            title = "Sintel",
            season = 1,
            episode = 2,
            isTv = true
        )
        assertTrue(html.contains("new window.WebTorrent({"))
        assertTrue(html.contains("stun:stun.l.google.com:19302"))
        assertTrue(html.contains("stun:stun.cloudflare.com:3478"))
        assertTrue(html.contains("installSafeRtcShim"))
        assertTrue(html.contains("addIceCandidate"))
        assertTrue(html.contains("setRemoteDescription"))
        assertTrue(html.contains("file.renderTo(videoEl"))
        assertTrue(html.contains("CinetraWebTorrentBridge"))
        assertTrue(html.contains("CinetraPlayerBridge"))

        // MediaItem with magnet link preserves magnet URI across servers
        val magnetMovie = MediaItem(
            id = "custom_sintel",
            title = "Sintel",
            overview = "Open movie",
            posterPath = "",
            mediaType = "movie",
            vidlyId = sampleMagnet
        )
        assertTrue(magnetMovie.isWebTorrentStream())
        assertTrue(magnetMovie.getStreamUrl(PlayerServer.VIDLY).startsWith("magnet:?xt=urn:btih:"))
        assertTrue(magnetMovie.getStreamUrl(PlayerServer.WEBTORRENT).startsWith("magnet:?xt=urn:btih:"))

        // Standard TMDB item under PlayerServer.WEBTORRENT returns blank to trigger inline magnet prompt
        val regularMovie = MediaItem(
            id = "27205",
            tmdbId = 27205L,
            title = "Inception",
            overview = "Dream",
            posterPath = "",
            mediaType = "movie",
            vidlyId = "27205"
        )
        assertFalse(regularMovie.isWebTorrentStream())
        assertEquals("", regularMovie.getStreamUrl(PlayerServer.WEBTORRENT))

        // PlayerControlsPolicy allows WebTorrent CDN/origin while still blocking ad popups
        assertTrue(
            com.example.ui.components.PlayerControlsPolicy.shouldAllowNavigation(
                url = "https://cdn.jsdelivr.net/npm/webtorrent@1.9.7/webtorrent.min.js",
                currentStreamUrl = "https://webtorrent.io/",
                isForMainFrame = false
            )
        )
    }

    @Test
    fun testMovieBoxApiParsingAndDownloadHelpers() {
        val searchJson = """
            {
              "code": 0,
              "data": {
                "items": [
                  {
                    "subjectId": "8906247916759695608",
                    "subjectType": 1,
                    "title": "Inception",
                    "releaseDate": "2010-07-16",
                    "imdbRatingValue": "8.8",
                    "detailPath": "inception-2010-abc",
                    "hasResource": true,
                    "cover": { "url": "https://img.example.com/inception.jpg" }
                  },
                  {
                    "subjectId": "111222333",
                    "subjectType": 2,
                    "title": "Inception: The Documentary Series",
                    "releaseDate": "2022-01-01",
                    "detailPath": "inception-doc",
                    "hasResource": true
                  }
                ]
              }
            }
        """.trimIndent()

        val subjects = com.example.data.remote.MovieBoxApiClient.parseSearchResponseJson(searchJson)
        assertEquals(2, subjects.size)
        assertEquals("8906247916759695608", subjects[0].subjectId)
        assertEquals("inception-2010-abc", subjects[0].detailPath)
        assertEquals("2010", subjects[0].releaseYear)

        val targetMovie = MediaItem(
            id = "27205",
            tmdbId = 27205L,
            title = "Inception",
            overview = "Dreams",
            posterPath = "",
            mediaType = "movie",
            releaseDate = "2010-07-16"
        )
        val bestSubject = com.example.data.remote.MovieBoxApiClient.pickBestSubjectCandidate(targetMovie, subjects)
        assertEquals("8906247916759695608", bestSubject?.subjectId)

        val downloadJson = """
            {
              "code": 0,
              "data": {
                "downloads": [
                  { "id": "d_360", "url": "https://cdn.example.com/v_360.mp4", "resolution": 360, "size": 314572800 },
                  { "id": "d_vip", "url": "", "resolution": 1080, "size": 2414667149, "vipLocked": true },
                  { "id": "d_720", "url": "https://cdn.example.com/v_720.mp4", "resolution": 720, "size": 734003200 }
                ],
                "streams": [
                  { "id": "s_720_dup", "url": "https://cdn2.example.com/v_720_alt.mp4", "resolutions": "720", "size": "734003200" },
                  { "id": "s_1080", "url": "https://cdn.example.com/v_1080.mp4", "resolutions": "1080", "size": "1610612736" }
                ],
                "captions": [
                  { "id": "c_en", "lan": "en", "lanName": "English", "url": "https://cdn.example.com/en.srt", "size": 45000 }
                ]
              }
            }
        """.trimIndent()

        val bundle = com.example.data.remote.MovieBoxApiClient.parseDownloadResponseJson(
            json = downloadJson,
            subject = subjects[0],
            sourceHost = "https://h5.inmoviebox.com"
        )
        assertEquals(3, bundle.options.size)
        assertEquals(1080, bundle.options[0].resolution)
        assertEquals("https://cdn.example.com/v_1080.mp4", bundle.options[0].downloadUrl)
        assertEquals("https://h5.inmoviebox.com/", bundle.options[0].refererHeader)
        assertEquals("https://cdn.example.com/v_720.mp4", bundle.options[1].downloadUrl)
        assertEquals("1080p Full HD", bundle.options[0].resolutionLabel)
        assertEquals(1, bundle.subtitles.size)
        assertEquals("English", bundle.subtitles[0].languageName)

        val picked720 = com.example.data.remote.MovieBoxApiClient.pickOptionForPreferredQuality(
            bundle.options,
            com.example.data.model.PreferredDownloadQuality.Q720P
        )
        assertEquals(720, picked720?.resolution)

        // Mobile BFF signing & client token format (OpenStream La94/Li63 compatible)
        val ts = 1763856000000L
        val clientToken = com.example.data.remote.MovieBoxApiClient.computeClientToken(ts)
        assertTrue(clientToken.startsWith("1763856000000,"))
        assertEquals(32, clientToken.substringAfter(',').length)

        val sigGetA = com.example.data.remote.MovieBoxApiClient.computeTrSignature(
            method = "GET",
            url = "https://api4sg.aoneroom.com/wefeed-mobile-bff/subject-api/play-info?se=1&subjectId=123&ep=2",
            body = null,
            timestampMillis = ts
        )
        val sigGetB = com.example.data.remote.MovieBoxApiClient.computeTrSignature(
            method = "GET",
            url = "https://api4sg.aoneroom.com/wefeed-mobile-bff/subject-api/play-info?ep=2&subjectId=123&se=1",
            body = null,
            timestampMillis = ts
        )
        assertTrue(sigGetA.startsWith("1763856000000|2|"))
        // Sorted query params produce identical HmacMD5 signatures regardless of parameter order
        assertEquals(sigGetA, sigGetB)

        val sigPost = com.example.data.remote.MovieBoxApiClient.computeTrSignature(
            method = "POST",
            url = "https://api4sg.aoneroom.com/wefeed-mobile-bff/user-api/visitor-login",
            body = "{}",
            timestampMillis = ts
        )
        assertTrue(sigPost.startsWith("1763856000000|2|"))

        // Bait clip filtering (OpenStream Lz84.b compatible)
        assertTrue(
            com.example.data.remote.MovieBoxApiClient.isBaitOrBlockedUrl(
                "https://macdn.aoneroom.com/other/2026/10/10/80da52722ee7de7a4303290af6783424.mp4"
            )
        )
        assertFalse(
            com.example.data.remote.MovieBoxApiClient.isBaitOrBlockedUrl(
                "https://bcdnxw.hakunaymatata.com/resource/2025/01/movie_1080p.mp4"
            )
        )

        // Mobile BFF resource/v2 parsing with bait filtering, extCaptions & signCookie
        val mobileResourceV2Json = """
            {
              "code": 0,
              "data": {
                "list": [
                  {
                    "resourceId": "r_bait",
                    "resourceLink": "https://macdn.aoneroom.com/other/2026/10/10/80da52722ee7de7a4303290af6783424.mp4",
                    "resolution": 480,
                    "size": 844123
                  },
                  {
                    "resourceId": "r_1080",
                    "resourceLink": "https://bcdnxw.hakunaymatata.com/resource/real_1080.mp4",
                    "resolutions": "480,720,1080",
                    "size": 2414667149,
                    "signCookie": "CloudFront-Policy=abc123",
                    "extCaptions": [
                      { "id": "ec_en", "lan": "en", "lanName": "English", "url": "https://cacdn.hakunaymatata.com/sub_en.srt", "size": 52100 }
                    ]
                  }
                ]
              }
            }
        """.trimIndent()

        val mobileBundle = com.example.data.remote.MovieBoxApiClient.parseDownloadResponseJson(
            json = mobileResourceV2Json,
            subject = subjects[0],
            sourceHost = com.example.data.remote.MovieBoxApiClient.DEFAULT_HOST,
            refererOverride = com.example.data.model.MovieBoxDownloadOption.DEFAULT_REFERER
        )
        assertEquals(1, mobileBundle.options.size)
        assertEquals(1080, mobileBundle.options[0].resolution)
        assertEquals("https://bcdnxw.hakunaymatata.com/resource/real_1080.mp4", mobileBundle.options[0].downloadUrl)
        assertEquals("CloudFront-Policy=abc123", mobileBundle.options[0].cookieHeader)
        assertEquals(com.example.data.model.MovieBoxDownloadOption.DEFAULT_REFERER, mobileBundle.options[0].refererHeader)
        assertEquals(1, mobileBundle.subtitles.size)
        assertEquals("https://cacdn.hakunaymatata.com/sub_en.srt", mobileBundle.subtitles[0].url)

        // Host classification & legacy host upgrade
        assertTrue(com.example.data.remote.MovieBoxApiClient.isMobileBackendHost("https://api4sg.aoneroom.com"))
        assertTrue(com.example.data.remote.MovieBoxApiClient.isMobileBackendHost("https://api.inmoviebox.com"))
        assertFalse(com.example.data.remote.MovieBoxApiClient.isMobileBackendHost("https://h5.inmoviebox.com"))
        assertFalse(com.example.data.remote.MovieBoxApiClient.isMobileBackendHost("https://h5-api.aoneroom.com"))
        assertEquals(
            com.example.data.remote.MovieBoxApiClient.DEFAULT_HOST,
            com.example.data.remote.MovieBoxApiClient.upgradeLegacyHostIfNeeded("https://h5-api.aoneroom.com")
        )
        assertEquals(
            com.example.data.remote.MovieBoxApiClient.DEFAULT_HOST,
            com.example.data.remote.MovieBoxApiClient.upgradeLegacyHostIfNeeded("https://h5.aoneroom.com/")
        )
        assertEquals(
            "https://my-custom-proxy.example.com",
            com.example.data.remote.MovieBoxApiClient.upgradeLegacyHostIfNeeded("my-custom-proxy.example.com/")
        )
        val hostsForLegacy = com.example.data.remote.MovieBoxApiClient.candidateHosts("https://h5-api.aoneroom.com")
        assertEquals(com.example.data.remote.MovieBoxApiClient.DEFAULT_HOST, hostsForLegacy.first())

        // TV season title matching (e.g. Breaking Bad S1-S5)
        val bbTv = MediaItem(
            id = "1396",
            tmdbId = 1396L,
            title = "Breaking Bad",
            overview = "",
            posterPath = "",
            mediaType = "tv",
            releaseDate = "2008-01-20"
        )
        val bbCand = com.example.data.model.MovieBoxSubject(
            subjectId = "6207982430134357800",
            subjectType = 2,
            title = "Breaking Bad S1",
            releaseDate = "2008-01-20",
            detailPath = "breaking-bad-ej6Bp0MCAo7",
            hasResource = true
        )
        assertTrue(com.example.data.remote.MovieBoxApiClient.scoreSubjectCandidate(bbTv, bbCand) >= 175)

        assertEquals(
            "27205_movie_1080p_full_hd",
            com.example.data.download.DownloadEngine.buildDownloadId("27205", "movie", 1, 1, "1080p Full HD")
        )
        assertEquals(
            "Breaking_Bad_S2E5_720p_HD.mp4",
            com.example.data.download.DownloadEngine.buildSafeFileName("Breaking Bad!", "tv", 2, 5, "720p HD", "mp4")
        )
        assertEquals(50, com.example.data.download.DownloadEngine.computeProgressPercent(500L, 1000L))
        assertEquals("1.50 GB", com.example.data.model.formatDownloadBytes(1610612736L))
    }

    @Test
    fun testPlayerSubresourceAdAndTrackerBlocking() {
        assertTrue(com.example.ui.components.PlayerControlsPolicy.shouldBlockPlayerSubresource("https://mc.yandex.ru/metrika/tag.js"))
        assertTrue(com.example.ui.components.PlayerControlsPolicy.shouldBlockPlayerSubresource("https://pl123.effectivegatecpm.com/ad.js"))
        assertTrue(com.example.ui.components.PlayerControlsPolicy.shouldBlockPlayerSubresource("https://s10.histats.com/js15_as.js"))
        assertTrue(com.example.ui.components.PlayerControlsPolicy.shouldBlockPlayerSubresource("https://stats.bingr.one/script.js"))
        assertTrue(com.example.ui.components.PlayerControlsPolicy.shouldBlockPlayerSubresource("https://cdn.example.com/disable-devtool.min.js"))
        assertTrue(com.example.ui.components.PlayerControlsPolicy.shouldBlockPlayerSubresource("https://cdn.example.com/anti-debugger.js"))
        assertTrue(com.example.ui.components.PlayerControlsPolicy.shouldBlockPlayerSubresource("https://cdn.example.com/popunder.min.js"))
        assertTrue(com.example.ui.components.PlayerControlsPolicy.shouldBlockPlayerSubresource("https://z.moatads.com/pixel.js"))
        assertTrue(com.example.ui.components.PlayerControlsPolicy.shouldBlockPlayerSubresource("https://vidly.pro/sw.js?v=1"))
        assertTrue(com.example.ui.components.PlayerControlsPolicy.shouldBlockPlayerSubresource("https://ads.pubmatic.com/AdServer/js/prebid.js"))
        assertTrue(com.example.ui.components.PlayerControlsPolicy.shouldBlockPlayerSubresource("https://js-agent.newrelic.com/nr-spa-1216.min.js"))
        assertTrue(com.example.ui.components.PlayerControlsPolicy.shouldBlockServiceWorkerRequest("https://vidsrc.to/service-worker.js"))
        assertTrue(com.example.ui.components.PlayerControlsPolicy.shouldBlockServiceWorkerRequest("https://stats.bingr.one/script.js"))
        assertFalse(com.example.ui.components.PlayerControlsPolicy.shouldBlockServiceWorkerRequest("https://vidly.pro/movie/693134"))
        assertFalse(com.example.ui.components.PlayerControlsPolicy.shouldBlockPlayerSubresource("https://vidly.pro/movie/693134"))
        assertFalse(com.example.ui.components.PlayerControlsPolicy.shouldBlockPlayerSubresource("https://www.youtube.com/embed/abc123"))
        assertFalse(com.example.ui.components.PlayerControlsPolicy.shouldBlockPlayerSubresource("https://cdn.jsdelivr.net/npm/webtorrent@1.9.7/webtorrent.min.js"))
    }

    @Test
    fun testDownloadEngineHeaderProfilesAndRangeHelpers() {
        val profiles = com.example.data.download.DownloadEngine.buildHeaderProfiles(
            refererHeader = "https://videodownloader.site/",
            userAgentHeader = "Mozilla/5.0 Test",
            cookieHeader = "CloudFront-Policy=abc",
            targetUrl = "https://bcdnxw.hakunaymatata.com/resource/movie.mp4"
        )
        assertTrue(profiles.size >= 3)
        assertEquals("https://videodownloader.site/", profiles[0]["Referer"])
        assertEquals("https://videodownloader.site", profiles[0]["Origin"])
        assertEquals("CloudFront-Policy=abc", profiles[0]["Cookie"])
        // Fallback profile omits Origin when CDN rejects Origin header
        assertEquals("https://videodownloader.site/", profiles[1]["Referer"])
        assertFalse(profiles[1].containsKey("Origin"))

        assertEquals(
            1223914409L,
            com.example.data.download.DownloadEngine.parseTotalBytesFromContentRange("bytes 0-1023/1223914409")
        )
        assertEquals(
            -1L,
            com.example.data.download.DownloadEngine.parseTotalBytesFromContentRange("bytes 0-1023/*")
        )
        assertTrue(com.example.data.download.DownloadEngine.isHtmlOrJsonErrorContentType("text/html; charset=utf-8"))
        assertTrue(com.example.data.download.DownloadEngine.isHtmlOrJsonErrorContentType("application/json"))
        assertFalse(com.example.data.download.DownloadEngine.isHtmlOrJsonErrorContentType("video/mp4"))
        assertTrue(
            com.example.data.download.DownloadEngine.mapHttpStatusToReadableError(429).contains("429")
        )
    }

    @Test
    fun testUnifiedLinksAndEpisodeDirectLinksCodec() {
        val streamOnly = MediaItem(
            id = "m1",
            title = "Stream Only",
            overview = "",
            posterPath = "",
            mediaType = "movie",
            directVideoUrl = "https://cdn.example.com/movie.m3u8"
        )
        assertEquals("https://cdn.example.com/movie.m3u8", streamOnly.effectiveStreamUrl())
        assertEquals("https://cdn.example.com/movie.m3u8", streamOnly.effectiveDownloadUrl())

        val downloadOnly = MediaItem(
            id = "m2",
            title = "Download Only",
            overview = "",
            posterPath = "",
            mediaType = "movie",
            downloadUrl = "https://cdn.example.com/movie.mp4"
        )
        assertEquals("https://cdn.example.com/movie.mp4", downloadOnly.effectiveStreamUrl())
        assertEquals("https://cdn.example.com/movie.mp4", downloadOnly.effectiveDownloadUrl())

        val multiline = """
            S1E1 | https://cdn.example.com/s1e1.mp4
            S01E02 | https://cdn.example.com/s1e2.m3u8 | https://cdn.example.com/s1e2.mp4
            2x3 | https://cdn.example.com/s2e3.mp4
        """.trimIndent()
        val json = com.example.data.model.EpisodeDirectLinksCodec.parseMultilineInput(multiline)
        val parsed = com.example.data.model.EpisodeDirectLinksCodec.decodeJson(json)
        assertEquals(3, parsed.size)
        assertEquals("https://cdn.example.com/s1e1.mp4", parsed["s1e1"]?.effectiveDownloadUrl())
        assertEquals("https://cdn.example.com/s1e2.m3u8", parsed["s1e2"]?.effectiveStreamUrl())
        assertEquals("https://cdn.example.com/s1e2.mp4", parsed["s1e2"]?.effectiveDownloadUrl())
        assertEquals("https://cdn.example.com/s2e3.mp4", parsed["s2e3"]?.effectiveStreamUrl())

        val roundTripText = com.example.data.model.EpisodeDirectLinksCodec.formatMultilineInput(json)
        assertTrue(roundTripText.contains("S1E1 | https://cdn.example.com/s1e1.mp4"))
        assertTrue(roundTripText.contains("S1E2 | https://cdn.example.com/s1e2.m3u8 | https://cdn.example.com/s1e2.mp4"))
    }

    @Test
    fun testPlaybackSourcePriorityLocalOverDirectOverWebEmbed() {
        val tempFile = java.io.File.createTempFile("cinetra_local_test", ".mp4")
        try {
            tempFile.writeText("fake video payload")
            val media = MediaItem(
                id = "m_prio",
                tmdbId = 27205L,
                title = "Inception",
                overview = "",
                posterPath = "",
                mediaType = "movie",
                vidlyId = "27205",
                directVideoUrl = "https://cdn.example.com/inception.m3u8"
            )
            val completedDownload = DownloadEntity(
                id = "m_prio_movie_direct",
                mediaId = "m_prio",
                tmdbId = 27205L,
                title = "Inception",
                posterPath = "",
                backdropPath = "",
                mediaType = "movie",
                sourceUrl = "https://cdn.example.com/inception.m3u8",
                localFilePath = tempFile.absolutePath,
                status = DownloadStatus.COMPLETED.storageValue
            )

            val localResolved = com.example.data.model.PlaybackSourcePolicy.resolvePlaybackSource(
                media = media,
                downloads = listOf(completedDownload)
            )
            assertTrue(localResolved is com.example.data.model.PlaybackSource.LocalFile)
            assertEquals(tempFile.absolutePath, (localResolved as com.example.data.model.PlaybackSource.LocalFile).filePath)

            val streamResolved = com.example.data.model.PlaybackSourcePolicy.resolvePlaybackSource(
                media = media,
                downloads = emptyList()
            )
            assertTrue(streamResolved is com.example.data.model.PlaybackSource.DirectStream)
            val streamUrl = (streamResolved as com.example.data.model.PlaybackSource.DirectStream).url
            assertEquals("https://cdn.example.com/inception.m3u8", streamUrl)
            assertTrue(com.example.data.model.PlaybackSourcePolicy.isHlsStreamUrl(streamUrl))

            val webMedia = media.copy(directVideoUrl = "", downloadUrl = "")
            val webResolved = com.example.data.model.PlaybackSourcePolicy.resolvePlaybackSource(
                media = webMedia,
                downloads = emptyList()
            )
            assertTrue(webResolved is com.example.data.model.PlaybackSource.WebEmbed)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun testSeasonEligibilityAndDownloadGroupingAndQueueSlots() {
        val tempFile = java.io.File.createTempFile("cinetra_ep1", ".mp4")
        try {
            tempFile.writeText("ep1")
            val epLinksJson = com.example.data.model.EpisodeDirectLinksCodec.parseMultilineInput(
                """
                S1E1 | https://cdn.example.com/s1e1.mp4
                S1E2 | https://cdn.example.com/s1e2.mp4
                S1E3 | https://cdn.example.com/s1e3.mp4
                """.trimIndent()
            )
            val series = MediaItem(
                id = "tv_100",
                tmdbId = 100L,
                title = "Arcane",
                overview = "",
                posterPath = "",
                mediaType = "tv",
                episodeLinksJson = epLinksJson
            )
            val episodes = listOf(
                EpisodeInfo(seasonNumber = 1, episodeNumber = 1, name = "Ep 1"),
                EpisodeInfo(seasonNumber = 1, episodeNumber = 2, name = "Ep 2"),
                EpisodeInfo(seasonNumber = 1, episodeNumber = 3, name = "Ep 3"),
                EpisodeInfo(seasonNumber = 1, episodeNumber = 4, name = "Ep 4 (no link)")
            )
            val completedEp1 = DownloadEntity(
                id = "tv_100_s1e1_direct",
                mediaId = "tv_100",
                tmdbId = 100L,
                title = "Arcane",
                posterPath = "",
                backdropPath = "",
                mediaType = "tv",
                season = 1,
                episode = 1,
                episodeTitle = "Ep 1",
                sourceUrl = "https://cdn.example.com/s1e1.mp4",
                localFilePath = tempFile.absolutePath,
                bytesDownloaded = 500L,
                totalBytes = 500L,
                status = DownloadStatus.COMPLETED.storageValue
            )
            val ep2Running = DownloadEntity(
                id = "tv_100_s1e2_direct",
                mediaId = "tv_100",
                tmdbId = 100L,
                title = "Arcane",
                posterPath = "",
                backdropPath = "",
                mediaType = "tv",
                season = 1,
                episode = 2,
                episodeTitle = "Ep 2",
                sourceUrl = "https://cdn.example.com/s1e2.mp4",
                bytesDownloaded = 250L,
                totalBytes = 500L,
                status = DownloadStatus.RUNNING.storageValue
            )
            val ep3Queued = DownloadEntity(
                id = "tv_100_s1e3_direct",
                mediaId = "tv_100",
                tmdbId = 100L,
                title = "Arcane",
                posterPath = "",
                backdropPath = "",
                mediaType = "tv",
                season = 1,
                episode = 3,
                episodeTitle = "Ep 3",
                sourceUrl = "https://cdn.example.com/s1e3.mp4",
                bytesDownloaded = 0L,
                totalBytes = 500L,
                status = DownloadStatus.QUEUED.storageValue
            )

            val eligibility = com.example.data.model.PlaybackSourcePolicy.computeSeasonEligibility(
                media = series,
                seasonNumber = 1,
                episodeNumbers = episodes.map { it.episodeNumber },
                episodesInSeason = episodes,
                downloads = listOf(completedEp1, ep2Running)
            )
            assertEquals(listOf(1), eligibility.downloadedEpisodes)
            assertEquals(listOf(4), eligibility.noLinkEpisodes)
            assertEquals(listOf(3), eligibility.eligibleEpisodes)

            val grouping = com.example.data.model.PlaybackSourcePolicy.groupDownloadsForDisplay(
                listOf(completedEp1, ep2Running, ep3Queued)
            )
            assertEquals(0, grouping.movieDownloads.size)
            assertEquals(1, grouping.seriesGroups.size)
            val arcaneGroup = grouping.seriesGroups[0]
            assertEquals("Arcane", arcaneGroup.seriesTitle)
            assertEquals(1, arcaneGroup.seasons.size)
            val s1Group = arcaneGroup.seasons[0]
            assertEquals(1, s1Group.completedCount)
            assertEquals(3, s1Group.episodes.size)
            assertEquals(50, s1Group.overallProgressPercent)

            // Queue promotion: 1 running, 2 queued, max 2 -> promote 1 slot
            val toPromoteCount = com.example.data.download.DownloadEngine.computeSlotsToPromote(
                runningCount = 1,
                queuedCount = 2,
                maxConcurrent = 2
            )
            assertEquals(1, toPromoteCount)
            assertEquals(DownloadStatus.RUNNING, DownloadStatus.fromStorage("DOWNLOADING"))
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun testCatalogSyncMappersPreserveDirectLinksAndEpisodeLinks() {
        val entity = CustomMediaEntity(
            id = "custom_series_1",
            title = "Custom Series",
            overview = "Desc",
            posterPath = "https://img/p.jpg",
            backdropPath = "https://img/b.jpg",
            mediaType = "tv",
            releaseDate = "2025",
            voteAverage = 8.5,
            genres = "Sci-Fi",
            certification = "TV-14",
            vidlyId = "12345",
            directVideoUrl = "https://cdn/master.m3u8",
            downloadUrl = "https://cdn/movie.mp4",
            episodeLinksJson = """[{"season":1,"episode":1,"directVideoUrl":"https://cdn/e1.mp4"}]""",
            tmdbId = 12345L,
            numberOfSeasons = 2,
            episodesPerSeason = 8
        )
        val map = CatalogSyncMappers.customMediaToMap(entity)
        val restored = CatalogSyncMappers.customMediaFromMap("custom_series_1", map)
        assertTrue(restored != null)
        assertEquals("https://cdn/master.m3u8", restored!!.directVideoUrl)
        assertEquals("https://cdn/movie.mp4", restored.downloadUrl)
        assertEquals(entity.episodeLinksJson, restored.episodeLinksJson)

        val tmdbBase = MediaItem(
            id = "12345",
            tmdbId = 12345L,
            title = "Base Series",
            overview = "",
            posterPath = "",
            mediaType = "tv"
        )
        val merged = tmdbBase.mergeWithCatalog(restored.toMediaItem())
        assertEquals("https://cdn/master.m3u8", merged.directVideoUrl)
        assertEquals("https://cdn/movie.mp4", merged.downloadUrl)
        assertEquals(entity.episodeLinksJson, merged.episodeLinksJson)
    }

    @Test
    fun testMultiLanguageDownloadParsingAndGroupingRules() {
        // Language extraction and title normalization
        assertEquals("Hindi", MovieBoxApiClient.extractLanguageFromTitle("Inception [Hindi]"))
        assertEquals("Bengali", MovieBoxApiClient.extractLanguageFromTitle("Jawan (Bangla Dub)"))
        assertEquals("Inception", MovieBoxApiClient.stripTitleLanguageAndSeason("Inception [Hindi Dubbed] S1"))
        assertEquals("English", MovieBoxApiClient.normalizeLanguageLabel("eng"))
        assertEquals("Hindi", MovieBoxApiClient.normalizeLanguageLabel("Hindi Dubbed"))

        val baseSubject = MovieBoxSubject(
            subjectId = "sub_en",
            subjectType = 1,
            title = "Inception",
            detailPath = "inception-2010",
            language = "English"
        )

        // Parse dubs from detail JSON
        val dubsJson = """
            {
              "code": 0,
              "data": {
                "subject": {
                  "detailPath": "inception-2010",
                  "dubs": [
                    { "subjectId": "sub_hi", "lanName": "Hindi", "detailPath": "inception-hindi" },
                    { "subjectId": "sub_bn", "lanCode": "bn", "detailPath": "inception-bengali" }
                  ]
                }
              }
            }
        """.trimIndent()
        val (resolvedPath, dubSubjects) = MovieBoxApiClient.parseSubjectDetailDubsJson(dubsJson, baseSubject)
        assertEquals("inception-2010", resolvedPath)
        assertEquals(2, dubSubjects.size)
        assertEquals("Hindi", dubSubjects[0].language)
        assertEquals("Bengali", dubSubjects[1].language)

        // Same-resolution options in different languages must NOT be collapsed by finalizeOptions
        val multiLangDownloadsJson = """
            {
              "code": 0,
              "data": {
                "downloads": [
                  { "id": "en_1080", "resolution": 1080, "size": 2100000000, "url": "https://cdn.example.com/en_1080.mp4", "lanName": "English" },
                  { "id": "hi_1080", "resolution": 1080, "size": 1950000000, "url": "https://cdn.example.com/hi_1080.mp4", "lanName": "Hindi" },
                  { "id": "hi_720", "resolution": 720, "size": 1050000000, "url": "https://cdn.example.com/hi_720.mp4", "lanName": "Hindi" }
                ]
              }
            }
        """.trimIndent()
        val multiBundle = MovieBoxApiClient.parseDownloadResponseJson(multiLangDownloadsJson, baseSubject)
        assertEquals(3, multiBundle.options.size)
        assertEquals(2, multiBundle.languageGroups.size)
        assertEquals(listOf("English", "Hindi"), multiBundle.languageGroups.map { it.language })

        // Single-language bundle must keep languageGroups empty so UI shows just the single available list
        val singleLangDownloadsJson = """
            {
              "code": 0,
              "data": {
                "downloads": [
                  { "id": "en_1080", "resolution": 1080, "size": 2100000000, "url": "https://cdn.example.com/en_1080.mp4", "lanName": "English" },
                  { "id": "en_720", "resolution": 720, "size": 1050000000, "url": "https://cdn.example.com/en_720.mp4", "lanName": "English" }
                ]
              }
            }
        """.trimIndent()
        val singleBundle = MovieBoxApiClient.parseDownloadResponseJson(singleLangDownloadsJson, baseSubject)
        assertEquals(2, singleBundle.options.size)
        assertTrue(singleBundle.languageGroups.isEmpty())

        // Same-title search candidate variant discovery + bundle merging
        val hindiSubject = baseSubject.copy(subjectId = "sub_hi", title = "Inception [Hindi]", language = "Hindi")
        val media = MediaItem(id = "27205", tmdbId = 27205L, title = "Inception", overview = "", posterPath = "", mediaType = "movie")
        val matchingVariants = MovieBoxApiClient.collectSameTitleLanguageVariants(
            media = media,
            primarySubject = baseSubject,
            candidates = listOf(
                baseSubject,
                hindiSubject,
                MovieBoxSubject(subjectId = "other", subjectType = 1, title = "Inception: Making Of [Hindi]", language = "Hindi")
            )
        )
        assertEquals(listOf("sub_hi"), matchingVariants.map { it.subjectId })

        val mergedMulti = MovieBoxApiClient.mergeLanguageBundles(
            primarySubject = baseSubject,
            bundlesWithSubjects = listOf(
                baseSubject to singleBundle,
                hindiSubject to MovieBoxApiClient.parseDownloadResponseJson(
                    """{"data":{"downloads":[{"id":"hi_1080","resolution":1080,"size":1900000000,"url":"https://cdn.example.com/hi.mp4"}]}}""",
                    hindiSubject
                )
            )
        )
        assertEquals(3, mergedMulti.options.size)
        assertEquals(2, mergedMulti.languageGroups.size)

        // Managed provider compatibility for legacy MovieBox rows and new Cloud HD rows
        assertTrue(com.example.data.download.DownloadEngine.isManagedApiProvider("Cloud HD"))
        assertTrue(com.example.data.download.DownloadEngine.isManagedApiProvider("MovieBox API"))
        assertFalse(com.example.data.download.DownloadEngine.isManagedApiProvider("Direct Stream"))
    }
}

