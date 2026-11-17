package com.leanwell.downloader.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaScannerConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.leanwell.downloader.model.DownloadStatus
import com.leanwell.downloader.model.MediaType
import com.leanwell.downloader.security.SecureCookieVault
import com.leanwell.downloader.settings.AppSettings
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

class DownloadForegroundService : Service() {

    companion object {
        private const val TAG = "DownloadService"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var notificationHelper: NotificationHelper
    private val activeWorkerJobs = ConcurrentHashMap<Int, Job>()
    private var lastNotificationTime = 0L

    // Regex for parsing yt-dlp / aria2 stdout
    private val speedPattern = Pattern.compile("([0-9.]+\\s*[kKmMgG]i?B/s)")

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        startInForeground()
    }

    private fun startInForeground() {
        val initialNotification = notificationHelper.buildForegroundNotification(null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.FOREGROUND_NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NotificationHelper.FOREGROUND_NOTIFICATION_ID, initialNotification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val downloadId = intent?.getStringExtra(NotificationHelper.EXTRA_DOWNLOAD_ID)

        when (action) {
            NotificationHelper.ACTION_CANCEL -> {
                if (downloadId != null) {
                    DownloadQueueManager.cancelDownload(downloadId)
                }
            }
            NotificationHelper.ACTION_PAUSE -> {
                if (downloadId != null) {
                    DownloadQueueManager.pauseDownload(downloadId)
                }
            }
            NotificationHelper.ACTION_RESUME -> {
                if (downloadId != null) {
                    DownloadQueueManager.resumeDownload(downloadId)
                }
            }
        }

        processQueue()
        return START_NOT_STICKY
    }

    private fun processQueue() {
        val maxConcurrent = if (com.leanwell.downloader.settings.AppSettings.isParallelDownloads.value) {
            com.leanwell.downloader.settings.AppSettings.maxParallelCount.value
        } else {
            1
        }

        for (workerIndex in 0 until maxConcurrent) {
            val existingJob = activeWorkerJobs[workerIndex]
            if (existingJob == null || !existingJob.isActive) {
                activeWorkerJobs[workerIndex] = serviceScope.launch {
                    runWorkerLoop(workerIndex)
                }
            }
        }
    }

    private suspend fun CoroutineScope.runWorkerLoop(workerIndex: Int) {
        while (isActive) {
            val task = DownloadQueueManager.pollNextQueuedTask()
            if (task == null) {
                break
            }

            // Enforce Wi-Fi only policy if enabled in Settings
            if (com.leanwell.downloader.settings.AppSettings.isWifiOnly.value &&
                !com.leanwell.downloader.utils.NetworkUtils.isWifiOrEthernet(applicationContext)
            ) {
                Log.w(TAG, "[Worker $workerIndex] Download paused: Wi-Fi only mode is enabled but device is on cellular data")
                val pausedState = task.download.copy(
                    status = DownloadStatus.PAUSED,
                    speed = "",
                    eta = "Paused",
                    statusMessage = "Waiting for Wi-Fi..."
                )
                DownloadQueueManager.updateDownload(pausedState)
                DownloadQueueManager.releaseTask(task.download.id)
                break
            }

            val initialDownloadingState = task.download.copy(
                status = DownloadStatus.DOWNLOADING,
                speed = "",
                eta = "Starting...",
                statusMessage = "Connecting..."
            )
            DownloadQueueManager.updateDownload(initialDownloadingState)
            updateAggregateNotification()

            val downloadStartTime = System.currentTimeMillis()
            var lastProgressUpdate = 0L
            var maxProgressSeen = 0f
            var currentStream = 1
            val isVideo = task.download.mediaType == MediaType.VIDEO

            try {
                Log.d(TAG, "[Worker $workerIndex] Starting download: ${task.download.title} (${task.download.id})")

                val progressCallback: (Float, Long, String) -> Unit = { progress, etaInSeconds, line ->
                    val now = System.currentTimeMillis()

                    // Detect second stream download (e.g. audio track after video track)
                    if (line.contains("[download] Destination:", ignoreCase = true) && maxProgressSeen > 0.4f) {
                        currentStream = 2
                    }

                    // Extract speed and status message
                    var speedText = ""
                    val matcher = speedPattern.matcher(line)
                    if (matcher.find()) {
                        speedText = matcher.group(1)?.trim() ?: ""
                    }

                    val etaText = if (etaInSeconds > 0) {
                        if (etaInSeconds < 60) "${etaInSeconds}s left" else "${etaInSeconds / 60}m left"
                    } else {
                        ""
                    }

                    var totalBytesText: String? = null
                    val totalMatcher = Pattern.compile("of\\s+~?\\s*([0-9.]+\\s*[kKmMgG]i?B)").matcher(line)
                    if (totalMatcher.find()) {
                        totalBytesText = totalMatcher.group(1)
                    }

                    var statusMsg: String? = null
                    if (line.contains("Solving JS challenges", ignoreCase = true)) {
                        statusMsg = "Bypassing YouTube bot-detection..."
                    } else if (line.contains("Downloading webpage", ignoreCase = true) || line.contains("Downloading mweb", ignoreCase = true) || line.contains("Downloading tv", ignoreCase = true) || line.contains("Downloading ios", ignoreCase = true) || line.contains("Downloading android", ignoreCase = true)) {
                        statusMsg = "Preparing media streams..."
                    } else if (line.contains("[ExtractAudio]", ignoreCase = true)) {
                        statusMsg = "Extracting audio with FFmpeg..."
                    } else if (line.contains("[EmbedThumbnail]", ignoreCase = true) || line.contains("[ThumbnailsConvertor]", ignoreCase = true)) {
                        statusMsg = "Embedding album art..."
                    } else if (line.contains("[Merger]", ignoreCase = true) || line.contains("[Fixup", ignoreCase = true)) {
                        statusMsg = "Merging video & audio with FFmpeg..."
                    } else if (line.contains("[Metadata]", ignoreCase = true)) {
                        statusMsg = "Writing media tags..."
                    } else if (line.contains("[download]", ignoreCase = true) || speedText.isNotEmpty() || progress > 0f) {
                        statusMsg = if (isVideo && currentStream == 2) "Downloading audio stream..." else "Downloading media stream..."
                    }

                    // Compute monotonic progress: smoothly progress through multi-stream downloads
                    if (progress > 0f) {
                        val rawNormalized = (progress / 100f).coerceIn(0f, 1f)
                        val scaled = if (isVideo) {
                            if (currentStream == 1) {
                                rawNormalized * 0.85f
                            } else {
                                0.85f + (rawNormalized * 0.12f)
                            }
                        } else {
                            rawNormalized * 0.92f
                        }
                        maxProgressSeen = maxOf(maxProgressSeen, scaled)
                    } else if (line.contains("[Merger]", ignoreCase = true) || line.contains("[Fixup", ignoreCase = true)) {
                        maxProgressSeen = maxOf(maxProgressSeen, 0.97f)
                    } else if (line.contains("[ExtractAudio]", ignoreCase = true) || line.contains("[EmbedThumbnail]", ignoreCase = true)) {
                        maxProgressSeen = maxOf(maxProgressSeen, 0.95f)
                    }

                    val normalizedProgress = maxProgressSeen.coerceIn(0f, 1f)

                    // Update at least every 200ms, or when status changes
                    if (now - lastProgressUpdate > 200 || progress >= 100f || statusMsg != null || speedText.isNotEmpty()) {
                        lastProgressUpdate = now

                        val downloadedBytesText = if (totalBytesText != null && normalizedProgress > 0f) {
                            val totalNum = totalBytesText.replace(Regex("[^0-9.]"), "").toDoubleOrNull()
                            if (totalNum != null) {
                                val current = totalNum * normalizedProgress
                                String.format(java.util.Locale.US, "%.1f %s", current, totalBytesText.replace(Regex("[0-9. ]"), ""))
                            } else null
                        } else null

                        DownloadQueueManager.updateProgress(
                            id = task.download.id,
                            progress = normalizedProgress,
                            speed = speedText,
                            eta = etaText,
                            downloadedBytes = downloadedBytesText,
                            totalBytes = totalBytesText,
                            statusMessage = statusMsg
                        )

                        updateAggregateNotification()
                    }
                }

                val cookieType = AppSettings.getCookieTypeForPlatform(task.download.platform)
                val serviceContext = this@DownloadForegroundService
                if (cookieType != null && SecureCookieVault.hasValidCookies(serviceContext, cookieType)) {
                    SecureCookieVault.withEphemeralCookieFile(serviceContext, cookieType) { ephemFile ->
                        if (ephemFile != null && ephemFile.exists()) {
                            task.request.addOption("--cookies", ephemFile.absolutePath)
                        }
                        YoutubeDL.getInstance().execute(task.request, task.download.id, progressCallback)
                    }
                } else {
                    YoutubeDL.getInstance().execute(task.request, task.download.id, progressCallback)
                }

                // Locate completed file in target directory
                val targetDir = task.targetDirectory
                val validMediaExtensions = setOf("mp4", "m4a", "mp3", "webm", "mkv", "ogg", "opus", "wav")
                val completedFile = targetDir.walkTopDown()
                    .filter { file ->
                        file.isFile &&
                        !file.name.endsWith(".part") &&
                        !file.name.endsWith(".ytdl") &&
                        !file.name.endsWith(".temp") &&
                        validMediaExtensions.contains(file.extension.lowercase()) &&
                        file.lastModified() >= downloadStartTime - 30000
                    }
                    .maxByOrNull { it.lastModified() }

                if (completedFile != null && completedFile.exists()) {
                    Log.d(TAG, "[Worker $workerIndex] Download finished successfully: ${completedFile.absolutePath}")
                    MediaScannerConnection.scanFile(
                        applicationContext,
                        arrayOf(completedFile.absolutePath),
                        null
                    ) { path, uri ->
                        Log.d(TAG, "Media scan complete for: $path -> $uri")
                    }

                    notificationHelper.notifyCompleted(task.download.title, task.download.id)
                    DownloadQueueManager.markCompleted(task.download.id, completedFile)
                } else {
                    Log.w(TAG, "[Worker $workerIndex] Download finished but could not locate specific output file in $targetDir")
                    notificationHelper.notifyCompleted(task.download.title, task.download.id)
                    DownloadQueueManager.markCompleted(task.download.id, null)
                }

            } catch (e: Exception) {
                DownloadQueueManager.releaseTask(task.download.id)
                val isCancelled = !isActive ||
                        !DownloadQueueManager.downloads.value.any { it.id == task.download.id } ||
                        DownloadQueueManager.downloads.value.find { it.id == task.download.id }?.status == DownloadStatus.PAUSED
                if (isCancelled) {
                    Log.d(TAG, "[Worker $workerIndex] Download was cancelled or paused: ${task.download.id}")
                } else {
                    Log.e(TAG, "[Worker $workerIndex] Download encountered an error for ${task.download.id}", e)
                    DownloadQueueManager.markFailed(task.download.id, e.message ?: "Download failed")
                }
            } finally {
                DownloadQueueManager.releaseTask(task.download.id)
            }
        }

        checkStopService()
    }

    private fun checkStopService() {
        val anyActiveWorker = activeWorkerJobs.values.any { it.isActive }
        val hasQueuedWork = DownloadQueueManager.downloads.value.any {
            it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING
        }
        if (!anyActiveWorker && !hasQueuedWork) {
            Log.d(TAG, "All workers idle and queue empty. Stopping foreground service.")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun updateAggregateNotification() {
        val now = System.currentTimeMillis()
        if (now - lastNotificationTime < 250) return
        lastNotificationTime = now

        val downloadingItems = DownloadQueueManager.downloads.value.filter { it.status == DownloadStatus.DOWNLOADING }
        val count = downloadingItems.size
        if (count > 1) {
            val avgProgress = downloadingItems.map { it.progress }.average().toFloat()
            val summaryTitle = "Downloading $count items in parallel"
            val summaryText = downloadingItems.joinToString(", ") { it.title.take(18) }
            val first = downloadingItems.first().copy(
                title = summaryTitle,
                progress = avgProgress,
                statusMessage = summaryText
            )
            notificationHelper.updateProgress(first)
        } else if (downloadingItems.isNotEmpty()) {
            notificationHelper.updateProgress(downloadingItems.first())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
