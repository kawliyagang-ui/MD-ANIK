package com.example

import com.example.data.model.*
import com.example.data.model.PlayerServer
import com.example.data.repository.applyToMediaItem
import com.example.data.repository.toEpisodeInfoList
import com.example.ui.theme.DarkThemePalette
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
}



