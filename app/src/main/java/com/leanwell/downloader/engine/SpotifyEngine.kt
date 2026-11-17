package com.leanwell.downloader.engine

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.regex.Pattern

data class SpotifyTrackInfo(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val durationText: String,
    val coverUrl: String?,
    val searchQuery: String
)

data class SpotifyCollectionInfo(
    val id: String,
    val title: String,
    val creatorOrArtist: String,
    val coverUrl: String?,
    val isAlbum: Boolean,
    val tracks: List<SpotifyTrackInfo>
)

object SpotifyEngine {
    private const val TAG = "SpotifyEngine"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    fun isSpotifyUrl(url: String): Boolean {
        val lower = url.lowercase().trim()
        return lower.contains("open.spotify.com") || lower.contains("spotify.link")
    }

    fun isTrackUrl(url: String): Boolean {
        val lower = url.lowercase().trim()
        return lower.contains("/track/")
    }

    fun isPlaylistOrAlbumUrl(url: String): Boolean {
        val lower = url.lowercase().trim()
        return lower.contains("/playlist/") || lower.contains("/album/")
    }

    /**
     * Resolves metadata for a single Spotify track.
     */
    suspend fun resolveTrack(url: String): SpotifyTrackInfo? = withContext(Dispatchers.IO) {
        val cleanUrl = sanitizeSpotifyUrl(url)
        val trackId = extractIdFromUrl(cleanUrl, "track")

        // First attempt: Scrape embed page for rich track details (artist, album, duration, artwork)
        if (trackId != null) {
            val embedInfo = fetchTrackFromEmbed(trackId)
            if (embedInfo != null) return@withContext embedInfo
        }

        // Fallback: Spotify oEmbed API (Public, highly reliable)
        fetchTrackFromOembed(cleanUrl, trackId ?: "spotify_track")
    }

    /**
     * Resolves metadata and track list for a Spotify Playlist or Album.
     */
    suspend fun resolveCollection(url: String): SpotifyCollectionInfo? = withContext(Dispatchers.IO) {
        val cleanUrl = sanitizeSpotifyUrl(url)
        val isAlbum = cleanUrl.contains("/album/")
        val type = if (isAlbum) "album" else "playlist"
        val collectionId = extractIdFromUrl(cleanUrl, type) ?: return@withContext null

        fetchCollectionFromEmbed(type, collectionId, isAlbum)
    }

    private fun sanitizeSpotifyUrl(url: String): String {
        val uri = Uri.parse(url.trim())
        val host = uri.host ?: "open.spotify.com"
        val path = uri.path ?: ""
        return "https://$host$path"
    }

