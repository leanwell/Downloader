package com.leanwell.downloader.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import com.leanwell.downloader.engine.YoutubeEngine
import com.leanwell.downloader.model.MediaItem
import com.leanwell.downloader.model.MediaType
import com.leanwell.downloader.model.Platform
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MediaScannerUtils {

    private const val TAG = "MediaScannerUtils"

    fun loadDownloadedMediaItems(context: Context): List<MediaItem> {
        val downloadDir = YoutubeEngine.getPublicDownloadDir()
        if (!downloadDir.exists() || !downloadDir.isDirectory) {
            return emptyList()
        }

        val validExtensions = setOf("mp3", "m4a", "mp4", "webm", "mkv", "ogg", "opus", "wav")
        val files = downloadDir.walkTopDown()
            .filter { it.isFile && validExtensions.contains(it.extension.lowercase()) }
            .sortedByDescending { it.lastModified() }
            .toList()

        val items = mutableListOf<MediaItem>()
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val retriever = MediaMetadataRetriever()

        for (file in files) {
            try {
                val isAudio = file.name.endsWith(".mp3", ignoreCase = true) || file.name.endsWith(".m4a", ignoreCase = true)
                val ext = file.extension.uppercase()

                var title = file.nameWithoutExtension
                var artist = "YouTube Creator"
                var durationText = "00:00"

                try {
                    retriever.setDataSource(file.absolutePath)
                    val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
                    val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L

                    if (!metaTitle.isNullOrBlank()) {
                        title = metaTitle
                    }
                    if (!metaArtist.isNullOrBlank()) {
                        artist = metaArtist
                    }
                    if (durationMs > 0) {
                        val totalSecs = durationMs / 1000
                        val mins = totalSecs / 60
                        val secs = totalSecs % 60
                        durationText = String.format(Locale.US, "%02d:%02d", mins, secs)
                    }
                } catch (e: Exception) {
                    // Fall back to filename
                }

                val fileSizeMb = file.length() / (1024.0 * 1024.0)
                val fileSizeText = String.format(Locale.US, "%.1f MB", fileSizeMb)

                val cleanTitle = ArtworkHelper.cleanTitle(title)
                val artworkUri = ArtworkHelper.getArtworkUriForFile(context, file, retriever)
                val modTime = if (file.lastModified() > 0) file.lastModified() else System.currentTimeMillis()
                val detectedPlatform = when {
                    cleanTitle.contains("Spotify", ignoreCase = true) || artist.contains("Spotify", ignoreCase = true) -> Platform.SPOTIFY
                    cleanTitle.contains("YouTube Music", ignoreCase = true) || artist.contains("YouTube Music", ignoreCase = true) -> Platform.YOUTUBE_MUSIC
                    cleanTitle.contains("Facebook", ignoreCase = true) || artist.contains("Facebook", ignoreCase = true) -> Platform.FACEBOOK
                    cleanTitle.contains("TikTok", ignoreCase = true) || artist.contains("TikTok", ignoreCase = true) -> Platform.TIKTOK
                    cleanTitle.contains("Instagram", ignoreCase = true) || artist.contains("Instagram", ignoreCase = true) -> Platform.INSTAGRAM
                    cleanTitle.contains("Twitter", ignoreCase = true) || artist.contains("Twitter", ignoreCase = true) -> Platform.TWITTER
                    else -> Platform.YOUTUBE
                }

                val parentFile = file.parentFile
                val playlistName = if (parentFile != null && parentFile.absolutePath != downloadDir.absolutePath) {
                    parentFile.name
                } else null

                items.add(
                    MediaItem(
                        id = file.absolutePath.hashCode().toString(),
                        title = cleanTitle,
                        artistOrChannel = artist,
                        duration = durationText,
                        fileSize = fileSizeText,
                        mediaType = if (isAudio) MediaType.AUDIO else MediaType.VIDEO,
                        formatTag = ext,
                        platform = detectedPlatform,
                        dateAdded = dateFormat.format(Date(modTime)),
                        filePath = file.absolutePath,
                        artworkUri = artworkUri,
                        playlistName = playlistName
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to inspect media file: ${file.name}", e)
            }
        }

        try {
            retriever.release()
        } catch (e: Exception) {
            // Ignored
        }

        return items
    }
}
