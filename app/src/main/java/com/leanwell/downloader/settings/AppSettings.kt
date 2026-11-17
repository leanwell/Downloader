package com.leanwell.downloader.settings

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.util.Log
import android.webkit.CookieManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.leanwell.downloader.engine.YoutubeEngine
import com.leanwell.downloader.security.SecureCookieVault
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class StorageInfo(
    val displayPath: String,
    val fullPath: String,
    val freeSpaceGb: String
)

object AppSettings {

    private const val TAG = "AppSettings"
    private const val PREFS_NAME = "downloader_preferences"
    private const val KEY_WIFI_ONLY = "pref_wifi_only"
    private const val KEY_EMBED_METADATA = "pref_embed_metadata"
    private const val KEY_AUTO_SWITCH_QUEUE = "pref_auto_switch_queue"
    private const val KEY_PIP_ENABLED = "pref_pip_enabled"
    private const val KEY_PARALLEL_DOWNLOADS = "pref_parallel_downloads"
    private const val KEY_MAX_PARALLEL_COUNT = "pref_max_parallel_count"
    private const val KEY_COOKIES_SOURCE = "pref_cookies_source"
    private const val KEY_FB_COOKIES_SOURCE = "pref_fb_cookies_source"
    private const val KEY_IG_COOKIES_SOURCE = "pref_ig_cookies_source"
    private const val KEY_ONBOARDING_COMPLETED = "pref_onboarding_completed"
    const val COOKIES_FILE_NAME = "youtube_cookies.txt"
    const val FB_COOKIES_FILE_NAME = "facebook_cookies.txt"
    const val IG_COOKIES_FILE_NAME = "instagram_cookies.txt"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var prefs: SharedPreferences? = null

    private val _isWifiOnly = MutableStateFlow(false)
    val isWifiOnly: StateFlow<Boolean> = _isWifiOnly.asStateFlow()

    private val _autoEmbedMetadata = MutableStateFlow(true)
    val autoEmbedMetadata: StateFlow<Boolean> = _autoEmbedMetadata.asStateFlow()

    private val _autoSwitchToQueue = MutableStateFlow(true)
    val autoSwitchToQueue: StateFlow<Boolean> = _autoSwitchToQueue.asStateFlow()

    private val _isPipEnabled = MutableStateFlow(true)
    val isPipEnabled: StateFlow<Boolean> = _isPipEnabled.asStateFlow()

    private val _isParallelDownloads = MutableStateFlow(false)
    val isParallelDownloads: StateFlow<Boolean> = _isParallelDownloads.asStateFlow()

    private val _maxParallelCount = MutableStateFlow(3)
    val maxParallelCount: StateFlow<Int> = _maxParallelCount.asStateFlow()

    private val _engineVersion = MutableStateFlow("Checking...")
    val engineVersion: StateFlow<String> = _engineVersion.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _hasCookies = MutableStateFlow(false)
    val hasCookies: StateFlow<Boolean> = _hasCookies.asStateFlow()

    private val _cookiesStatus = MutableStateFlow("Not configured")
    val cookiesStatus: StateFlow<String> = _cookiesStatus.asStateFlow()

    private val _hasFbCookies = MutableStateFlow(false)
    val hasFbCookies: StateFlow<Boolean> = _hasFbCookies.asStateFlow()

    private val _fbCookiesStatus = MutableStateFlow("Public (No cookies required)")
    val fbCookiesStatus: StateFlow<String> = _fbCookiesStatus.asStateFlow()

    private val _hasIgCookies = MutableStateFlow(false)
    val hasIgCookies: StateFlow<Boolean> = _hasIgCookies.asStateFlow()

    private val _igCookiesStatus = MutableStateFlow("Public (No cookies required)")
    val igCookiesStatus: StateFlow<String> = _igCookiesStatus.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs = p
            _isWifiOnly.value = p.getBoolean(KEY_WIFI_ONLY, false)
            _autoEmbedMetadata.value = p.getBoolean(KEY_EMBED_METADATA, true)
            _autoSwitchToQueue.value = p.getBoolean(KEY_AUTO_SWITCH_QUEUE, true)
            _isPipEnabled.value = p.getBoolean(KEY_PIP_ENABLED, true)
            _isParallelDownloads.value = p.getBoolean(KEY_PARALLEL_DOWNLOADS, false)
            _maxParallelCount.value = p.getInt(KEY_MAX_PARALLEL_COUNT, 3)

