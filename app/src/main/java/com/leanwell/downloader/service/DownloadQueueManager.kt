package com.leanwell.downloader.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.leanwell.downloader.DownloaderApp
import com.leanwell.downloader.model.ActiveDownload
import com.leanwell.downloader.model.DownloadStatus
import com.leanwell.downloader.model.MediaItem
import com.leanwell.downloader.model.MediaType
import com.leanwell.downloader.model.Platform
import com.leanwell.downloader.utils.DownloadErrorHelper
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

@Immutable
data class QueuedDownloadTask(
    val download: ActiveDownload,
    val request: YoutubeDLRequest,
    val targetDirectory: File,
    val authorOrChannel: String,
    val durationText: String
)

object DownloadQueueManager {

    private const val TAG = "DownloadQueueManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _downloads = MutableStateFlow<List<ActiveDownload>>(emptyList())
    val downloads: StateFlow<List<ActiveDownload>> = _downloads.asStateFlow()

    private val _completedMediaItems = MutableSharedFlow<MediaItem>(extraBufferCapacity = 64)
    val completedMediaItems: SharedFlow<MediaItem> = _completedMediaItems.asSharedFlow()

    private val tasks = ConcurrentHashMap<String, QueuedDownloadTask>()
    val activeRunningTaskIds = ConcurrentHashMap.newKeySet<String>()

    val currentRunningTaskId: String?
        get() = activeRunningTaskIds.firstOrNull()

    fun pollNextQueuedTask(): QueuedDownloadTask? {
        synchronized(this) {
            val next = _downloads.value.firstOrNull {
                it.status == DownloadStatus.QUEUED && !activeRunningTaskIds.contains(it.id)
            } ?: return null
            activeRunningTaskIds.add(next.id)
            return tasks[next.id]
        }
    }

    fun releaseTask(id: String) {
        activeRunningTaskIds.remove(id)
    }

    fun isRunning(id: String): Boolean = activeRunningTaskIds.contains(id)

    fun enqueue(task: QueuedDownloadTask) {
        tasks[task.download.id] = task

        val updated = _downloads.value.toMutableList()
        val index = updated.indexOfFirst { it.id == task.download.id }
        if (index >= 0) {
            updated[index] = task.download
        } else {
            updated.add(task.download)
        }
        _downloads.value = updated

        ensureServiceRunning()
    }

    fun getTask(id: String): QueuedDownloadTask? = tasks[id]

    fun updateDownload(updated: ActiveDownload) {
        _downloads.value = _downloads.value.map {
            if (it.id == updated.id) updated else it
        }
    }

    fun updateProgress(
        id: String,
        progress: Float,
        speed: String,
        eta: String,
        downloadedBytes: String? = null,
        totalBytes: String? = null,
        statusMessage: String? = null
    ) {
        _downloads.value = _downloads.value.map {
            if (it.id == id) {
                it.copy(
                    progress = progress,
                    speed = speed.ifEmpty { it.speed },
                    eta = eta.ifEmpty { it.eta },
                    downloadedBytes = downloadedBytes ?: it.downloadedBytes,
                    totalBytes = totalBytes ?: it.totalBytes,
                    statusMessage = statusMessage,
                    status = if (statusMessage?.contains("Converting", ignoreCase = true) == true) {
                        DownloadStatus.CONVERTING
                    } else {
                        DownloadStatus.DOWNLOADING
                    }
                )
            } else {
                it
            }
        }
    }

