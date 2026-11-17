package com.leanwell.downloader.engine

import com.leanwell.downloader.model.FormatOption
import com.leanwell.downloader.model.MediaType

data class ExtractedVideoInfo(
    val url: String,
    val title: String,
    val author: String,
    val durationText: String,
    val durationSeconds: Long,
    val thumbnailUrl: String?,
    val audioFormats: List<FormatOption>,
    val videoFormats: List<FormatOption>,
    val customPlatformName: String? = null
)

data class PlaylistItemInfo(
    val id: String,
    val title: String,
    val author: String,
    val url: String,
    val durationText: String,
    val durationSeconds: Long,
    val thumbnailUrl: String?
)

data class ExtractedPlaylistInfo(
    val id: String,
    val title: String,
    val author: String,
    val itemCount: Int,
    val items: List<PlaylistItemInfo>,
    val thumbnailUrl: String?
)

enum class PlaylistQualityPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val mediaType: MediaType,
    val formatOptionId: String
) {
    AUDIO_320K("playlist_audio_320k", "Best Audio (320k)", "MP3 • 320 kbps", MediaType.AUDIO, "mp3_320"),
    AUDIO_192K("playlist_audio_192k", "Standard Audio (192k)", "MP3 • 192 kbps", MediaType.AUDIO, "mp3_192"),
    VIDEO_1080P("playlist_video_1080p", "Full HD (1080p)", "MP4 • 1080p", MediaType.VIDEO, "video_1080"),
    VIDEO_720P("playlist_video_720p", "HD Video (720p)", "MP4 • 720p", MediaType.VIDEO, "video_720"),
    VIDEO_480P("playlist_video_480p", "Standard (480p)", "MP4 • 480p", MediaType.VIDEO, "video_480");

    fun toFormatOption(): FormatOption {
        return FormatOption(
            id = this.formatOptionId,
            label = this.title,
            badge = this.subtitle,
            estimatedSize = when (this) {
                AUDIO_320K -> "~8.5 MB"
                AUDIO_192K -> "~5.0 MB"
                VIDEO_1080P -> "~75 MB"
                VIDEO_720P -> "~45 MB"
                VIDEO_480P -> "~25 MB"
            },
            mediaType = this.mediaType
        )
    }
}