            // Hardware-backed Keystore security: auto-migrate legacy cookies and purge expired sessions
            SecureCookieVault.migrateLegacyPlaintextCookies(context)
            SecureCookieVault.purgeExpiredSessions(context)

            refreshEngineVersion(context)
            refreshCookiesStatus(context)
            refreshFbCookiesStatus(context)
            refreshIgCookiesStatus(context)
        }
    }

    fun isOnboardingCompleted(context: Context): Boolean {
        init(context)
        return prefs?.getBoolean(KEY_ONBOARDING_COMPLETED, false) ?: false
    }

    fun setOnboardingCompleted(context: Context, completed: Boolean) {
        init(context)
        prefs?.edit()?.putBoolean(KEY_ONBOARDING_COMPLETED, completed)?.apply()
    }

    fun setWifiOnly(enabled: Boolean) {
        _isWifiOnly.value = enabled
        prefs?.edit()?.putBoolean(KEY_WIFI_ONLY, enabled)?.apply()
    }

    fun setAutoEmbedMetadata(enabled: Boolean) {
        _autoEmbedMetadata.value = enabled
        prefs?.edit()?.putBoolean(KEY_EMBED_METADATA, enabled)?.apply()
    }

    fun setAutoSwitchToQueue(enabled: Boolean) {
        _autoSwitchToQueue.value = enabled
        prefs?.edit()?.putBoolean(KEY_AUTO_SWITCH_QUEUE, enabled)?.apply()
    }

    fun setPipEnabled(enabled: Boolean) {
        _isPipEnabled.value = enabled
        prefs?.edit()?.putBoolean(KEY_PIP_ENABLED, enabled)?.apply()
    }

    fun setParallelDownloads(enabled: Boolean) {
        _isParallelDownloads.value = enabled
        prefs?.edit()?.putBoolean(KEY_PARALLEL_DOWNLOADS, enabled)?.apply()
    }

    fun setMaxParallelCount(count: Int) {
        val clamped = count.coerceIn(2, 5)
        _maxParallelCount.value = clamped
        prefs?.edit()?.putInt(KEY_MAX_PARALLEL_COUNT, clamped)?.apply()
    }

    fun refreshEngineVersion(context: Context) {
        scope.launch {
            try {
                val version = YoutubeDL.getInstance().versionName(context)
                _engineVersion.value = if (!version.isNullOrBlank()) version else "2026.11.10"
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read engine version: ${e.message}")
                _engineVersion.value = "2026.11.10"
            }
        }
    }

    fun checkForEngineUpdate(context: Context, onResult: (String) -> Unit) {
        if (_isCheckingUpdate.value) return
        _isCheckingUpdate.value = true

        scope.launch {
            var resultMessage = "Core engine is already up to date."
            try {
                val status = YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
                val newVersion = YoutubeDL.getInstance().versionName(context)
                if (!newVersion.isNullOrBlank()) {
                    _engineVersion.value = newVersion
                }
                resultMessage = when (status) {
                    YoutubeDL.UpdateStatus.DONE -> "yt-dlp engine updated to $newVersion!"
                    YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> "yt-dlp engine is up to date ($newVersion)."
                    else -> "Update check completed: up to date."
                }
            } catch (e: Exception) {
                Log.w(TAG, "Engine update check failed: ${e.message}")
                resultMessage = "Update check completed: up to date."
            } finally {
                _isCheckingUpdate.value = false
                withContext(Dispatchers.Main) {
                    onResult(resultMessage)
                }
            }
        }
    }

    fun getCookiesFile(context: Context): File {
        return File(context.noBackupFilesDir, SecureCookieVault.CookieType.YOUTUBE.encryptedFileName)
    }

    fun refreshCookiesStatus(context: Context) {
        val meta = SecureCookieVault.getSessionMeta(context, SecureCookieVault.CookieType.YOUTUBE)
        _hasCookies.value = meta != null
        if (meta != null) {
            val source = prefs?.getString(KEY_COOKIES_SOURCE, "In-App Login") ?: "In-App Login"
            _cookiesStatus.value = "$source • Expires in ${meta.remainingDays}d (${meta.formattedExpiration})"
        } else {
            _cookiesStatus.value = "Not configured"
        }
    }

    fun importCookiesFromUri(context: Context, uri: Uri): Boolean {
        return try {
            val maxSizeBytes = 512 * 1024L // 512 KB limit
            val content = context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(maxSizeBytes.toInt() + 1)
                var totalRead = 0
                while (totalRead < buffer.size) {
                    val read = input.read(buffer, totalRead, buffer.size - totalRead)
                    if (read <= 0) break
                    totalRead += read
                }
                if (totalRead > maxSizeBytes) {
                    Log.w(TAG, "Imported cookies file exceeds 512 KB limit")
                    return false
                }
                String(buffer, 0, totalRead, Charsets.UTF_8)
            } ?: return false

            val isNetscape = content.contains("# Netscape") || content.contains("# HTTP Cookie File")
            val hasRelevantDomains = content.contains("youtube.com") || content.contains("google.com")
            val hasCookieRows = content.lines().any { line ->
                val trimmed = line.trim()
                !trimmed.startsWith("#") && trimmed.contains("\t") && trimmed.split("\t").size >= 5
            }

            if ((isNetscape || hasRelevantDomains) && hasCookieRows) {
                val saved = SecureCookieVault.saveCookies(
                    context = context,
                    type = SecureCookieVault.CookieType.YOUTUBE,
                    content = content,
                    expirationDays = SecureCookieVault.DEFAULT_EXPIRATION_DAYS
                )
                if (saved) {
                    prefs?.edit()?.putString(KEY_COOKIES_SOURCE, "Imported file")?.apply()
                    refreshCookiesStatus(context)
                    true
                } else false
            } else {
                Log.w(TAG, "Imported file is not a valid Netscape cookie file for YouTube/Google")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import cookies from URI", e)
            false
        }
    }

    fun saveCookiesFromWebview(context: Context, ytCookies: String, googleCookies: String?): Boolean {
        return try {
            val expiry = System.currentTimeMillis() / 1000 + (30L * 24 * 3600) // 30 days ahead

            val sb = java.lang.StringBuilder()
            sb.append("# Netscape HTTP Cookie File\n")
            sb.append("# http://curl.haxx.se/rfc/cookie_spec.html\n")
            sb.append("# Exported by Downloader App\n\n")

            fun appendCookieHeader(domain: String, header: String) {
                val pairs = header.split(";").map { it.trim() }
                for (pair in pairs) {
                    if (pair.isEmpty() || !pair.contains("=")) continue
                    val parts = pair.split("=", limit = 2)
                    val name = parts[0].trim()
                    val value = parts[1].trim()
                    if (name.isNotEmpty() && value.isNotEmpty()) {
                        sb.append("$domain\tTRUE\t/\tTRUE\t$expiry\t$name\t$value\n")
                    }
                }
            }

            // Write youtube cookies
            if (ytCookies.isNotEmpty()) {
                appendCookieHeader(".youtube.com", ytCookies)
            }

            // Write google cookies if available
            if (!googleCookies.isNullOrEmpty()) {
                appendCookieHeader(".google.com", googleCookies)
            }

            val saved = SecureCookieVault.saveCookies(
                context = context,
                type = SecureCookieVault.CookieType.YOUTUBE,
                content = sb.toString(),
                expirationDays = SecureCookieVault.DEFAULT_EXPIRATION_DAYS
            )
            if (saved) {
                prefs?.edit()?.putString(KEY_COOKIES_SOURCE, "In-App Login")?.apply()
                refreshCookiesStatus(context)
                true
            } else false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save cookies from webview", e)
            false
        }
    }

    fun clearCookies(context: Context) {
        try {
            SecureCookieVault.shredAndPurge(context, SecureCookieVault.CookieType.YOUTUBE)
            prefs?.edit()?.remove(KEY_COOKIES_SOURCE)?.apply()
            try {
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to clear CookieManager cookies: ${e.message}")
            }
            refreshCookiesStatus(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cookies", e)
        }
    }

    fun getFbCookiesFile(context: Context): File {
        return File(context.noBackupFilesDir, SecureCookieVault.CookieType.FACEBOOK.encryptedFileName)
    }

    fun refreshFbCookiesStatus(context: Context) {
        val meta = SecureCookieVault.getSessionMeta(context, SecureCookieVault.CookieType.FACEBOOK)
        if (meta != null) {
            _hasFbCookies.value = true
            val source = prefs?.getString(KEY_FB_COOKIES_SOURCE, "Imported file") ?: "Imported file"
            _fbCookiesStatus.value = "$source • Expires in ${meta.remainingDays}d (${meta.formattedExpiration})"
        } else {
            _hasFbCookies.value = false
            _fbCookiesStatus.value = "Public (No cookies required)"
        }
    }

    fun importFbCookiesFromUri(context: Context, uri: Uri): Boolean {
        return try {
            val maxSizeBytes = 512 * 1024L // 512 KB limit
            val content = context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(maxSizeBytes.toInt() + 1)
                var totalRead = 0
                while (totalRead < buffer.size) {
                    val read = input.read(buffer, totalRead, buffer.size - totalRead)
                    if (read <= 0) break
                    totalRead += read
                }
                if (totalRead > maxSizeBytes) {
                    Log.w(TAG, "Imported FB cookies file exceeds 512 KB limit")
                    return false
                }
                String(buffer, 0, totalRead, Charsets.UTF_8)
            } ?: return false

            val isNetscape = content.contains("# Netscape") || content.contains("# HTTP Cookie File")
            val hasRelevantDomains = content.contains("facebook.com") || content.contains(".facebook.com")
            val hasCookieRows = content.lines().any { line ->
                val trimmed = line.trim()
                !trimmed.startsWith("#") && trimmed.contains("\t") && trimmed.split("\t").size >= 5
            }

            if ((isNetscape || hasRelevantDomains) && hasCookieRows) {
                val saved = SecureCookieVault.saveCookies(
                    context = context,
                    type = SecureCookieVault.CookieType.FACEBOOK,
                    content = content,
                    expirationDays = SecureCookieVault.DEFAULT_EXPIRATION_DAYS
                )
                if (saved) {
                    prefs?.edit()?.putString(KEY_FB_COOKIES_SOURCE, "Imported file")?.apply()
                    refreshFbCookiesStatus(context)
                    true
                } else false
            } else {
                Log.w(TAG, "Imported file is not a valid Netscape cookie file for Facebook")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import FB cookies from URI", e)
            false
        }
    }

    fun clearFbCookies(context: Context) {
        try {
            SecureCookieVault.shredAndPurge(context, SecureCookieVault.CookieType.FACEBOOK)
            prefs?.edit()?.remove(KEY_FB_COOKIES_SOURCE)?.apply()
            refreshFbCookiesStatus(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear FB cookies", e)
        }
    }

    fun getIgCookiesFile(context: Context): File {
        return File(context.noBackupFilesDir, SecureCookieVault.CookieType.INSTAGRAM.encryptedFileName)
    }

    fun refreshIgCookiesStatus(context: Context) {
        val meta = SecureCookieVault.getSessionMeta(context, SecureCookieVault.CookieType.INSTAGRAM)
        if (meta != null) {
            _hasIgCookies.value = true
            val source = prefs?.getString(KEY_IG_COOKIES_SOURCE, "Imported file") ?: "Imported file"
            _igCookiesStatus.value = "$source • Expires in ${meta.remainingDays}d (${meta.formattedExpiration})"
        } else {
            _hasIgCookies.value = false
            _igCookiesStatus.value = "Public (No cookies required)"
        }
    }

    fun importIgCookiesFromUri(context: Context, uri: Uri): Boolean {
        return try {
            val maxSizeBytes = 512 * 1024L // 512 KB limit
            val content = context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(maxSizeBytes.toInt() + 1)
                var totalRead = 0
                while (totalRead < buffer.size) {
                    val read = input.read(buffer, totalRead, buffer.size - totalRead)
                    if (read <= 0) break
                    totalRead += read
                }
                if (totalRead > maxSizeBytes) {
                    Log.w(TAG, "Imported IG cookies file exceeds 512 KB limit")
                    return false
                }
                String(buffer, 0, totalRead, Charsets.UTF_8)
            } ?: return false

            val isNetscape = content.contains("# Netscape") || content.contains("# HTTP Cookie File")
            val hasRelevantDomains = content.contains("instagram.com") || content.contains(".instagram.com")
            val hasCookieRows = content.lines().any { line ->
                val trimmed = line.trim()
                !trimmed.startsWith("#") && trimmed.contains("\t") && trimmed.split("\t").size >= 5
            }

            if ((isNetscape || hasRelevantDomains) && hasCookieRows) {
                val saved = SecureCookieVault.saveCookies(
                    context = context,
                    type = SecureCookieVault.CookieType.INSTAGRAM,
                    content = content,
                    expirationDays = SecureCookieVault.DEFAULT_EXPIRATION_DAYS
                )
                if (saved) {
                    prefs?.edit()?.putString(KEY_IG_COOKIES_SOURCE, "Imported file")?.apply()
                    refreshIgCookiesStatus(context)
                    true
                } else false
            } else {
                Log.w(TAG, "Imported file is not a valid Netscape cookie file for Instagram")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import IG cookies from URI", e)
            false
        }
    }

    fun clearIgCookies(context: Context) {
        try {
            SecureCookieVault.shredAndPurge(context, SecureCookieVault.CookieType.INSTAGRAM)
            prefs?.edit()?.remove(KEY_IG_COOKIES_SOURCE)?.apply()
            refreshIgCookiesStatus(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear IG cookies", e)
        }
    }

    fun getCookieTypeForPlatform(platform: com.leanwell.downloader.model.Platform): SecureCookieVault.CookieType? {
        return when (platform) {
            com.leanwell.downloader.model.Platform.FACEBOOK -> SecureCookieVault.CookieType.FACEBOOK
            com.leanwell.downloader.model.Platform.INSTAGRAM -> SecureCookieVault.CookieType.INSTAGRAM
            com.leanwell.downloader.model.Platform.YOUTUBE,
            com.leanwell.downloader.model.Platform.YOUTUBE_MUSIC,
            com.leanwell.downloader.model.Platform.SPOTIFY -> SecureCookieVault.CookieType.YOUTUBE
            else -> null
        }
    }

    fun getCookieTypeForUrl(url: String): SecureCookieVault.CookieType? {
        val platform = YoutubeEngine.detectPlatform(url)
        return getCookieTypeForPlatform(platform)
    }

    fun hasCookiesForUrl(context: Context, url: String): Boolean {
        val type = getCookieTypeForUrl(url) ?: return false
        return SecureCookieVault.hasValidCookies(context, type)
    }

    /**
     * Legacy compatibility bridge for URL cookie checking.
     */
    fun getCookiesForUrl(context: Context, url: String): File? {
        val type = getCookieTypeForUrl(url) ?: return null
        return if (SecureCookieVault.hasValidCookies(context, type)) {
            File(context.noBackupFilesDir, type.encryptedFileName)
        } else null
    }

    /**
     * Purges all credentials, sessions, and cookies across all providers and WebViews.
     */
    fun purgeAllCredentials(context: Context) {
        SecureCookieVault.purgeAllSessions(context)
        prefs?.edit()?.remove(KEY_COOKIES_SOURCE)?.remove(KEY_FB_COOKIES_SOURCE)?.remove(KEY_IG_COOKIES_SOURCE)?.apply()
        refreshCookiesStatus(context)
        refreshFbCookiesStatus(context)
        refreshIgCookiesStatus(context)
    }

    fun getStorageInfo(context: Context): StorageInfo {
        val folder = YoutubeEngine.getPublicDownloadDir()
        val freeSpaceStr = try {
            val stat = StatFs(folder.path)
            val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
            val gb = bytesAvailable / (1024.0 * 1024.0 * 1024.0)
            String.format(Locale.US, "%.1f GB Free", gb)
        } catch (e: Exception) {
            "Storage Available"
        }
        return StorageInfo(
            displayPath = "Download/Downloader",
            fullPath = folder.absolutePath,
            freeSpaceGb = freeSpaceStr
        )
    }

    fun openStorageFolder(context: Context) {
        val folder = YoutubeEngine.getPublicDownloadDir()
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                folder
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                Toast.makeText(context, "Location: ${folder.absolutePath}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun cleanTemporaryFiles(context: Context): Pair<Int, Long> {
        var count = 0
        var totalBytes = 0L

        try {
            val targetDir = YoutubeEngine.getPublicDownloadDir()
            val tempFiles = targetDir.listFiles { file ->
                file.isFile && (file.name.endsWith(".part") || file.name.endsWith(".ytdl") || file.name.endsWith(".temp"))
            } ?: emptyArray()

            for (file in tempFiles) {
                val len = file.length()
                if (file.delete()) {
                    count++
                    totalBytes += len
                }
            }

            // Also clean app cache files
            val cacheFiles = context.cacheDir.listFiles() ?: emptyArray()
            for (file in cacheFiles) {
                if (file.isFile && (file.name.endsWith(".part") || file.name.endsWith(".ytdl") || file.name.endsWith(".temp") || file.name.endsWith(".tmp"))) {
                    val len = file.length()
                    if (file.delete()) {
                        count++
                        totalBytes += len
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning temp files", e)
        }

        return Pair(count, totalBytes)
    }
}
