package com.leanwell.downloader.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object ArtworkHelper {

    private const val TAG = "ArtworkHelper"

    /**
     * Extracts embedded album art / thumbnail from an audio or video file and caches it locally.
     * Reuses [existingRetriever] if provided, avoiding redundant file opens and metadata extraction.
     * Returns a [Uri] pointing to the cached artwork file, or null if no embedded art is present.
     */
    fun getArtworkUriForFile(
        context: Context,
        file: File,
        existingRetriever: MediaMetadataRetriever? = null
    ): String? {
        if (!file.exists() || !file.canRead()) return null

        val cacheDir = File(context.cacheDir, "artworks").apply { if (!exists()) mkdirs() }
        val artworkFileName = "art_${file.name.hashCode()}_${file.lastModified()}.jpg"
        val cachedFile = File(cacheDir, artworkFileName)

        if (cachedFile.exists() && cachedFile.length() > 0) {
            return Uri.fromFile(cachedFile).toString()
        }

        val retrieverToUse = existingRetriever ?: MediaMetadataRetriever()
        val shouldRelease = existingRetriever == null

        return try {
            if (shouldRelease) {
                retrieverToUse.setDataSource(file.absolutePath)
            }
            val pictureBytes = retrieverToUse.embeddedPicture
            if (pictureBytes != null && pictureBytes.isNotEmpty()) {
                FileOutputStream(cachedFile).use { out ->
                    out.write(pictureBytes)
                    out.flush()
                }
                Uri.fromFile(cachedFile).toString()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract embedded artwork for: ${file.name}", e)
            null
        } finally {
            if (shouldRelease) {
                try {
                    retrieverToUse.release()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Cleans filename formatting tags (e.g. "[320k]", "[1080p]", "[8K]", "(1)")
     * to return the pristine video / song title.
     */
    fun cleanTitle(rawTitle: String): String {
        return rawTitle
            .replace(Regex("""\s*\[(320k|192k|M4A|1080p|720p|480p|360p|240p|144p|8K|4K|2K|Audio|Video)\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\(\d+\)$"""), "")
            .replace(Regex("""\.(mp3|m4a|mp4|webm|mkv)$""", RegexOption.IGNORE_CASE), "")
            .trim()
    }
}