    private fun extractIdFromUrl(url: String, type: String): String? {
        val pattern = Pattern.compile("/$type/([a-zA-Z0-9]+)")
        val matcher = pattern.matcher(url)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun fetchTrackFromEmbed(trackId: String): SpotifyTrackInfo? {
        return try {
            val embedUrl = "https://open.spotify.com/embed/track/$trackId"
            val html = httpGet(embedUrl) ?: return null
            val nextData = extractNextDataJson(html) ?: return null

            val entity = nextData.optJSONObject("props")
                ?.optJSONObject("pageProps")
                ?.optJSONObject("state")
                ?.optJSONObject("data")
                ?.optJSONObject("entity") ?: return null

            val name = entity.optString("name", "Unknown Track")
            val artistsArray = entity.optJSONArray("artists")
            val artistList = mutableListOf<String>()
            if (artistsArray != null) {
                for (i in 0 until artistsArray.length()) {
                    val a = artistsArray.optJSONObject(i)?.optString("name")
                    if (!a.isNullOrBlank()) artistList.add(a)
                }
            }
            val artistStr = if (artistList.isNotEmpty()) artistList.joinToString(", ") else "Unknown Artist"
            val primaryArtist = artistList.firstOrNull() ?: artistStr

            val albumName = entity.optJSONObject("album")?.optString("name") ?: ""
            val durationMs = entity.optLong("duration", 0L)
            val durationText = formatDuration(durationMs)

            val coverSources = entity.optJSONObject("coverArt")?.optJSONArray("sources")
            var coverUrl: String? = null
            if (coverSources != null && coverSources.length() > 0) {
                coverUrl = coverSources.optJSONObject(0)?.optString("url")
            }

            SpotifyTrackInfo(
                id = trackId,
                title = name,
                artist = artistStr,
                album = albumName,
                durationMs = durationMs,
                durationText = durationText,
                coverUrl = coverUrl,
                searchQuery = "ytsearch1:$primaryArtist - $name audio"
            )
        } catch (e: Exception) {
            Log.w(TAG, "fetchTrackFromEmbed failed: ${e.message}")
            null
        }
    }

    private fun fetchTrackFromOembed(url: String, trackId: String): SpotifyTrackInfo? {
        return try {
            val oembedEndpoint = "https://open.spotify.com/oembed?url=" + URLEncoder.encode(url, "UTF-8")
            val jsonStr = httpGet(oembedEndpoint) ?: return null
            val obj = JSONObject(jsonStr)

            val fullTitle = obj.optString("title", "Spotify Track")
            val coverUrl = obj.optString("thumbnail_url").takeIf { it.isNotBlank() }

            // oEmbed title format is usually: "Title" or "Title by Artist"
            var title = fullTitle
            var artist = "Spotify Artist"
            if (fullTitle.contains(" by ")) {
                val parts = fullTitle.split(" by ")
                title = parts[0].trim()
                artist = parts.getOrNull(1)?.trim() ?: "Spotify Artist"
            }

            SpotifyTrackInfo(
                id = trackId,
                title = title,
                artist = artist,
                album = "",
                durationMs = 0L,
                durationText = "03:30",
                coverUrl = coverUrl,
                searchQuery = "ytsearch1:$artist - $title audio"
            )
        } catch (e: Exception) {
            Log.w(TAG, "fetchTrackFromOembed failed: ${e.message}")
            null
        }
    }

    private fun fetchCollectionFromEmbed(type: String, id: String, isAlbum: Boolean): SpotifyCollectionInfo? {
        return try {
            val embedUrl = "https://open.spotify.com/embed/$type/$id"
            val html = httpGet(embedUrl) ?: return null
            val nextData = extractNextDataJson(html) ?: return null

            val entity = nextData.optJSONObject("props")
                ?.optJSONObject("pageProps")
                ?.optJSONObject("state")
                ?.optJSONObject("data")
                ?.optJSONObject("entity") ?: return null

            val title = entity.optString("name", if (isAlbum) "Spotify Album" else "Spotify Playlist")
            val subtitle = entity.optString("subtitle", "")
            val coverSources = entity.optJSONObject("coverArt")?.optJSONArray("sources")
            var coverUrl: String? = null
            if (coverSources != null && coverSources.length() > 0) {
                coverUrl = coverSources.optJSONObject(0)?.optString("url")
            }

            val tracksList = mutableListOf<SpotifyTrackInfo>()
            val trackListArray = entity.optJSONArray("trackList")
            if (trackListArray != null) {
                for (i in 0 until trackListArray.length()) {
                    val item = trackListArray.optJSONObject(i) ?: continue
                    val trackTitle = item.optString("title", "Track ${i + 1}")
                    val trackSubtitle = item.optString("subtitle", subtitle)
                    val durationMs = item.optLong("duration", 0L)
                    val trackUri = item.optString("uri", "")
                    val trackId = trackUri.substringAfterLast(":", "t_$i")

                    val primaryArtist = trackSubtitle.split(",").firstOrNull()?.trim() ?: trackSubtitle

                    tracksList.add(
                        SpotifyTrackInfo(
                            id = trackId,
                            title = trackTitle,
                            artist = trackSubtitle,
                            album = title,
                            durationMs = durationMs,
                            durationText = formatDuration(durationMs),
                            coverUrl = coverUrl,
                            searchQuery = "ytsearch1:$primaryArtist - $trackTitle audio"
                        )
                    )
                }
            }

            SpotifyCollectionInfo(
                id = id,
                title = title,
                creatorOrArtist = if (subtitle.isNotBlank()) subtitle else if (isAlbum) "Album" else "Playlist",
                coverUrl = coverUrl,
                isAlbum = isAlbum,
                tracks = tracksList
            )
        } catch (e: Exception) {
            Log.e(TAG, "fetchCollectionFromEmbed failed: ${e.message}", e)
            null
        }
    }

    private fun extractNextDataJson(html: String): JSONObject? {
        val pattern = Pattern.compile("<script id=\"__NEXT_DATA__\" type=\"application/json\">(.*?)</script>", Pattern.DOTALL)
        val matcher = pattern.matcher(html)
        if (matcher.find()) {
            val jsonStr = matcher.group(1) ?: return null
            return JSONObject(jsonStr)
        }
        return null
    }

    private fun httpGet(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,application/json,*/*;q=0.8")
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            if (conn.responseCode in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                    reader.readText()
                }
            } else {
                Log.w(TAG, "HTTP GET failed with code ${conn.responseCode} for $urlStr")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "HTTP GET error: ${e.message}")
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0L) return "03:30"
        val totalSec = durationMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%02d:%02d", min, sec)
    }
}