    fun markCompleted(id: String, downloadedFile: File?) {
        val task = tasks[id]
        if (task != null) {
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val sizeText = if (downloadedFile != null && downloadedFile.exists()) {
                val mb = downloadedFile.length() / (1024.0 * 1024.0)
                String.format(Locale.US, "%.1f MB", mb)
            } else {
                task.download.totalBytes.ifEmpty { "Audio/Video" }
            }

            val cleanTitle = com.leanwell.downloader.utils.ArtworkHelper.cleanTitle(task.download.title)
            val artworkUri = if (downloadedFile != null) {
                com.leanwell.downloader.utils.ArtworkHelper.getArtworkUriForFile(DownloaderApp.instance, downloadedFile)
            } else null

            val baseDownloadDir = com.leanwell.downloader.engine.YoutubeEngine.getPublicDownloadDir()
            val parentFile = downloadedFile?.parentFile
            val playlistName = if (parentFile != null && parentFile.absolutePath != baseDownloadDir.absolutePath) {
                parentFile.name
            } else null

            val mediaItem = MediaItem(
                id = id,
                title = cleanTitle,
                artistOrChannel = task.authorOrChannel,
                duration = task.durationText,
                fileSize = sizeText,
                mediaType = task.download.mediaType,
                formatTag = task.download.formatLabel,
                platform = task.download.platform,
                dateAdded = dateFormat.format(Date()),
                filePath = downloadedFile?.absolutePath,
                artworkUri = artworkUri,
                playlistName = playlistName
            )
            scope.launch {
                _completedMediaItems.emit(mediaItem)
            }
        }

        // Show 100% completed state first in the queue UI
        _downloads.value = _downloads.value.map {
            if (it.id == id) {
                it.copy(
                    progress = 1.0f,
                    status = DownloadStatus.COMPLETED,
                    speed = "Complete",
                    eta = ""
                )
            } else {
                it
            }
        }

        // Release task from active running set immediately
        releaseTask(id)

        // Keep completed card for 3 seconds so user sees full progress before dismissal
        scope.launch {
            kotlinx.coroutines.delay(3000L)
            tasks.remove(id)
            _downloads.value = _downloads.value.filterNot { it.id == id }
            triggerNextIfAvailable()
        }
    }

    fun markFailed(id: String, errorMsg: String) {
        Log.e(TAG, "Download failed for $id: $errorMsg")
        releaseTask(id)
        val notice = DownloadErrorHelper.parse(errorMsg)
        _downloads.value = _downloads.value.map {
            if (it.id == id) it.copy(status = DownloadStatus.ERROR, statusMessage = errorMsg) else it
        }

        // Show immediate user toast notification
        scope.launch(Dispatchers.Main) {
            try {
                Toast.makeText(
                    DownloaderApp.instance,
                    "${notice.title}: ${notice.userFriendlyMessage}",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to display failure toast: ${e.message}")
            }
        }

        triggerNextIfAvailable()
    }

    fun pauseDownload(id: String) {
        if (activeRunningTaskIds.contains(id)) {
            try {
                YoutubeDL.getInstance().destroyProcessById(id)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to destroy process $id", e)
            }
            activeRunningTaskIds.remove(id)
        }
        _downloads.value = _downloads.value.map {
            if (it.id == id) it.copy(status = DownloadStatus.PAUSED, speed = "Paused", eta = "") else it
        }
        triggerNextIfAvailable()
    }

    fun resumeDownload(id: String) {
        _downloads.value = _downloads.value.map {
            if (it.id == id) it.copy(status = DownloadStatus.QUEUED, speed = "Queued", eta = "") else it
        }
        ensureServiceRunning()
    }

    fun cancelDownload(id: String) {
        if (activeRunningTaskIds.contains(id)) {
            try {
                YoutubeDL.getInstance().destroyProcessById(id)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to destroy process $id", e)
            }
            activeRunningTaskIds.remove(id)
        }
        tasks.remove(id)
        _downloads.value = _downloads.value.filterNot { it.id == id }
        triggerNextIfAvailable()
    }

    fun pauseAll() {
        activeRunningTaskIds.forEach { taskId ->
            try {
                YoutubeDL.getInstance().destroyProcessById(taskId)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to destroy process $taskId", e)
            }
        }
        activeRunningTaskIds.clear()
        _downloads.value = _downloads.value.map {
            if (it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.CONVERTING) {
                it.copy(status = DownloadStatus.PAUSED, speed = "Paused", eta = "")
            } else {
                it
            }
        }
    }

    fun cancelAll() {
        activeRunningTaskIds.forEach { taskId ->
            try {
                YoutubeDL.getInstance().destroyProcessById(taskId)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to destroy process $taskId", e)
            }
        }
        activeRunningTaskIds.clear()
        tasks.clear()
        _downloads.value = emptyList()
    }

    fun getNextQueuedTask(): QueuedDownloadTask? = pollNextQueuedTask()

    fun setCurrentRunning(id: String?) {
        if (id != null) {
            activeRunningTaskIds.add(id)
        } else {
            activeRunningTaskIds.clear()
        }
    }

    private fun ensureServiceRunning() {
        val hasWork = _downloads.value.any { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING }
        if (hasWork) {
            val context = DownloaderApp.instance
            val serviceIntent = Intent(context, DownloadForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }

    private fun triggerNextIfAvailable() {
        val hasWork = _downloads.value.any { it.status == DownloadStatus.QUEUED }
        if (hasWork) {
            ensureServiceRunning()
        }
    }
}
