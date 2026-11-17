package com.leanwell.downloader.model

import androidx.compose.runtime.Immutable

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    CONVERTING,
    PAUSED,
    COMPLETED,
    ERROR
}

enum class MediaType {
    AUDIO,
    VIDEO
}

enum class Platform(val displayName: String) {
    YOUTUBE("YouTube"),
    YOUTUBE_MUSIC("YouTube Music"),
    SPOTIFY("Spotify"),
    FACEBOOK("Facebook"),
    TIKTOK("TikTok"),
    INSTAGRAM("Instagram"),
    TWITTER("Twitter / X"),
    UNIVERSAL("Web")
}

@Immutable
data class ActiveDownload(
    val id: String,
    val title: String,
    val platform: Platform,
    val mediaType: MediaType,
    val formatLabel: String,
    val qualityLabel: String,
    val progress: Float, // 0.0f to 1.0f
    val speed: String, // e.g. "3.8 MB/s"
    val eta: String, // e.g. "45s left"
    val downloadedBytes: String, // e.g. "24.5 MB"
    val totalBytes: String, // e.g. "36.0 MB"
    val status: DownloadStatus = DownloadStatus.DOWNLOADING,
    val statusMessage: String? = null, // e.g. "Converting & embedding album art..."
    val customPlatformName: String? = null
)

@Immutable
data class MediaItem(
    val id: String,
    val title: String,
    val artistOrChannel: String,
    val duration: String,
    val fileSize: String,
    val mediaType: MediaType,
    val formatTag: String,
    val platform: Platform,
    val dateAdded: String,
    val filePath: String? = null,
    val artworkUri: String? = null,
    val playlistName: String? = null,
    val customPlatformName: String? = null
)

@Immutable
data class FormatOption(
    val id: String,
    val label: String,
    val badge: String? = null,
    val estimatedSize: String,
    val mediaType: MediaType,
    val isRecommended: Boolean = false
)
