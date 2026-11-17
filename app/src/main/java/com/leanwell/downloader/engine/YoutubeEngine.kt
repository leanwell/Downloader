package com.leanwell.downloader.engine

import android.net.Uri
import android.os.Environment
import android.util.Log
import com.leanwell.downloader.DownloaderApp
import com.leanwell.downloader.model.FormatOption
import com.leanwell.downloader.model.MediaType
import com.leanwell.downloader.security.SecureCookieVault
import com.leanwell.downloader.settings.AppSettings
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

object YoutubeEngine {

    private const val TAG = "YoutubeEngine"

    fun isValidMediaUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            return false
        }
        if (trimmed.startsWith("-")) {
            return false
        }
        return try {
            val parsed = Uri.parse(trimmed)
            val scheme = parsed.scheme?.lowercase()
            (scheme == "http" || scheme == "https") && !parsed.host.isNullOrBlank()
        } catch (e: Exception) {
            false
        }
    }

    fun isYouTubeUrl(url: String): Boolean {
        val lower = url.lowercase().trim()
        return lower.contains("youtube.com") || lower.contains("youtu.be")
    }

    fun isYoutubeMusicUrl(url: String): Boolean {
        val lower = url.lowercase().trim()
        return lower.contains("music.youtube.com")
    }

    fun isSpotifyUrl(url: String): Boolean {
        return SpotifyEngine.isSpotifyUrl(url)
    }

    fun isPlaylistUrl(url: String): Boolean {
        val lower = url.lowercase().trim()
        val isYt = (isYouTubeUrl(lower) || isYoutubeMusicUrl(lower)) && (
            lower.contains("playlist?list=") ||
            lower.contains("&list=") ||
            lower.contains("?list=")
        )
        val isSp = SpotifyEngine.isSpotifyUrl(lower) && SpotifyEngine.isPlaylistOrAlbumUrl(lower)
        return isYt || isSp
    }

    fun isFacebookUrl(url: String): Boolean {
        val lower = url.lowercase().trim()
        return lower.contains("facebook.com") ||
                lower.contains("fb.watch") ||
                lower.contains("fb.gg") ||
                lower.contains("fb.me") ||
                lower.contains("m.facebook.com")
    }

    fun isTikTokUrl(url: String): Boolean {
        val lower = url.lowercase().trim()
        return lower.contains("tiktok.com")
    }

    fun isInstagramUrl(url: String): Boolean {
        val lower = url.lowercase().trim()
        return lower.contains("instagram.com") || lower.contains("instagr.am")
    }

    fun isTwitterUrl(url: String): Boolean {
        val lower = url.lowercase().trim()
        return lower.contains("twitter.com") || lower.contains("x.com") || lower.contains("t.co")
    }

    fun detectPlatform(url: String): com.leanwell.downloader.model.Platform {
        val lower = url.lowercase().trim()
        return when {
            isSpotifyUrl(lower) -> com.leanwell.downloader.model.Platform.SPOTIFY
            isYoutubeMusicUrl(lower) -> com.leanwell.downloader.model.Platform.YOUTUBE_MUSIC
            isYouTubeUrl(lower) -> com.leanwell.downloader.model.Platform.YOUTUBE
            isFacebookUrl(lower) -> com.leanwell.downloader.model.Platform.FACEBOOK
            isTikTokUrl(lower) -> com.leanwell.downloader.model.Platform.TIKTOK
            isInstagramUrl(lower) -> com.leanwell.downloader.model.Platform.INSTAGRAM
            isTwitterUrl(lower) -> com.leanwell.downloader.model.Platform.TWITTER
            else -> com.leanwell.downloader.model.Platform.UNIVERSAL
        }
    }

    fun normalizeMediaUrl(url: String): String {
        val trimmed = url.trim()
        val platform = detectPlatform(trimmed)

        return try {
            val uri = Uri.parse(trimmed)
            val host = uri.host?.lowercase() ?: return trimmed
            val path = uri.path ?: ""

            when (platform) {
                com.leanwell.downloader.model.Platform.YOUTUBE_MUSIC -> {
                    if (path.contains("watch") && uri.getQueryParameter("v") != null) {
                        val videoId = uri.getQueryParameter("v")
                        "https://www.youtube.com/watch?v=$videoId"
                    } else if (path.contains("playlist") && uri.getQueryParameter("list") != null) {
                        val listId = uri.getQueryParameter("list")
                        "https://www.youtube.com/playlist?list=$listId"
                    } else {
                        trimmed.replace("music.youtube.com", "www.youtube.com")
                    }
                }
                com.leanwell.downloader.model.Platform.FACEBOOK -> {
                    if (path.contains("watch") && uri.getQueryParameter("v") != null) {
                        val videoId = uri.getQueryParameter("v")
                        "https://www.facebook.com/watch/?v=$videoId"
                    } else if (path.contains("/reel/") || path.contains("/videos/")) {
                        "${uri.scheme ?: "https"}://$host$path"
                    } else {
                        trimmed
                    }
                }
                com.leanwell.downloader.model.Platform.INSTAGRAM -> {
                    // Strip tracking query parameters like ?igsh=... or ?utm_source=...
                    if (path.contains("/reel/") || path.contains("/p/") || path.contains("/tv/")) {
                        "${uri.scheme ?: "https"}://$host$path"
                    } else {
                        trimmed
                    }
                }
                com.leanwell.downloader.model.Platform.TWITTER -> {
                    // Strip tracking query parameters like ?s=20 or ?t=...
                    if (path.contains("/status/")) {
                        "${uri.scheme ?: "https"}://$host$path"
                    } else {
                        trimmed
                    }
                }
                else -> trimmed
            }
        } catch (e: Exception) {
            trimmed
        }
    }

    fun sanitizeFilename(raw: String): String {
        return raw.replace("..", "")
            .replace(Regex("[/\\\\:*?\"<>|]"), "_")
            .trim()
            .take(150)
    }

    suspend fun extractMetadata(url: String, cookiesFile: File? = null): Result<ExtractedVideoInfo> = withContext(Dispatchers.IO) {
        val trimmedUrl = url.trim()
        if (!isValidMediaUrl(trimmedUrl)) {
            return@withContext Result.failure(
                IllegalArgumentException("Invalid URL. Please enter a valid HTTP or HTTPS media link.")
            )
        }

        val platform = detectPlatform(trimmedUrl)
        val normalizedUrl = normalizeMediaUrl(trimmedUrl)

        try {
            Log.d(TAG, "Extracting metadata for URL: $normalizedUrl (platform=$platform)")

            // 0. SPOTIFY HANDLER: Fetch rich metadata via SpotifyEngine
            if (platform == com.leanwell.downloader.model.Platform.SPOTIFY) {
                if (SpotifyEngine.isTrackUrl(trimmedUrl)) {
                    val track = SpotifyEngine.resolveTrack(trimmedUrl)
                    if (track != null) {
                        val durationSeconds = (track.durationMs / 1000).coerceAtLeast(0L)
                        val audioFormats = buildAudioFormats(durationSeconds)
                        return@withContext Result.success(
                            ExtractedVideoInfo(
                                url = track.searchQuery,
                                title = track.title,
                                author = track.artist,
                                durationText = track.durationText,
                                durationSeconds = durationSeconds,
                                thumbnailUrl = track.coverUrl,
                                audioFormats = audioFormats,
                                videoFormats = emptyList(),
                                customPlatformName = "Spotify"
                            )
                        )
                    }
                }
            }

            // 1. FAST PATH: Attempt ultra-fast oEmbed metadata extraction (< 250ms) for YouTube & YouTube Music
            if (platform == com.leanwell.downloader.model.Platform.YOUTUBE || platform == com.leanwell.downloader.model.Platform.YOUTUBE_MUSIC) {
                val fastInfo = fetchFastMetadata(normalizedUrl)
                if (fastInfo != null) {
                    Log.d(TAG, "Fast metadata extraction succeeded (< 250ms) for: ${fastInfo.title}")
                    return@withContext Result.success(fastInfo)
                }
            }

            // 2. FALLBACK PATH: yt-dlp if fast path was unavailable or for non-YouTube
            if (!DownloaderApp.isEngineReady.value) {
                DownloaderApp.isEngineReady.first { it }
            }

            val request = YoutubeDLRequest(normalizedUrl).apply {
                addOption("--skip-download")
                addOption("--no-playlist")
            }

            val cookieType = AppSettings.getCookieTypeForUrl(normalizedUrl)
            val videoInfo = if (cookieType != null && SecureCookieVault.hasValidCookies(DownloaderApp.instance, cookieType)) {
                SecureCookieVault.withEphemeralCookieFile(DownloaderApp.instance, cookieType) { ephemFile ->
                    if (ephemFile != null && ephemFile.exists()) {
                        request.addOption("--cookies", ephemFile.absolutePath)
                    }
                    YoutubeDL.getInstance().getInfo(request)
                }
            } else if (cookiesFile != null && cookiesFile.exists() && cookiesFile.length() > 0) {
                request.addOption("--cookies", cookiesFile.absolutePath)
                YoutubeDL.getInstance().getInfo(request)
            } else {
                YoutubeDL.getInstance().getInfo(request)
            }
            val extractor = videoInfo.extractorKey ?: videoInfo.extractor ?: Uri.parse(normalizedUrl).host?.replace("www.", "")?.replaceFirstChar { it.uppercase() } ?: "Web"

            val defaultTitle = when (platform) {
                com.leanwell.downloader.model.Platform.FACEBOOK -> "Facebook Video"
                com.leanwell.downloader.model.Platform.TIKTOK -> "TikTok Video"
                com.leanwell.downloader.model.Platform.INSTAGRAM -> "Instagram Reel"
                com.leanwell.downloader.model.Platform.TWITTER -> "Twitter / X Video"
                com.leanwell.downloader.model.Platform.YOUTUBE,
                com.leanwell.downloader.model.Platform.YOUTUBE_MUSIC -> "YouTube Video"
                com.leanwell.downloader.model.Platform.SPOTIFY -> "Spotify Track"
                com.leanwell.downloader.model.Platform.UNIVERSAL -> "$extractor Video"
            }
            val defaultAuthor = when (platform) {
                com.leanwell.downloader.model.Platform.FACEBOOK -> "Facebook Creator"
                com.leanwell.downloader.model.Platform.TIKTOK -> "TikTok Creator"
                com.leanwell.downloader.model.Platform.INSTAGRAM -> "Instagram Creator"
                com.leanwell.downloader.model.Platform.TWITTER -> "Twitter / X Author"
                com.leanwell.downloader.model.Platform.YOUTUBE,
                com.leanwell.downloader.model.Platform.YOUTUBE_MUSIC -> "YouTube Creator"
                com.leanwell.downloader.model.Platform.SPOTIFY -> "Spotify Artist"
                com.leanwell.downloader.model.Platform.UNIVERSAL -> extractor
            }
            val rawTitle = videoInfo.title
            val rawUploader = videoInfo.uploader
            val title: String = if (!rawTitle.isNullOrBlank()) rawTitle else defaultTitle
            val author: String = if (!rawUploader.isNullOrBlank()) rawUploader else defaultAuthor
            val durationSeconds = videoInfo.duration.toLong().coerceAtLeast(0L)
            val durationText = formatDuration(durationSeconds)
            val thumbnail = videoInfo.thumbnail

            val availableHeights = videoInfo.formats
                ?.mapNotNull { it.height }
                ?.filter { it > 0 }
                ?.toSet() ?: emptySet()

            val maxHeight = maxOf(videoInfo.height, availableHeights.maxOrNull() ?: 0)
            val audioFormats = buildAudioFormats(durationSeconds)
            val videoFormats = when (platform) {
                com.leanwell.downloader.model.Platform.FACEBOOK -> buildFacebookVideoFormats(durationSeconds, maxHeight, availableHeights)
                com.leanwell.downloader.model.Platform.TIKTOK -> buildTikTokVideoFormats(durationSeconds, maxHeight)
                com.leanwell.downloader.model.Platform.INSTAGRAM -> buildInstagramVideoFormats(durationSeconds, maxHeight)
                com.leanwell.downloader.model.Platform.TWITTER -> buildTwitterVideoFormats(durationSeconds, maxHeight, availableHeights)
                com.leanwell.downloader.model.Platform.YOUTUBE,
                com.leanwell.downloader.model.Platform.YOUTUBE_MUSIC,
                com.leanwell.downloader.model.Platform.UNIVERSAL -> buildVideoFormats(durationSeconds, maxHeight = maxHeight, availableHeights = availableHeights)
                com.leanwell.downloader.model.Platform.SPOTIFY -> emptyList()
            }

            Result.success(
                ExtractedVideoInfo(
                    url = normalizedUrl,
                    title = title,
                    author = author,
                    durationText = durationText,
                    durationSeconds = durationSeconds,
                    thumbnailUrl = thumbnail,
                    audioFormats = audioFormats,
                    videoFormats = videoFormats,
                    customPlatformName = if (platform == com.leanwell.downloader.model.Platform.UNIVERSAL) extractor else null
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract metadata for URL: $url", e)
            val msg = e.message ?: ""
            val friendlyError = when {
                platform == com.leanwell.downloader.model.Platform.FACEBOOK && (msg.contains("login", ignoreCase = true) || msg.contains("private", ignoreCase = true) || msg.contains("restricted", ignoreCase = true)) -> {
                    Exception("This Facebook video is private or restricted. Please import Facebook cookies in Settings to access it.")
                }
                platform == com.leanwell.downloader.model.Platform.INSTAGRAM && (msg.contains("login", ignoreCase = true) || msg.contains("private", ignoreCase = true) || msg.contains("restricted", ignoreCase = true) || msg.contains("checkpoint", ignoreCase = true)) -> {
                    Exception("This Instagram post or reel is private. Please import Instagram cookies in Settings to access it.")
                }
                platform == com.leanwell.downloader.model.Platform.TIKTOK && (msg.contains("private", ignoreCase = true) || msg.contains("login", ignoreCase = true)) -> {
                    Exception("This TikTok video is private or deleted.")
                }
                platform == com.leanwell.downloader.model.Platform.TWITTER && (msg.contains("suspended", ignoreCase = true) || msg.contains("private", ignoreCase = true)) -> {
                    Exception("This tweet or video is protected or no longer available.")
                }
                else -> e
            }
            Result.failure(friendlyError)
        }
    }

    suspend fun extractPlaylistMetadata(
        url: String,
        cookiesFile: File? = null
    ): Result<ExtractedPlaylistInfo> = withContext(Dispatchers.IO) {
        val trimmedUrl = url.trim()
        if (!isValidMediaUrl(trimmedUrl) || !isPlaylistUrl(trimmedUrl)) {
            return@withContext Result.failure(
                IllegalArgumentException("Invalid playlist link.")
            )
        }

        // Spotify Playlist / Album resolution
        if (SpotifyEngine.isSpotifyUrl(trimmedUrl)) {
            val collection = SpotifyEngine.resolveCollection(trimmedUrl)
            if (collection != null) {
                val items = collection.tracks.map { track ->
                    PlaylistItemInfo(
                        id = track.id,
                        title = track.title,
                        author = track.artist,
                        url = track.searchQuery,
                        durationText = track.durationText,
                        durationSeconds = (track.durationMs / 1000).coerceAtLeast(0L),
                        thumbnailUrl = track.coverUrl
                    )
                }
                return@withContext Result.success(
                    ExtractedPlaylistInfo(
                        id = collection.id,
                        title = collection.title,
                        author = collection.creatorOrArtist,
                        itemCount = items.size,
                        items = items,
                        thumbnailUrl = collection.coverUrl
                    )
                )
            } else {
                return@withContext Result.failure(Exception("Could not retrieve Spotify playlist or album details."))
            }
        }

        try {
            if (!DownloaderApp.isEngineReady.value) {
                DownloaderApp.isEngineReady.first { it }
            }

            Log.d(TAG, "Extracting playlist metadata for: $trimmedUrl")

            val request = YoutubeDLRequest(trimmedUrl).apply {
                addOption("--skip-download")
                addOption("--flat-playlist")
                addOption("--dump-single-json")
            }

            val cookieType = AppSettings.getCookieTypeForUrl(trimmedUrl)
            val response = if (cookieType != null && SecureCookieVault.hasValidCookies(DownloaderApp.instance, cookieType)) {
                SecureCookieVault.withEphemeralCookieFile(DownloaderApp.instance, cookieType) { ephemFile ->
                    if (ephemFile != null && ephemFile.exists()) {
                        request.addOption("--cookies", ephemFile.absolutePath)
                    }
                    YoutubeDL.getInstance().execute(request)
                }
            } else if (cookiesFile != null && cookiesFile.exists() && cookiesFile.length() > 0) {
                request.addOption("--cookies", cookiesFile.absolutePath)
                YoutubeDL.getInstance().execute(request)
            } else {
                request.addOption("--extractor-args", "youtube:player_client=visionos,web")
                YoutubeDL.getInstance().execute(request)
            }
            val jsonString = response.out
            if (jsonString.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Could not retrieve playlist details."))
            }

            val json = JSONObject(jsonString)
            val playlistId = json.optString("id", "")
            val playlistTitle = json.optString("title", "YouTube Playlist")
            val playlistAuthor = json.optString("uploader", json.optString("channel", "YouTube Creator"))
            val entriesArray = json.optJSONArray("entries")

            val items = mutableListOf<PlaylistItemInfo>()
            if (entriesArray != null) {
                for (i in 0 until entriesArray.length()) {
                    val entry = entriesArray.optJSONObject(i) ?: continue
                    val id = entry.optString("id", "")
                    if (id.isBlank()) continue

                    val title = entry.optString("title", "Video #${i + 1}")
                    val author = entry.optString("uploader", entry.optString("channel", playlistAuthor))
                    val durationSec = entry.optLong("duration", 0L)
                    val durationText = formatDuration(durationSec)
                    val rawUrl = entry.optString("url", "")
                    val itemUrl = if (rawUrl.startsWith("http")) rawUrl else "https://www.youtube.com/watch?v=$id"

                    var thumbUrl: String? = null
                    val thumbs = entry.optJSONArray("thumbnails")
                    if (thumbs != null && thumbs.length() > 0) {
                        thumbUrl = thumbs.optJSONObject(thumbs.length() - 1)?.optString("url")
                    }
                    if (thumbUrl.isNullOrBlank()) {
                        thumbUrl = entry.optString("thumbnail", "")
                    }
                    if (thumbUrl.isNullOrBlank()) {
                        thumbUrl = "https://i.ytimg.com/vi/$id/hqdefault.jpg"
                    }

                    items.add(
                        PlaylistItemInfo(
                            id = id,
                            title = title,
                            author = author,
                            url = itemUrl,
                            durationText = durationText,
                            durationSeconds = durationSec,
                            thumbnailUrl = thumbUrl
                        )
                    )
                }
            }

            val playlistThumb = items.firstOrNull()?.thumbnailUrl
            Result.success(
                ExtractedPlaylistInfo(
                    id = playlistId,
                    title = playlistTitle,
                    author = playlistAuthor,
                    itemCount = items.size,
                    items = items,
                    thumbnailUrl = playlistThumb
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract playlist metadata: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * On-demand background scan to check for 2K (1440p), 4K (2160p), or 8K (4320p) video streams.
     * Invoked only when user manually taps "Check for 2K / 4K / 8K resolutions" in the format sheet.
     */
    suspend fun scanHighResolutions(
        url: String,
        durationSeconds: Long,
        cookiesFile: File? = null
    ): Result<List<FormatOption>> = withContext(Dispatchers.IO) {
        try {
            val trimmedUrl = url.trim()
            if (!isValidMediaUrl(trimmedUrl)) {
                return@withContext Result.failure(
                    IllegalArgumentException("Invalid URL. Please enter a valid HTTP or HTTPS media link.")
                )
            }

            Log.d(TAG, "Scanning for high resolutions on demand for: $trimmedUrl")
            if (!DownloaderApp.isEngineReady.value) {
                DownloaderApp.isEngineReady.first { it }
            }

            val request = YoutubeDLRequest(trimmedUrl).apply {
                addOption("--skip-download")
                addOption("--no-playlist")
            }

            val cookieType = AppSettings.getCookieTypeForUrl(trimmedUrl)
            val videoInfo = if (cookieType != null && SecureCookieVault.hasValidCookies(DownloaderApp.instance, cookieType)) {
                SecureCookieVault.withEphemeralCookieFile(DownloaderApp.instance, cookieType) { ephemFile ->
                    if (ephemFile != null && ephemFile.exists()) {
                        request.addOption("--cookies", ephemFile.absolutePath)
                    }
                    YoutubeDL.getInstance().getInfo(request)
                }
            } else if (cookiesFile != null && cookiesFile.exists() && cookiesFile.length() > 0) {
                request.addOption("--cookies", cookiesFile.absolutePath)
                YoutubeDL.getInstance().getInfo(request)
            } else {
                YoutubeDL.getInstance().getInfo(request)
            }
            val availableHeights = videoInfo.formats
                ?.mapNotNull { it.height }
                ?.filter { it > 0 }
                ?.toSet() ?: emptySet()

            val maxHeight = maxOf(videoInfo.height, availableHeights.maxOrNull() ?: 0)
            Log.d(TAG, "High-res scan complete. maxHeight=$maxHeight, availableHeights=$availableHeights")

            val updatedFormats = buildVideoFormats(
                durationSeconds = durationSeconds,
                maxHeight = maxHeight,
                availableHeights = availableHeights
            )
            Result.success(updatedFormats)
        } catch (e: Exception) {
            Log.e(TAG, "Failed on-demand high-res scan", e)
            Result.failure(e)
        }
    }

    private fun fetchFastMetadata(url: String): ExtractedVideoInfo? {
        try {
            val isYouTube = url.contains("youtube.com", ignoreCase = true) || url.contains("youtu.be", ignoreCase = true)
            if (!isYouTube) return null

            val encoded = URLEncoder.encode(url, "UTF-8")
            val oembedUrl = "https://www.youtube.com/oembed?url=$encoded&format=json"
            val conn = (URL(oembedUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile)")
            }

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)
                val title = json.optString("title", "YouTube Media")
                val author = json.optString("author_name", "YouTube Creator")
                var thumbnail = if (json.has("thumbnail_url")) json.optString("thumbnail_url") else null

                val videoId = extractYouTubeId(url)
                if (videoId != null) {
                    thumbnail = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                }

                val durationSeconds = 210L // Fast sensible estimate (~3:30) for instant preview
                val durationText = formatDuration(durationSeconds)
                val audioFormats = buildAudioFormats(durationSeconds)
                val videoFormats = buildVideoFormats(durationSeconds)

                return ExtractedVideoInfo(
                    url = url,
                    title = title,
                    author = author,
                    durationText = durationText,
                    durationSeconds = durationSeconds,
                    thumbnailUrl = thumbnail,
                    audioFormats = audioFormats,
                    videoFormats = videoFormats
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "Fast metadata fetch skipped: ${e.message}")
        }
        return null
    }

    private fun extractYouTubeId(url: String): String? {
        val pattern = Regex("(?:youtu\\.be/|youtube\\.com/(?:embed/|v/|shorts/|watch\\?v=|watch\\?.+&v=))([\\w-]{11})")
        return pattern.find(url)?.groupValues?.get(1)
    }

    private fun buildAudioFormats(durationSeconds: Long): List<FormatOption> {
        return listOf(
            FormatOption(
                id = "mp3_320",
                label = "MP3 High Quality (320 kbps)",
                badge = null,
                estimatedSize = estimateAudioSize(durationSeconds, 320),
                mediaType = MediaType.AUDIO,
                isRecommended = true
            ),
            FormatOption(
                id = "mp3_192",
                label = "MP3 Standard (192 kbps)",
                badge = null,
                estimatedSize = estimateAudioSize(durationSeconds, 192),
                mediaType = MediaType.AUDIO
            ),
            FormatOption(
                id = "m4a_aac",
                label = "M4A Original (AAC)",
                badge = null,
                estimatedSize = estimateAudioSize(durationSeconds, 128),
                mediaType = MediaType.AUDIO
            )
        )
    }

    private data class VideoResolutionDef(
        val height: Int,
        val id: String,
        val label: String
    )

    private val VIDEO_DEFINITIONS = listOf(
        VideoResolutionDef(4320, "video_4320", "8K Ultra HD (4320p)"),
        VideoResolutionDef(2160, "video_2160", "4K Ultra HD (2160p)"),
        VideoResolutionDef(1440, "video_1440", "1440p Quad HD (2K)"),
        VideoResolutionDef(1080, "video_1080", "1080p Full HD"),
        VideoResolutionDef(720, "video_720", "720p HD"),
        VideoResolutionDef(480, "video_480", "480p SD"),
        VideoResolutionDef(360, "video_360", "360p Standard"),
        VideoResolutionDef(240, "video_240", "240p Compact"),
        VideoResolutionDef(144, "video_144", "144p Data Saver")
    )

    private fun buildVideoFormats(
        durationSeconds: Long,
        maxHeight: Int = 1080,
        availableHeights: Set<Int> = emptySet()
    ): List<FormatOption> {
        val effectiveMax = if (maxHeight > 0) maxHeight else 1080

        val filtered = VIDEO_DEFINITIONS.filter { def ->
            if (def.height > effectiveMax) {
                false // Never show resolutions higher than video's max resolution
            } else if (availableHeights.isNotEmpty() && def.height > 1080) {
                // For 1440p, 4K, 8K: only show if confirmed in available formats
                effectiveMax >= def.height && (availableHeights.contains(def.height) || availableHeights.any { it >= def.height })
            } else {
                true
            }
        }

        val recommendedDef = filtered.firstOrNull { it.height == 1080 }
            ?: filtered.firstOrNull { it.height <= 1080 }
            ?: filtered.firstOrNull()

        return filtered.map { def ->
            FormatOption(
                id = def.id,
                label = def.label,
                badge = null,
                estimatedSize = estimateVideoSize(durationSeconds, def.height),
                mediaType = MediaType.VIDEO,
                isRecommended = (def == recommendedDef)
            )
        }
    }

    private fun buildFacebookVideoFormats(
        durationSeconds: Long,
        maxHeight: Int = 1080,
        availableHeights: Set<Int> = emptySet()
    ): List<FormatOption> {
        val effectiveMax = if (maxHeight > 0) maxHeight else 720
        val options = mutableListOf<FormatOption>()

        if (effectiveMax >= 720) {
            val hdLabel = if (effectiveMax >= 1080) "HD Video (1080p)" else "HD Video (720p)"
            options.add(
                FormatOption(
                    id = "fb_hd",
                    label = hdLabel,
                    badge = null,
                    estimatedSize = estimateVideoSize(durationSeconds, effectiveMax),
                    mediaType = MediaType.VIDEO,
                    isRecommended = true
                )
            )
            options.add(
                FormatOption(
                    id = "fb_sd",
                    label = "SD Video (Standard Quality)",
                    badge = null,
                    estimatedSize = estimateVideoSize(durationSeconds, 480),
                    mediaType = MediaType.VIDEO,
                    isRecommended = false
                )
            )
        } else {
            options.add(
                FormatOption(
                    id = "fb_sd",
                    label = "SD Video (Standard Quality)",
                    badge = null,
                    estimatedSize = estimateVideoSize(durationSeconds, effectiveMax),
                    mediaType = MediaType.VIDEO,
                    isRecommended = true
                )
            )
        }
        return options
    }

    private fun buildTikTokVideoFormats(durationSeconds: Long, maxHeight: Int): List<FormatOption> {
        val effectiveMax = if (maxHeight > 0) maxHeight else 1080
        return listOf(
            FormatOption(
                id = "tiktok_hd",
                label = "HD Video (Watermark-Free)",
                badge = "Original",
                estimatedSize = estimateVideoSize(durationSeconds, effectiveMax),
                mediaType = MediaType.VIDEO,
                isRecommended = true
            )
        )
    }

    private fun buildInstagramVideoFormats(durationSeconds: Long, maxHeight: Int): List<FormatOption> {
        val effectiveMax = if (maxHeight > 0) maxHeight else 1080
        return listOf(
            FormatOption(
                id = "ig_best",
                label = "Best Video (MP4)",
                badge = "Original",
                estimatedSize = estimateVideoSize(durationSeconds, effectiveMax),
                mediaType = MediaType.VIDEO,
                isRecommended = true
            )
        )
    }

    private fun buildTwitterVideoFormats(
        durationSeconds: Long,
        maxHeight: Int,
        availableHeights: Set<Int>
    ): List<FormatOption> {
        val effectiveMax = if (maxHeight > 0) maxHeight else 720
        val options = mutableListOf<FormatOption>()
        options.add(
            FormatOption(
                id = "tw_best",
                label = "Best Quality (${if (effectiveMax >= 1080) "1080p HD" else if (effectiveMax >= 720) "720p HD" else "MP4"})",
                badge = "Best",
                estimatedSize = estimateVideoSize(durationSeconds, effectiveMax),
                mediaType = MediaType.VIDEO,
                isRecommended = true
            )
        )
        if (availableHeights.any { it in 1..480 } || effectiveMax > 480) {
            options.add(
                FormatOption(
                    id = "tw_sd",
                    label = "Standard Quality (SD)",
                    badge = null,
                    estimatedSize = estimateVideoSize(durationSeconds, 480),
                    mediaType = MediaType.VIDEO,
                    isRecommended = false
                )
            )
        }
        return options
    }

    /**
     * Finds a unique filename in [outputDir] by checking both disk files and [reservedTitles]
     * (currently queued or downloading items), appending incrementing "(1)", "(2)", etc. if duplicate exists.
     */
    fun getUniqueTitle(
        outputDir: File,
        rawTitle: String,
        targetExt: String,
        reservedTitles: Set<String> = emptySet()
    ): String {
        val sanitized = rawTitle.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        val baseCandidate = sanitized
        val baseFile = File(outputDir, "$baseCandidate.$targetExt")
        if (!baseFile.exists() && !reservedTitles.contains(baseCandidate)) {
            return baseCandidate
        }
        var count = 1
        while (File(outputDir, "$sanitized ($count).$targetExt").exists() || reservedTitles.contains("$sanitized ($count)")) {
            count++
        }
        return "$sanitized ($count)"
    }

    fun buildDownloadRequest(
        url: String,
        format: FormatOption,
        outputDir: File,
        title: String? = null,
        useAria2c: Boolean = false,
        embedMetadata: Boolean = true,
        cookiesFile: File? = null,
        tempDir: File? = null
    ): YoutubeDLRequest {
        val trimmedUrl = url.trim()
        val request = YoutubeDLRequest(trimmedUrl)

        // Ensure target directory exists
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        // Set output directory and isolate temporary and thumbnail writes to app cache
        // to prevent Android Scoped Storage "[Errno 1] Operation not permitted" on /Music
        val workingTempDir = tempDir ?: File(DownloaderApp.instance.cacheDir, "yt_dlp_tmp").apply { mkdirs() }
        request.addOption("-P", "home:${outputDir.absolutePath}")
        request.addOption("-P", "temp:${workingTempDir.absolutePath}")
        request.addOption("-P", "thumbnail:${workingTempDir.absolutePath}")

        val targetExt = when (format.id) {
            "mp3_320", "mp3_192" -> "mp3"
            "m4a_aac" -> "m4a"
            else -> if (format.mediaType == MediaType.AUDIO) "mp3" else "mp4"
        }

        val safeTitle = if (!title.isNullOrBlank()) sanitizeFilename(title) else null
        val outputTemplate = if (!safeTitle.isNullOrBlank()) {
            "$safeTitle.%(ext)s"
        } else {
            "%(title)s.%(ext)s"
        }

        val isSearch = trimmedUrl.startsWith("ytsearch")
        val isYt = isYouTubeUrl(trimmedUrl) || isSearch

        request.addOption("-o", outputTemplate)
        request.addOption("--no-mtime")
        if (!isSearch) {
            request.addOption("--no-playlist")
        }
        request.addOption("--newline")
        request.addOption("--force-overwrites")

        if (cookiesFile != null && cookiesFile.exists() && cookiesFile.length() > 0) {
            request.addOption("--cookies", cookiesFile.absolutePath)
        } else if (isYt) {
            // When unauthenticated, visionos/web helps bypass generic client-side gating for YouTube only
            request.addOption("--extractor-args", "youtube:player_client=visionos,web")
        }

        if (useAria2c) {
            request.addOption("--external-downloader", "aria2c")
            request.addOption("--external-downloader-args", "aria2c:-j 8 -x 8 -s 8 -k 1M")
        }

        if (embedMetadata) {
            request.addOption("--embed-metadata")
            request.addOption("--embed-thumbnail")
        }

        when (format.id) {
            "tiktok_hd", "ig_best", "tw_best" -> {
                request.addOption("-S", "res:1080,ext:mp4:m4a")
                request.addOption("-f", "bestvideo+bestaudio/best")
                request.addOption("--merge-output-format", "mp4")
            }
            "tw_sd" -> {
                request.addOption("-S", "res:480,ext:mp4:m4a")
                request.addOption("-f", "bestvideo[height<=480]+bestaudio/best[height<=480]/best")
                request.addOption("--merge-output-format", "mp4")
            }
            "fb_hd" -> {
                request.addOption("-S", "res:1080,ext:mp4:m4a")
                request.addOption("-f", "bestvideo+bestaudio/best")
                request.addOption("--merge-output-format", "mp4")
            }
            "fb_sd" -> {
                request.addOption("-S", "res:480,ext:mp4:m4a")
                request.addOption("-f", "worstvideo[height>=360]+worstaudio/worst/best")
                request.addOption("--merge-output-format", "mp4")
            }
            "mp3_320" -> {
                request.addOption("-f", "ba/b")
                request.addOption("-x")
                request.addOption("--audio-format", "mp3")
                request.addOption("--audio-quality", "320k")
            }
            "mp3_192" -> {
                request.addOption("-f", "ba/b")
                request.addOption("-x")
                request.addOption("--audio-format", "mp3")
                request.addOption("--audio-quality", "192k")
            }
            "m4a_aac" -> {
                request.addOption("-f", "ba[ext=m4a]/ba/b")
                request.addOption("-x")
                request.addOption("--audio-format", "m4a")
            }
            "video_4320" -> {
                request.addOption("-S", "res:4320,ext:mp4:m4a")
                request.addOption("-f", "bv*[height<=4320]+ba/b[height<=4320]/bv*+ba/b")
                request.addOption("--merge-output-format", "mp4")
            }
            "video_2160" -> {
                request.addOption("-S", "res:2160,ext:mp4:m4a")
                request.addOption("-f", "bv*[height<=2160]+ba/b[height<=2160]/bv*+ba/b")
                request.addOption("--merge-output-format", "mp4")
            }
            "video_1440" -> {
                request.addOption("-S", "res:1440,ext:mp4:m4a")
                request.addOption("-f", "bv*[height<=1440]+ba/b[height<=1440]/bv*+ba/b")
                request.addOption("--merge-output-format", "mp4")
            }
            "video_1080" -> {
                request.addOption("-S", "res:1080,ext:mp4:m4a")
                request.addOption("-f", "bv*[height<=1080]+ba/b[height<=1080]/bv*+ba/b")
                request.addOption("--merge-output-format", "mp4")
            }
            "video_720" -> {
                request.addOption("-S", "res:720,ext:mp4:m4a")
                request.addOption("-f", "bv*[height<=720]+ba/b[height<=720]/bv*+ba/b")
                request.addOption("--merge-output-format", "mp4")
            }
            "video_480" -> {
                request.addOption("-S", "res:480,ext:mp4:m4a")
                request.addOption("-f", "bv*[height<=480]+ba/b[height<=480]/bv*+ba/b")
                request.addOption("--merge-output-format", "mp4")
            }
            "video_360" -> {
                request.addOption("-S", "res:360,ext:mp4:m4a")
                request.addOption("-f", "bv*[height<=360]+ba/b[height<=360]/bv*+ba/b")
                request.addOption("--merge-output-format", "mp4")
            }
            "video_240" -> {
                request.addOption("-S", "res:240,ext:mp4:m4a")
                request.addOption("-f", "bv*[height<=240]+ba/b[height<=240]/bv*+ba/b")
                request.addOption("--merge-output-format", "mp4")
            }
            "video_144" -> {
                request.addOption("-S", "res:144,ext:mp4:m4a")
                request.addOption("-f", "bv*[height<=144]+ba/b[height<=144]/bv*+ba/b")
                request.addOption("--merge-output-format", "mp4")
            }
            else -> {
                if (format.mediaType == MediaType.AUDIO) {
                    request.addOption("-f", "ba/b")
                    request.addOption("-x")
                    request.addOption("--audio-format", "mp3")
                } else {
                    request.addOption("-S", "res,ext:mp4:m4a")
                    request.addOption("-f", "bv*+ba/b")
                    request.addOption("--merge-output-format", "mp4")
                }
            }
        }

        return request
    }

    fun getPublicDownloadDir(): File {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val appFolder = File(publicDownloads, "Downloader")
        if (!appFolder.exists()) {
            appFolder.mkdirs()
        }
        return appFolder
    }

    private fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return "00:00"
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, secs)
        }
    }

    private fun estimateAudioSize(durationSeconds: Long, bitrateKbps: Int): String {
        if (durationSeconds <= 0) return "~8.0 MB"
        val megabytes = (durationSeconds * (bitrateKbps * 1000L / 8)) / (1024.0 * 1024.0)
        return String.format(Locale.US, "~%.1f MB", megabytes)
    }

    private fun estimateVideoSize(durationSeconds: Long, height: Int): String {
        if (durationSeconds <= 0) {
            return when (height) {
                4320 -> "~750 MB"
                2160 -> "~350 MB"
                1440 -> "~220 MB"
                1080 -> "~150 MB"
                720 -> "~75 MB"
                480 -> "~35 MB"
                360 -> "~20 MB"
                240 -> "~12 MB"
                144 -> "~6 MB"
                else -> "~35 MB"
            }
        }
        val estimatedBitrateKbps = when (height) {
            4320 -> 40000L
            2160 -> 18000L
            1440 -> 9000L
            1080 -> 4500L
            720 -> 2200L
            480 -> 1000L
            360 -> 600L
            240 -> 350L
            144 -> 150L
            else -> 1000L
        }
        val megabytes = (durationSeconds * (estimatedBitrateKbps * 1000L / 8)) / (1024.0 * 1024.0)
        return String.format(Locale.US, "~%.1f MB", megabytes)
    }
}
