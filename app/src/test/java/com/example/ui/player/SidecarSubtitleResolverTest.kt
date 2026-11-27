package com.example.ui.player

import androidx.media3.common.MimeTypes
import com.example.data.download.DownloadEngine
import com.example.data.model.CatalogSyncMappers
import com.example.data.model.CustomMediaEntity
import com.example.data.model.EpisodeDirectLinksCodec
import com.example.data.model.MediaItem
import com.example.data.model.mergeWithCatalog
import com.example.data.model.toMediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SidecarSubtitleResolverTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun findSidecarSubtitle_returnsSrtWhenMatchingSrtExistsNextToVideo() {
        val dir = tempFolder.newFolder("downloads_srt")
        val videoFile = File(dir, "Dune_2021_1080p.mp4").apply { writeText("video-bytes") }
        val srtFile = File(dir, "Dune_2021_1080p.srt").apply {
            writeText("1\n00:00:01,000 --> 00:00:03,000\nHello\n")
        }

        val foundFromPath = SidecarSubtitleResolver.findSidecarSubtitle(videoFile.absolutePath)
        assertNotNull(foundFromPath)
        assertEquals(srtFile.absolutePath, foundFromPath!!.file.absolutePath)
        assertEquals(MimeTypes.APPLICATION_SUBRIP, foundFromPath.mimeType)

        // Also supports file:// URI input
        val foundFromFileUri = SidecarSubtitleResolver.findSidecarSubtitle(videoFile.toURI().toString())
        assertNotNull(foundFromFileUri)
        assertEquals(srtFile.absolutePath, foundFromFileUri!!.file.absolutePath)
        assertEquals(MimeTypes.APPLICATION_SUBRIP, foundFromFileUri.mimeType)
    }

    @Test
    fun findSidecarSubtitle_returnsVttWhenMatchingVttExistsNextToVideo() {
        val dir = tempFolder.newFolder("downloads_vtt")
        val videoFile = File(dir, "Breaking_Bad_S1E1_Direct_HD.mkv").apply { writeText("video-bytes") }
        val vttFile = File(dir, "Breaking_Bad_S1E1_Direct_HD.vtt").apply {
            writeText("WEBVTT\n\n00:00:01.000 --> 00:00:03.000\nSay my name.\n")
        }

        val found = SidecarSubtitleResolver.findSidecarSubtitle(
            videoPathOrUri = "content://downloads/public_downloads/42",
            fallbackPath = videoFile.absolutePath
        )
        assertNotNull(found)
        assertEquals(vttFile.absolutePath, found!!.file.absolutePath)
        assertEquals(MimeTypes.TEXT_VTT, found.mimeType)
    }

    @Test
    fun findSidecarSubtitle_returnsNullWhenNoMatchingSidecarExistsOrSourceIsRemote() {
        val dir = tempFolder.newFolder("downloads_none")
        val videoFile = File(dir, "Inception_720p.mp4").apply { writeText("video-bytes") }
        // Different base name in same directory must not match
        File(dir, "Interstellar_720p.srt").apply { writeText("1\n00:00:01,000 --> 00:00:02,000\nWrong\n") }

        assertNull(SidecarSubtitleResolver.findSidecarSubtitle(videoFile.absolutePath))
        assertNull(SidecarSubtitleResolver.findSidecarSubtitle("https://cdn.example.com/Inception_720p.mp4"))
        assertNull(SidecarSubtitleResolver.findSidecarSubtitle(""))
    }

    @Test
    fun directDownloadSubtitlePlumbing_preservesSubtitleUrlAndLabelAcrossModels() {
        val epJson = """
            {
              "s1e2": {
                "directVideoUrl": "https://cdn.example.com/s1e2.mp4",
                "downloadUrl": "https://cdn.example.com/s1e2_dl.mp4",
                "subtitleUrl": "https://cdn.example.com/s1e2_es.vtt",
                "subtitleLabel": "Spanish"
              }
            }
        """.trimIndent()
        val decoded = EpisodeDirectLinksCodec.decodeJson(epJson)
        val s1e2 = decoded["s1e2"]
        assertNotNull(s1e2)
        assertEquals("https://cdn.example.com/s1e2_es.vtt", s1e2!!.subtitleUrl)
        assertEquals("Spanish", s1e2.subtitleLabel)

        val customEntity = CustomMediaEntity(
            id = "custom_show_1",
            title = "Show",
            mediaType = "tv",
            tmdbId = 999L,
            directVideoUrl = "https://cdn.example.com/movie.mp4",
            episodeLinksJson = epJson,
            subtitleUrl = "https://cdn.example.com/default_en.srt",
            subtitleLabel = "English"
        )
        val mapped = CatalogSyncMappers.customMediaToMap(customEntity)
        val restored = CatalogSyncMappers.customMediaFromMap("custom_show_1", mapped)
        assertNotNull(restored)
        assertEquals("https://cdn.example.com/default_en.srt", restored!!.subtitleUrl)
        assertEquals("English", restored.subtitleLabel)

        val merged = MediaItem(
            id = "999",
            tmdbId = 999L,
            title = "Show",
            overview = "",
            posterPath = "",
            mediaType = "tv"
        ).mergeWithCatalog(restored.toMediaItem())
        val epLink = merged.resolveDirectLinkForEpisode(season = 1, episode = 2)
        assertEquals("https://cdn.example.com/s1e2_es.vtt", epLink.subtitleUrl)
        assertEquals("Spanish", epLink.subtitleLabel)

        val subOpt = DownloadEngine.buildDirectSubtitleOption(
            mediaId = merged.id,
            season = 1,
            episode = 2,
            subtitleUrl = epLink.subtitleUrl,
            subtitleLabel = epLink.subtitleLabel
        )
        assertNotNull(subOpt)
        assertEquals("https://cdn.example.com/s1e2_es.vtt", subOpt!!.url)
        assertEquals("Spanish", subOpt.languageName)

        assertNull(
            DownloadEngine.buildDirectSubtitleOption(
                mediaId = merged.id,
                season = 1,
                episode = 2,
                subtitleUrl = "",
                subtitleLabel = ""
            )
        )
    }
}
