package com.example.ui.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.MimeTypes
import java.io.File
import java.net.URI

data class LocalSidecarSubtitle(
    val file: File,
    val mimeType: String
)

object SidecarSubtitleResolver {

    /**
     * Looks next to a local video file (same directory, same base name without extension)
     * for a sidecar `.srt` or `.vtt` subtitle file.
     */
    fun findSidecarSubtitle(
        videoPathOrUri: String,
        fallbackPath: String = ""
    ): LocalSidecarSubtitle? {
        val candidates = listOf(videoPathOrUri.trim(), fallbackPath.trim())
            .filter { it.isNotBlank() }
            .distinct()
        for (raw in candidates) {
            val videoFile = resolveLocalVideoFile(raw) ?: continue
            val parent = videoFile.parentFile ?: continue
            if (!parent.isDirectory) continue
            val baseName = videoFile.nameWithoutExtension
            if (baseName.isBlank()) continue

            val srtFile = findMatchingFile(parent, baseName, "srt")
            if (srtFile != null) {
                return LocalSidecarSubtitle(
                    file = srtFile,
                    mimeType = MimeTypes.APPLICATION_SUBRIP
                )
            }

            val vttFile = findMatchingFile(parent, baseName, "vtt")
            if (vttFile != null) {
                return LocalSidecarSubtitle(
                    file = vttFile,
                    mimeType = MimeTypes.TEXT_VTT
                )
            }
        }
        return null
    }

    fun resolveLocalVideoFile(pathOrUri: String): File? {
        val trimmed = pathOrUri.trim()
        if (trimmed.isBlank()) return null
        return when {
            trimmed.startsWith("file:", ignoreCase = true) -> {
                runCatching { File(URI(trimmed)) }.getOrNull()
                    ?: File("/" + trimmed.substring(5).trimStart('/'))
            }
            trimmed.contains("://") -> null
            else -> File(trimmed)
        }
    }

    private fun findMatchingFile(parent: File, baseName: String, ext: String): File? {
        val exact = File(parent, "$baseName.$ext")
        if (exact.exists() && exact.isFile) return exact
        val upper = File(parent, "$baseName.${ext.uppercase()}")
        if (upper.exists() && upper.isFile) return upper
        return parent.listFiles()?.firstOrNull { candidate ->
            candidate.isFile &&
                candidate.nameWithoutExtension == baseName &&
                candidate.extension.equals(ext, ignoreCase = true)
        }
    }

    fun buildSubtitleConfiguration(
        sidecar: LocalSidecarSubtitle,
        subtitleLabel: String? = null
    ): Media3Item.SubtitleConfiguration {
        val cleanLabel = subtitleLabel?.trim().orEmpty()
        val language = cleanLabel.ifBlank { "en" }
        val builder = Media3Item.SubtitleConfiguration.Builder(Uri.fromFile(sidecar.file))
            .setMimeType(sidecar.mimeType)
            .setLanguage(language)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
        if (cleanLabel.isNotBlank()) {
            builder.setLabel(cleanLabel)
        }
        return builder.build()
    }
}
