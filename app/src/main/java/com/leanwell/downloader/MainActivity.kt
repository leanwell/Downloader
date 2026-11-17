package com.leanwell.downloader

import android.Manifest
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.Rational
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.media3.ui.PlayerView
import com.leanwell.downloader.engine.ExtractedPlaylistInfo
import com.leanwell.downloader.engine.ExtractedVideoInfo
import com.leanwell.downloader.engine.PlaylistItemInfo
import com.leanwell.downloader.engine.PlaylistQualityPreset
import com.leanwell.downloader.engine.YoutubeEngine
import com.leanwell.downloader.model.ActiveDownload
import com.leanwell.downloader.model.DownloadStatus
import com.leanwell.downloader.model.MediaItem
import com.leanwell.downloader.model.MediaType
import com.leanwell.downloader.model.NavDestination
import com.leanwell.downloader.model.Platform
import com.leanwell.downloader.player.AppPlayerManager
import com.leanwell.downloader.service.DownloadQueueManager
import com.leanwell.downloader.service.QueuedDownloadTask
import com.leanwell.downloader.settings.AppSettings
import com.leanwell.downloader.ui.components.FrostedBottomNavBar
import com.leanwell.downloader.ui.components.FrostedMiniPlayerBar
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.leanwell.downloader.ui.screens.HomeScreen
import com.leanwell.downloader.ui.screens.LibraryScreen
import com.leanwell.downloader.ui.screens.OnboardingScreen
import com.leanwell.downloader.ui.screens.QueueScreen
import com.leanwell.downloader.ui.screens.SettingsScreen
import com.leanwell.downloader.ui.sheets.AboutLegalBottomSheet
import com.leanwell.downloader.ui.sheets.ExpandedPlayerBottomSheet
import com.leanwell.downloader.ui.sheets.FileActionsBottomSheet
import com.leanwell.downloader.ui.sheets.FormatSelectionBottomSheet
import com.leanwell.downloader.ui.sheets.PlaylistBatchSelectionBottomSheet
import com.leanwell.downloader.ui.theme.DownloaderTheme
import com.leanwell.downloader.utils.MediaScannerUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val isInPipModeState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            DownloaderTheme {
                MainApp(
                    isInPipMode = isInPipModeState.value,
                    onEnterPip = { enterPip() }
                )
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeState.value = isInPictureInPictureMode
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (AppSettings.isPipEnabled.value &&
            AppPlayerManager.isPlaying.value &&
            AppPlayerManager.currentTrack.value?.mediaType == MediaType.VIDEO) {
            enterPip()
        }
    }

    fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                Log.w("MainActivity", "Failed to enter PiP: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppPlayerManager.stop()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainApp(
    isInPipMode: Boolean = false,
    onEnterPip: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var hasSeenOnboarding by remember { mutableStateOf(AppSettings.isOnboardingCompleted(context)) }
    var selectedDestination by remember { mutableStateOf(NavDestination.HOME) }

    // Active downloads state observed directly from DownloadQueueManager
    val activeDownloads by DownloadQueueManager.downloads.collectAsState()

    // Library media items state
    var mediaItems by remember { mutableStateOf(emptyList<MediaItem>()) }

    // Load existing downloaded files from disk on launch (offloaded to IO dispatcher)
    LaunchedEffect(Unit) {
        val existingFiles = withContext(Dispatchers.IO) {
            MediaScannerUtils.loadDownloadedMediaItems(context)
        }
        mediaItems = existingFiles
    }

    // Collect newly completed media items into Library
    LaunchedEffect(Unit) {
        DownloadQueueManager.completedMediaItems.collect { newItem ->
            mediaItems = listOf(newItem) + mediaItems.filterNot { it.filePath == newItem.filePath }
        }
    }

    // Notification Permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Media Player State from AppPlayerManager
    val currentlyPlayingItem by AppPlayerManager.currentTrack.collectAsState()
    val isPlaying by AppPlayerManager.isPlaying.collectAsState()
    val playerProgress by AppPlayerManager.progress.collectAsState()
    val currentPositionMs by AppPlayerManager.currentPositionMs.collectAsState()
    val durationMs by AppPlayerManager.durationMs.collectAsState()
    val autoSwitchToQueue by AppSettings.autoSwitchToQueue.collectAsState()

    // Modal Bottom Sheets state
    var showFormatSheet by remember { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }
    var pendingVideoUrl by remember { mutableStateOf("") }
    var isAnalyzingUrl by remember { mutableStateOf(false) }
    var analyzeJob by remember { mutableStateOf<Job?>(null) }
    var lastQueuedTime by remember { mutableLongStateOf(0L) }
    var extractedVideoInfo by remember { mutableStateOf<ExtractedVideoInfo?>(null) }
    var extractedPlaylistInfo by remember { mutableStateOf<ExtractedPlaylistInfo?>(null) }
    var analyzeErrorTrigger by remember { mutableLongStateOf(0L) }

    var showExpandedPlayerSheet by remember { mutableStateOf(false) }
    var selectedMediaForActions by remember { mutableStateOf<MediaItem?>(null) }
    var showAboutLegalSheet by remember { mutableStateOf(false) }
    var aboutLegalInitialTab by remember { mutableIntStateOf(0) }

    // Keep screen on when expanded playback sheet is visible and actively playing
    val activity = context as? Activity
    DisposableEffect(showExpandedPlayerSheet, isPlaying) {
        if (showExpandedPlayerSheet && isPlaying) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    if (isInPipMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = AppPlayerManager.player
                        useController = false
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                update = { pv ->
                    if (pv.player != AppPlayerManager.player) {
                        pv.player = AppPlayerManager.player
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    val hazeState = remember { HazeState() }

    if (!hasSeenOnboarding) {
        OnboardingScreen(
            onGetStartedClick = {
                AppSettings.setOnboardingCompleted(context, true)
                hasSeenOnboarding = true
            },
            onTermsClick = {
                aboutLegalInitialTab = 1
                showAboutLegalSheet = true
            },
            onPrivacyPolicyClick = {
                aboutLegalInitialTab = 1
                showAboutLegalSheet = true
            }
        )
        if (showAboutLegalSheet) {
            AboutLegalBottomSheet(
                initialTab = aboutLegalInitialTab,
                onDismissRequest = { showAboutLegalSheet = false }
            )
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main Content Area (Covered by hazeSource for live hardware backdrop blur sampling)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
            ) {
                Crossfade(
                    targetState = selectedDestination,
                    label = "ScreenNavigation"
                ) { destination ->
                when (destination) {
                    NavDestination.HOME -> HomeScreen(
                        onAnalyzeUrl = { url ->
                            analyzeJob?.cancel()
                            isAnalyzingUrl = true
                            pendingVideoUrl = url
                            analyzeJob = coroutineScope.launch {
                                val cookiesFile = com.leanwell.downloader.settings.AppSettings.getCookiesForUrl(context, url)
                                if (YoutubeEngine.isPlaylistUrl(url)) {
                                    val playlistResult = YoutubeEngine.extractPlaylistMetadata(url, cookiesFile)
                                    isAnalyzingUrl = false
                                    playlistResult.onSuccess { playlist ->
                                        extractedPlaylistInfo = playlist
                                        showPlaylistSheet = true
                                    }.onFailure { error ->
                                        analyzeErrorTrigger = System.currentTimeMillis()
                                        Toast.makeText(
                                            context,
                                            "Error reading playlist: ${error.localizedMessage ?: "Invalid Playlist URL"}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } else {
                                    val result = YoutubeEngine.extractMetadata(url, cookiesFile)
                                    isAnalyzingUrl = false
                                    result.onSuccess { info ->
                                        extractedVideoInfo = info
                                        showFormatSheet = true
                                    }.onFailure { error ->
                                        analyzeErrorTrigger = System.currentTimeMillis()
                                        Toast.makeText(
                                            context,
                                            "Error fetching video details: ${error.localizedMessage ?: "Invalid URL"}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        },
                        onCancelAnalyze = {
                            analyzeJob?.cancel()
                            isAnalyzingUrl = false
                            Toast.makeText(context, "Analysis cancelled", Toast.LENGTH_SHORT).show()
                        },
                        onViewQueueClick = {
                            selectedDestination = NavDestination.QUEUE
                        },
                        activeDownloads = activeDownloads,
                        isAnalyzing = isAnalyzingUrl,
                        analyzeErrorTrigger = analyzeErrorTrigger,
                        isMiniPlayerVisible = currentlyPlayingItem != null
                    )

                    NavDestination.QUEUE -> QueueScreen(
                        downloads = activeDownloads,
                        onPauseClick = { id -> DownloadQueueManager.pauseDownload(id) },
                        onResumeClick = { id -> DownloadQueueManager.resumeDownload(id) },
                        onCancelClick = { id ->
                            DownloadQueueManager.cancelDownload(id)
                            Toast.makeText(context, "Download cancelled", Toast.LENGTH_SHORT).show()
                        },
                        onPauseAllClick = {
                            DownloadQueueManager.pauseAll()
                            Toast.makeText(context, "Paused all downloads", Toast.LENGTH_SHORT).show()
                        },
                        onCancelAllClick = {
                            DownloadQueueManager.cancelAll()
                            Toast.makeText(context, "Cancelled all downloads", Toast.LENGTH_SHORT).show()
                        },
                        onNavigateToSettings = {
                            selectedDestination = NavDestination.SETTINGS
                        },
                        isMiniPlayerVisible = currentlyPlayingItem != null
                    )

                    NavDestination.LIBRARY -> LibraryScreen(
                        mediaItems = mediaItems,
                        onItemClick = { item ->
                            AppPlayerManager.play(item)
                            if (item.mediaType == MediaType.VIDEO) {
                                showExpandedPlayerSheet = true
                            }
                        },
                        onItemMenuClick = { item ->
                            selectedMediaForActions = item
                        },
                        onDeleteItems = { itemsToDelete ->
                            val baseDir = YoutubeEngine.getPublicDownloadDir()
                            itemsToDelete.forEach { item ->
                                val path = item.filePath
                                if (path != null) {
                                    try {
                                        val file = File(path)
                                        val parent = file.parentFile
                                        file.delete()
                                        if (parent != null && parent.exists() && parent.absolutePath != baseDir.absolutePath) {
                                            val remaining = parent.listFiles()
                                            if (remaining != null && remaining.isEmpty()) {
                                                parent.delete()
                                            }
                                        }
                                    } catch (e: Exception) { /* ignore */ }
                                }
                                if (currentlyPlayingItem?.id == item.id) {
                                    AppPlayerManager.stop()
                                }
                            }
                            val deletedIds = itemsToDelete.map { it.id }.toSet()
                            mediaItems = mediaItems.filterNot { it.id in deletedIds }
                            Toast.makeText(context, "${itemsToDelete.size} file(s) deleted", Toast.LENGTH_SHORT).show()
                        },
                        isMiniPlayerVisible = currentlyPlayingItem != null
                    )

                    NavDestination.SETTINGS -> SettingsScreen(
                        onLicensesClick = {
                            aboutLegalInitialTab = 0
                            showAboutLegalSheet = true
                        },
                        onTermsClick = {
                            aboutLegalInitialTab = 1
                            showAboutLegalSheet = true
                        },
                        onRevisitOnboardingClick = {
                            hasSeenOnboarding = false
                        }
                    )
                }
            }
            }

            // Bottom Navigation & Mini Player Dock (Hidden when keyboard is showing, slides down/up)
            AnimatedVisibility(
                visible = !WindowInsets.isImeVisible,
                enter = fadeIn(tween(250)) + slideInVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) { it },
                exit = fadeOut(tween(200)) + slideOutVertically(
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                ) { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Persistent Mini-Player Bar with Immediate Slide-Up Animation on Bottom Sheet Dismissal
                    AnimatedVisibility(
                        visible = currentlyPlayingItem != null && !showExpandedPlayerSheet,
                        enter = slideInVertically(
                            initialOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(durationMillis = 80)),
                        exit = slideOutVertically(
                            targetOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(durationMillis = 80))
                    ) {
                        if (currentlyPlayingItem != null) {
                            FrostedMiniPlayerBar(
                                mediaItem = currentlyPlayingItem!!,
                                isPlaying = isPlaying,
                                progress = playerProgress,
                                onPlayPauseToggle = { AppPlayerManager.togglePlayPause() },
                                onSkipNext = {
                                    if (mediaItems.isNotEmpty() && currentlyPlayingItem != null) {
                                        val curIdx = mediaItems.indexOfFirst { it.id == currentlyPlayingItem?.id }
                                        val nextIdx = if (curIdx >= 0) (curIdx + 1) % mediaItems.size else 0
                                        AppPlayerManager.play(mediaItems[nextIdx])
                                    }
                                },
                                onDismiss = { AppPlayerManager.stop() },
                                onClick = { showExpandedPlayerSheet = true },
                                hazeState = hazeState
                            )
                        }
                    }

                    // Docked 4-Tab Bottom Navigation Bar with real frosted glass blur
                    FrostedBottomNavBar(
                        selectedDestination = selectedDestination,
                        onDestinationSelected = { selectedDestination = it },
                        queueBadgeCount = activeDownloads.count {
                            it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.CONVERTING
                        },
                        hazeState = hazeState
                    )
                }
            }

            // Signature Modal Bottom Sheet 1: Format & Quality Selector
            if (showFormatSheet) {
                val info = extractedVideoInfo
                val currentPlatform = YoutubeEngine.detectPlatform(pendingVideoUrl)
                val defaultTitle = when (currentPlatform) {
                    Platform.FACEBOOK -> "Facebook Video"
                    Platform.TIKTOK -> "TikTok Video"
                    Platform.INSTAGRAM -> "Instagram Reel"
                    Platform.TWITTER -> "Twitter / X Video"
                    Platform.YOUTUBE -> "YouTube Media"
                    Platform.YOUTUBE_MUSIC -> "YouTube Music Track"
                    Platform.SPOTIFY -> "Spotify Track"
                    Platform.UNIVERSAL -> "Web Media"
                }
                val defaultChannel = info?.customPlatformName ?: currentPlatform.displayName

                FormatSelectionBottomSheet(
                    videoTitle = info?.title ?: if (pendingVideoUrl.isNotBlank()) pendingVideoUrl else defaultTitle,
                    channelName = info?.author ?: defaultChannel,
                    duration = info?.durationText ?: "00:00",
                    thumbnailUrl = info?.thumbnailUrl,
                    customAudioFormats = info?.audioFormats,
                    customVideoFormats = info?.videoFormats,
                    initialAutoEmbedMetadata = com.leanwell.downloader.settings.AppSettings.autoEmbedMetadata.value,
                    platform = currentPlatform,
                    onDismissRequest = { showFormatSheet = false },
                    onScanHighRes = {
                        if (currentPlatform != Platform.YOUTUBE) {
                            null
                        } else {
                            val cookiesFile = com.leanwell.downloader.settings.AppSettings.getCookiesFile(context)
                            val scanRes = YoutubeEngine.scanHighResolutions(
                                url = pendingVideoUrl,
                                durationSeconds = info?.durationSeconds ?: 210L,
                                cookiesFile = cookiesFile
                            )
                            scanRes.getOrNull()
                        }
                    },
                    onConfirmDownload = { format, autoEmbedMetadata ->
                        val now = System.currentTimeMillis()
                        if (now - lastQueuedTime < 1000L) {
                            return@FormatSelectionBottomSheet
                        }
                        lastQueuedTime = now
                        showFormatSheet = false
                        val downloadId = UUID.randomUUID().toString()
                        val targetDir = YoutubeEngine.getPublicDownloadDir()
                        val rawTitle = info?.title ?: "Download_${System.currentTimeMillis()}"
                        val targetExt = when (format.id) {
                            "mp3_320", "mp3_192" -> "mp3"
                            "m4a_aac" -> "m4a"
                            else -> if (format.mediaType == MediaType.AUDIO) "mp3" else "mp4"
                        }

                        val qualityTag = when (format.id) {
                            "tiktok_hd" -> "[HD]"
                            "ig_best" -> "[Best]"
                            "tw_best" -> "[HD]"
                            "tw_sd" -> "[SD]"
                            "fb_hd" -> "[HD]"
                            "fb_sd" -> "[SD]"
                            "video_4320" -> "[8K]"
                            "video_2160" -> "[4K]"
                            "video_1440" -> "[1440p]"
                            "video_1080" -> "[1080p]"
                            "video_720" -> "[720p]"
                            "video_480" -> "[480p]"
                            "video_360" -> "[360p]"
                            "video_240" -> "[240p]"
                            "video_144" -> "[144p]"
                            "mp3_320" -> "[320k]"
                            "mp3_192" -> "[192k]"
                            "m4a_aac" -> "[M4A]"
                            else -> if (format.mediaType == MediaType.VIDEO) "[${format.label.substringBefore(" ")}]" else "[Audio]"
                        }
                        val titleWithQuality = "$rawTitle $qualityTag"
                        val reservedTitles = activeDownloads.map { it.title }.toSet()
                        val uniqueTitle = YoutubeEngine.getUniqueTitle(targetDir, titleWithQuality, targetExt, reservedTitles)
                        val cookiesFile = com.leanwell.downloader.settings.AppSettings.getCookiesForUrl(context, pendingVideoUrl)
                        val downloadUrl = if (currentPlatform == Platform.SPOTIFY && !info?.url.isNullOrBlank()) {
                            info.url
                        } else {
                            pendingVideoUrl
                        }

                        val dlRequest = YoutubeEngine.buildDownloadRequest(
                            url = downloadUrl,
                            format = format,
                            outputDir = targetDir,
                            title = uniqueTitle,
                            useAria2c = false,
                            embedMetadata = autoEmbedMetadata,
                            cookiesFile = cookiesFile
                        )

                        val cleanFormatLabel = when (format.id) {
                            "tiktok_hd" -> "HD"
                            "ig_best" -> "Best"
                            "tw_best" -> "HD"
                            "tw_sd" -> "SD"
                            "fb_hd" -> "HD"
                            "fb_sd" -> "SD"
                            "video_4320" -> "8K"
                            "video_2160" -> "4K"
                            "video_1440" -> "1440p"
                            "video_1080" -> "1080p"
                            "video_720" -> "720p"
                            "video_480" -> "480p"
                            "video_360" -> "360p"
                            "video_240" -> "240p"
                            "video_144" -> "144p"
                            "mp3_320" -> "MP3 320k"
                            "mp3_192" -> "MP3 192k"
                            "m4a_aac" -> "M4A"
                            else -> format.label.substringBefore(" (")
                        }

                        val newDownload = ActiveDownload(
                            id = downloadId,
                            title = uniqueTitle,
                            platform = currentPlatform,
                            mediaType = format.mediaType,
                            formatLabel = cleanFormatLabel,
                            qualityLabel = format.estimatedSize,
                            progress = 0.0f,
                            speed = "",
                            eta = "Pending",
                            downloadedBytes = "0 MB",
                            totalBytes = format.estimatedSize,
                            status = DownloadStatus.QUEUED,
                            statusMessage = "Queued",
                            customPlatformName = info?.customPlatformName
                        )

                        DownloadQueueManager.enqueue(
                            QueuedDownloadTask(
                                download = newDownload,
                                request = dlRequest,
                                targetDirectory = targetDir,
                                authorOrChannel = info?.author ?: "YouTube Creator",
                                durationText = info?.durationText ?: "00:00"
                            )
                        )

                        if (autoSwitchToQueue) {
                            selectedDestination = NavDestination.QUEUE
                        }
                        Toast.makeText(context, "Queued: ${format.label}", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Signature Modal Bottom Sheet 1B: YouTube Playlist Batch Selection
            if (showPlaylistSheet && extractedPlaylistInfo != null) {
                val playlist = extractedPlaylistInfo!!
                PlaylistBatchSelectionBottomSheet(
                    playlistInfo = playlist,
                    onDismissRequest = {
                        showPlaylistSheet = false
                        extractedPlaylistInfo = null
                    },
                    onConfirmBatchDownload = { selectedItems, preset, folderName ->
                        val now = System.currentTimeMillis()
                        if (now - lastQueuedTime < 1000L) {
                            return@PlaylistBatchSelectionBottomSheet
                        }
                        lastQueuedTime = now
                        showPlaylistSheet = false
                        val formatOpt = preset.toFormatOption()
                        val cookiesFile = AppSettings.getCookiesForUrl(context, pendingVideoUrl)
                        val baseDir = YoutubeEngine.getPublicDownloadDir() // Downloads/Downloader
                        val sanitizedFolder = YoutubeEngine.sanitizeFilename(folderName.ifBlank { playlist.title })
                        val targetDir = File(baseDir, sanitizedFolder).apply { if (!exists()) mkdirs() }

                        selectedItems.forEach { item ->
                            val dlRequest = YoutubeEngine.buildDownloadRequest(
                                url = item.url,
                                format = formatOpt,
                                outputDir = targetDir,
                                title = item.title,
                                embedMetadata = AppSettings.autoEmbedMetadata.value,
                                cookiesFile = cookiesFile
                            )

                            val cleanFormatLabel = when (preset) {
                                com.leanwell.downloader.engine.PlaylistQualityPreset.AUDIO_320K -> "MP3 320k"
                                com.leanwell.downloader.engine.PlaylistQualityPreset.AUDIO_192K -> "MP3 192k"
                                com.leanwell.downloader.engine.PlaylistQualityPreset.VIDEO_1080P -> "1080p"
                                com.leanwell.downloader.engine.PlaylistQualityPreset.VIDEO_720P -> "720p"
                                com.leanwell.downloader.engine.PlaylistQualityPreset.VIDEO_480P -> "480p"
                            }
                            val cleanQualityLabel = if (preset.mediaType == MediaType.AUDIO) "Audio" else "Video"

                            val playlistPlatform = YoutubeEngine.detectPlatform(pendingVideoUrl)
                            val newDownload = ActiveDownload(
                                id = UUID.randomUUID().toString(),
                                title = item.title,
                                platform = playlistPlatform,
                                mediaType = preset.mediaType,
                                formatLabel = cleanFormatLabel,
                                qualityLabel = cleanQualityLabel,
                                progress = 0.0f,
                                speed = "",
                                eta = "Pending",
                                downloadedBytes = "0 MB",
                                totalBytes = formatOpt.estimatedSize,
                                status = DownloadStatus.QUEUED,
                                statusMessage = "Queued from $sanitizedFolder",
                                customPlatformName = if (playlistPlatform == Platform.SPOTIFY) "Spotify" else null
                            )

                            DownloadQueueManager.enqueue(
                                QueuedDownloadTask(
                                    download = newDownload,
                                    request = dlRequest,
                                    targetDirectory = targetDir,
                                    authorOrChannel = item.author,
                                    durationText = item.durationText
                                )
                            )
                        }

                        if (autoSwitchToQueue) {
                            selectedDestination = NavDestination.QUEUE
                        }
                        Toast.makeText(
                            context,
                            "Queued ${selectedItems.size} items from ${playlist.title}",
                            Toast.LENGTH_SHORT
                        ).show()
                        extractedPlaylistInfo = null
                    }
                )
            }

            // Signature Modal Bottom Sheet 2: Expanded Media Player
            if (showExpandedPlayerSheet && currentlyPlayingItem != null) {
                ExpandedPlayerBottomSheet(
                    mediaItem = currentlyPlayingItem!!,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    progress = playerProgress,
                    onPlayPauseToggle = { AppPlayerManager.togglePlayPause() },
                    onSeek = { ratio -> AppPlayerManager.seekTo(ratio) },
                    onSkipNext = {
                        if (mediaItems.isNotEmpty() && currentlyPlayingItem != null) {
                            val curIdx = mediaItems.indexOfFirst { it.id == currentlyPlayingItem?.id }
                            val nextIdx = if (curIdx >= 0) (curIdx + 1) % mediaItems.size else 0
                            AppPlayerManager.play(mediaItems[nextIdx])
                        }
                    },
                    onSkipPrevious = {
                        if (mediaItems.isNotEmpty() && currentlyPlayingItem != null) {
                            val curIdx = mediaItems.indexOfFirst { it.id == currentlyPlayingItem?.id }
                            val prevIdx = if (curIdx > 0) curIdx - 1 else mediaItems.size - 1
                            AppPlayerManager.play(mediaItems[prevIdx])
                        }
                    },
                    onEnterPip = onEnterPip,
                    onOpenInExternalApp = {
                        val item = currentlyPlayingItem
                        if (item?.filePath != null) {
                            val file = File(item.filePath)
                            if (file.exists()) {
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val mime = if (item.mediaType == MediaType.AUDIO) "audio/*" else "video/*"
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, mime)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Open with"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open external player: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    onDismissRequest = { showExpandedPlayerSheet = false }
                )
            }

            // Signature Modal Bottom Sheet 3: File Actions Menu
            if (selectedMediaForActions != null) {
                FileActionsBottomSheet(
                    mediaItem = selectedMediaForActions!!,
                    onPlay = {
                        AppPlayerManager.play(selectedMediaForActions!!)
                        if (selectedMediaForActions!!.mediaType == MediaType.VIDEO) {
                            showExpandedPlayerSheet = true
                        }
                    },
                    onOpenExternal = {
                        val path = selectedMediaForActions!!.filePath
                        if (path != null && File(path).exists()) {
                            try {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    File(path)
                                )
                                val mime = if (selectedMediaForActions!!.mediaType == MediaType.AUDIO) "audio/*" else "video/*"
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, mime)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Open with"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onShare = {
                        val path = selectedMediaForActions!!.filePath
                        if (path != null && File(path).exists()) {
                            try {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    File(path)
                                )
                                val mime = if (selectedMediaForActions!!.mediaType == MediaType.AUDIO) "audio/*" else "video/*"
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = mime
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share media file"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot share: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onShowInFiles = {
                        val path = selectedMediaForActions!!.filePath
                        if (path != null && File(path).exists()) {
                            try {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    File(path)
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "*/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Open file"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Location: $path", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "File does not exist on disk", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDelete = {
                        val path = selectedMediaForActions!!.filePath
                        if (path != null) {
                            try {
                                val file = File(path)
                                val parent = file.parentFile
                                val baseDir = YoutubeEngine.getPublicDownloadDir()
                                file.delete()
                                if (parent != null && parent.exists() && parent.absolutePath != baseDir.absolutePath) {
                                    val remaining = parent.listFiles()
                                    if (remaining != null && remaining.isEmpty()) {
                                        parent.delete()
                                    }
                                }
                            } catch (e: Exception) { /* ignore */ }
                        }
                        if (currentlyPlayingItem?.id == selectedMediaForActions!!.id) {
                            AppPlayerManager.stop()
                        }
                        mediaItems = mediaItems.filterNot { it.id == selectedMediaForActions!!.id }
                        Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show()
                    },
                    onDismissRequest = { selectedMediaForActions = null }
                )
            }

            // Signature Modal Bottom Sheet 4: About & Legal
            if (showAboutLegalSheet) {
                AboutLegalBottomSheet(
                    initialTab = aboutLegalInitialTab,
                    onDismissRequest = { showAboutLegalSheet = false }
                )
            }
        }
    }
}
