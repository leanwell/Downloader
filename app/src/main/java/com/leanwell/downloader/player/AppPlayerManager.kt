package com.leanwell.downloader.player

import android.content.ComponentName
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.AspectRatioFrameLayout
import com.leanwell.downloader.DownloaderApp
import com.leanwell.downloader.model.MediaItem
import com.leanwell.downloader.utils.ArtworkHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

object AppPlayerManager {

    private const val TAG = "AppPlayerManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var mediaController: MediaController? = null
    private var progressJob: Job? = null

    private val _currentTrack = MutableStateFlow<MediaItem?>(null)
    val currentTrack: StateFlow<MediaItem?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    /**
     * Exposes the active [Player] instance for video surfaces (e.g. PlayerView).
     */
    val player: Player?
        get() = mediaController

    init {
        initController()
    }

    private fun initController(onReady: ((MediaController) -> Unit)? = null) {
        if (mediaController != null) {
            onReady?.invoke(mediaController!!)
            return
        }

        try {
            val context = DownloaderApp.instance
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            controllerFuture.addListener({
                try {
                    val controller = controllerFuture.get()
                    mediaController = controller
                    setupPlayerListener(controller)
                    Log.d(TAG, "MediaController successfully connected to PlaybackService")
                    onReady?.invoke(controller)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get MediaController from future", e)
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaController", e)
        }
    }

    private fun setupPlayerListener(controller: MediaController) {
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressTicker()
                } else {
                    stopProgressTicker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        val dur = controller.duration.coerceAtLeast(0L)
                        _durationMs.value = dur
                        val pos = controller.currentPosition.coerceAtLeast(0L)
                        _currentPositionMs.value = pos
                        if (dur > 0) {
                            _progress.value = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                        }
                    }
                    Player.STATE_ENDED -> {
                        _isPlaying.value = false
                        _currentPositionMs.value = _durationMs.value
                        _progress.value = 1f
                        stopProgressTicker()
                    }
                    else -> {}
                }
            }
        })
    }

    fun play(item: MediaItem) {
        val path = item.filePath
        if (path.isNullOrBlank() || !File(path).exists()) {
            Log.e(TAG, "Cannot play file: path is invalid or does not exist: $path")
            return
        }

        val context = DownloaderApp.instance
        val cleanTitle = ArtworkHelper.cleanTitle(item.title)
        val artUri = item.artworkUri ?: ArtworkHelper.getArtworkUriForFile(context, File(path))
        val updatedItem = item.copy(title = cleanTitle, artworkUri = artUri)
        _currentTrack.value = updatedItem

        val action: (MediaController) -> Unit = { controller ->
            try {
                // If clicking currently active track that's paused
                if (controller.currentMediaItem?.mediaId == item.id) {
                    if (!controller.isPlaying) {
                        controller.play()
                    }
                } else {
                    val mediaMetadata = MediaMetadata.Builder()
                        .setTitle(cleanTitle)
                        .setArtist(item.artistOrChannel)
                        .apply {
                            if (artUri != null) {
                                setArtworkUri(Uri.parse(artUri))
                            }
                        }
                        .build()

                    val media3Item = androidx.media3.common.MediaItem.Builder()
                        .setMediaId(item.id)
                        .setUri(Uri.fromFile(File(path)))
                        .setMediaMetadata(mediaMetadata)
                        .build()

                    controller.setMediaItem(media3Item)
                    controller.prepare()
                    controller.setPlaybackSpeed(_playbackSpeed.value)
                    controller.play()
                    Log.d(TAG, "Media3 playing: $cleanTitle (artUri=$artUri)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start Media3 playback for: $path", e)
            }
        }

        if (mediaController != null) {
            action(mediaController!!)
        } else {
            initController(action)
        }
    }

    fun pause() {
        try {
            mediaController?.pause()
            _isPlaying.value = false
            stopProgressTicker()
        } catch (e: Exception) {
            Log.w(TAG, "Error pausing playback: ${e.message}")
        }
    }

    fun resume() {
        try {
            mediaController?.play()
            _isPlaying.value = true
            startProgressTicker()
        } catch (e: Exception) {
            Log.w(TAG, "Error resuming playback: ${e.message}")
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            resume()
        }
    }

    fun seekTo(progressRatio: Float) {
        try {
            val ratio = progressRatio.coerceIn(0f, 1f)
            val dur = _durationMs.value
            if (dur > 0 && mediaController != null) {
                val targetMs = (dur * ratio).toLong()
                mediaController?.seekTo(targetMs)
                _currentPositionMs.value = targetMs
                _progress.value = ratio
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error seeking: ${e.message}")
        }
    }

    fun seekBy(deltaMs: Long) {
        try {
            val controller = mediaController ?: return
            val dur = _durationMs.value
            val currentPos = controller.currentPosition.coerceAtLeast(0L)
            val targetMs = (currentPos + deltaMs).coerceIn(0L, if (dur > 0) dur else Long.MAX_VALUE)
            controller.seekTo(targetMs)
            _currentPositionMs.value = targetMs
            if (dur > 0) {
                _progress.value = (targetMs.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error seeking by offset: ${e.message}")
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        try {
            val clamped = speed.coerceIn(0.25f, 3.0f)
            _playbackSpeed.value = clamped
            mediaController?.setPlaybackSpeed(clamped)
        } catch (e: Exception) {
            Log.w(TAG, "Error setting playback speed: ${e.message}")
        }
    }

    fun stop() {
        stopProgressTicker()
        try {
            mediaController?.stop()
            mediaController?.clearMediaItems()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping MediaController: ${e.message}")
        } finally {
            _isPlaying.value = false
            _currentPositionMs.value = 0L
            _durationMs.value = 0L
            _progress.value = 0f
            _currentTrack.value = null
        }
    }

    private fun startProgressTicker() {
        stopProgressTicker()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                try {
                    val controller = mediaController
                    if (controller != null) {
                        val pos = controller.currentPosition.coerceAtLeast(0L)
                        val dur = controller.duration.coerceAtLeast(0L)
                        _currentPositionMs.value = pos
                        if (dur > 0) {
                            _durationMs.value = dur
                            _progress.value = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                        }
                    }
                } catch (e: Exception) {
                    // Ignored on player state transitions
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun formatTimeMs(millis: Long): String {
        if (millis <= 0) return "00:00"
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
