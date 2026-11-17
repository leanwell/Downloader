package com.leanwell.downloader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leanwell.downloader.engine.YoutubeEngine
import com.leanwell.downloader.model.ActiveDownload
import com.leanwell.downloader.model.DownloadStatus
import com.leanwell.downloader.ui.components.AmbientGlowBackground
import com.leanwell.downloader.ui.components.GlassCard
import com.leanwell.downloader.ui.sheets.SupportedWebsitesBottomSheet
import com.leanwell.downloader.ui.theme.DigitalIrisSecondary
import com.leanwell.downloader.ui.theme.ObsidianPrimary
import com.leanwell.downloader.ui.theme.TextMuted
import com.leanwell.downloader.ui.theme.TextPrimary
import com.leanwell.downloader.ui.theme.TextSecondary
import com.leanwell.downloader.ui.theme.YouTubeRed
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAnalyzeUrl: (String) -> Unit,
    onCancelAnalyze: () -> Unit = {},
    onViewQueueClick: () -> Unit,
    activeDownloads: List<ActiveDownload>,
    isAnalyzing: Boolean = false,
    analyzeErrorTrigger: Long = 0L,
    isMiniPlayerVisible: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var urlText by remember { mutableStateOf("") }
    var detectedClipboardUrl by remember { mutableStateOf("") }
    var showSupportedWebsitesSheet by remember { mutableStateOf(false) }
    var localAnalyzeErrorTrigger by remember { mutableLongStateOf(0L) }
    val effectiveErrorTrigger = maxOf(analyzeErrorTrigger, localAnalyzeErrorTrigger)

    LaunchedEffect(Unit) {
        val clip = clipboardManager.getText()?.text?.toString()?.trim()
        if (!clip.isNullOrBlank() && (clip.startsWith("http://", ignoreCase = true) || clip.startsWith("https://", ignoreCase = true) || clip.contains("youtu") || clip.contains("facebook.com") || clip.contains("fb.watch") || clip.contains("tiktok.com") || clip.contains("instagram.com") || clip.contains("instagr.am") || clip.contains("twitter.com") || clip.contains("x.com"))) {
            detectedClipboardUrl = clip
        }
    }

    val isImeVisible = WindowInsets.isImeVisible

    val animatedBottomPadding by animateDpAsState(
        targetValue = if (isImeVisible) 16.dp else if (isMiniPlayerVisible) 166.dp else 96.dp,
        animationSpec = tween(durationMillis = 250),
        label = "homeBottomPadding"
    )

    AmbientGlowBackground(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = animatedBottomPadding)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isImeVisible,
                        enter = fadeIn(tween(250)) + slideInVertically(
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) { -it / 2 },
                        exit = fadeOut(tween(200)) + slideOutVertically(
                            animationSpec = tween(250, easing = FastOutSlowInEasing)
                        ) { -it / 2 }
                    ) {
                        InteractiveHeroLogo(
                            isAnalyzing = isAnalyzing,
                            activeDownloads = activeDownloads,
                            analyzeErrorTrigger = effectiveErrorTrigger,
                            onViewQueueClick = onViewQueueClick
                        )
                    }
                }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Grab Video or Music",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 20.sp
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, TextPrimary.copy(alpha = 0.75f), CircleShape)
                                    .clickable { showSupportedWebsitesSheet = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "?",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Paste a link to download audio or video directly to your device",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        OutlinedTextField(
                            value = urlText,
                            onValueChange = { urlText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    "Paste link here...",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Link,
                                    contentDescription = null,
                                    tint = DigitalIrisSecondary
                                )
                            },
                            trailingIcon = {
                                if (urlText.isNotEmpty()) {
                                    IconButton(onClick = { urlText = "" }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Clear,
                                            contentDescription = "Clear",
                                            tint = TextMuted
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                                focusedBorderColor = DigitalIrisSecondary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                                cursorColor = ObsidianPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text
                                    if (!clip.isNullOrBlank()) {
                                        urlText = clip
                                    }
                                },
                                enabled = !isAnalyzing,
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color.Black.copy(alpha = 0.08f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentPaste,
                                        contentDescription = null,
                                        tint = if (!isAnalyzing) TextPrimary else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Paste",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            color = if (!isAnalyzing) TextPrimary else TextMuted,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    if (isAnalyzing) {
                                        onCancelAnalyze()
                                    } else if (urlText.isNotBlank()) {
                                        val trimmed = urlText.trim()
                                        val isValid = trimmed.startsWith("http://", ignoreCase = true) ||
                                                trimmed.startsWith("https://", ignoreCase = true) ||
                                                trimmed.contains("youtu") ||
                                                trimmed.contains("facebook.com") ||
                                                trimmed.contains("fb.watch") ||
                                                trimmed.contains("tiktok.com") ||
                                                trimmed.contains("instagram.com") ||
                                                trimmed.contains("instagr.am") ||
                                                trimmed.contains("twitter.com") ||
                                                trimmed.contains("x.com") ||
                                                YoutubeEngine.isValidMediaUrl(trimmed)
                                        if (!isValid) {
                                            localAnalyzeErrorTrigger = System.currentTimeMillis()
                                            android.widget.Toast.makeText(context, "Please enter a valid video link.", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            onAnalyzeUrl(trimmed)
                                        }
                                    }
                                },
                                enabled = isAnalyzing || urlText.isNotBlank(),
                                shape = RoundedCornerShape(14.dp),
                                color = if (isAnalyzing) Color(0xFFDC2626) else ObsidianPrimary,
                                contentColor = Color.White,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isAnalyzing) {
                                        Text(
                                            text = "Cancel Analyze",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.Download,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Analyze Link",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        if (detectedClipboardUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(
                                color = Color.Black.copy(alpha = 0.06f),
                                thickness = 1.dp
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF1F4FA))
                                    .border(
                                        1.dp,
                                        Color.White.copy(alpha = 0.9f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        urlText = detectedClipboardUrl
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val platform = YoutubeEngine.detectPlatform(detectedClipboardUrl)
                                val isValidMedia = YoutubeEngine.isValidMediaUrl(detectedClipboardUrl)
                                val isPlaylist = YoutubeEngine.isPlaylistUrl(detectedClipboardUrl)

                                val clipIcon = if (isPlaylist) {
                                    Icons.Rounded.PlaylistPlay
                                } else {
                                    when (platform) {
                                        com.leanwell.downloader.model.Platform.YOUTUBE -> Icons.Rounded.PlayCircle
                                        com.leanwell.downloader.model.Platform.YOUTUBE_MUSIC -> Icons.Rounded.PlayCircle
                                        com.leanwell.downloader.model.Platform.SPOTIFY -> Icons.Rounded.MusicNote
                                        com.leanwell.downloader.model.Platform.FACEBOOK -> Icons.Rounded.Videocam
                                        com.leanwell.downloader.model.Platform.TIKTOK -> Icons.Rounded.MusicNote
                                        com.leanwell.downloader.model.Platform.INSTAGRAM -> Icons.Rounded.CameraAlt
                                        com.leanwell.downloader.model.Platform.TWITTER -> Icons.Rounded.Videocam
                                        com.leanwell.downloader.model.Platform.UNIVERSAL -> Icons.Rounded.Language
                                    }
                                }
                                val clipIconTint = when (platform) {
                                    com.leanwell.downloader.model.Platform.YOUTUBE -> YouTubeRed
                                    com.leanwell.downloader.model.Platform.YOUTUBE_MUSIC -> Color(0xFFFF0000)
                                    com.leanwell.downloader.model.Platform.SPOTIFY -> Color(0xFF1DB954)
                                    com.leanwell.downloader.model.Platform.FACEBOOK -> Color(0xFF1877F2)
                                    com.leanwell.downloader.model.Platform.TIKTOK -> Color(0xFF0F172A)
                                    com.leanwell.downloader.model.Platform.INSTAGRAM -> Color(0xFFE1306C)
                                    com.leanwell.downloader.model.Platform.TWITTER -> Color(0xFF0F172A)
                                    com.leanwell.downloader.model.Platform.UNIVERSAL -> DigitalIrisSecondary
                                }
                                val clipBgTint = when (platform) {
                                    com.leanwell.downloader.model.Platform.YOUTUBE -> YouTubeRed.copy(alpha = 0.12f)
                                    com.leanwell.downloader.model.Platform.YOUTUBE_MUSIC -> Color(0xFFFF0000).copy(alpha = 0.12f)
                                    com.leanwell.downloader.model.Platform.SPOTIFY -> Color(0xFF1DB954).copy(alpha = 0.12f)
                                    com.leanwell.downloader.model.Platform.FACEBOOK -> Color(0xFF1877F2).copy(alpha = 0.12f)
                                    com.leanwell.downloader.model.Platform.TIKTOK -> Color(0xFF0F172A).copy(alpha = 0.08f)
                                    com.leanwell.downloader.model.Platform.INSTAGRAM -> Color(0xFFE1306C).copy(alpha = 0.12f)
                                    com.leanwell.downloader.model.Platform.TWITTER -> Color(0xFF0F172A).copy(alpha = 0.08f)
                                    com.leanwell.downloader.model.Platform.UNIVERSAL -> DigitalIrisSecondary.copy(alpha = 0.12f)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(clipBgTint),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = clipIcon,
                                        contentDescription = null,
                                        tint = clipIconTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isPlaylist) "YouTube Playlist in clipboard" else "Link in clipboard",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    )
                                    Text(
                                        text = detectedClipboardUrl,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEEF2FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentPaste,
                                        contentDescription = "Paste to URL bar",
                                        tint = DigitalIrisSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showSupportedWebsitesSheet) {
                SupportedWebsitesBottomSheet(
                    onDismissRequest = { showSupportedWebsitesSheet = false }
                )
            }
        }
    }
}

@Composable
fun InteractiveHeroLogo(
    isAnalyzing: Boolean,
    activeDownloads: List<ActiveDownload>,
    analyzeErrorTrigger: Long = 0L,
    onViewQueueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val downloadingItems = remember(activeDownloads) {
        activeDownloads.filter {
            it.status == DownloadStatus.DOWNLOADING ||
            it.status == DownloadStatus.CONVERTING ||
            it.status == DownloadStatus.QUEUED
        }
    }
    val isDownloading = downloadingItems.isNotEmpty()

    val errorItems = remember(activeDownloads) {
        activeDownloads.filter { it.status == DownloadStatus.ERROR }
    }
    val hasError = errorItems.isNotEmpty()

    var celebrationUntil by remember { mutableLongStateOf(0L) }
    var errorUntil by remember { mutableLongStateOf(0L) }

    LaunchedEffect(activeDownloads) {
        if (activeDownloads.any { it.status == DownloadStatus.COMPLETED }) {
            celebrationUntil = System.currentTimeMillis() + 3500L
        }
        if (hasError) {
            errorUntil = System.currentTimeMillis() + 5000L
        }
    }

    LaunchedEffect(analyzeErrorTrigger) {
        if (analyzeErrorTrigger > 0L) {
            errorUntil = maxOf(errorUntil, System.currentTimeMillis() + 4500L)
        }
    }

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(celebrationUntil, errorUntil) {
        while (System.currentTimeMillis() < celebrationUntil || System.currentTimeMillis() < errorUntil) {
            currentTime = System.currentTimeMillis()
            delay(200L)
        }
        currentTime = System.currentTimeMillis()
    }

    val isCelebrationActive = currentTime < celebrationUntil
    val isErrorActive = hasError || (currentTime < errorUntil)

    val YellowIndicator = Color(0xFFF59E0B)
    val GreenIndicator = Color(0xFF10B981)
    val RedIndicator = Color(0xFFEF4444)
    val IrisIndicator = DigitalIrisSecondary
    val IdleGrayishRipple = Color(0xFF94A3B8)

    val activeStateColor = when {
        isErrorActive -> RedIndicator
        isCelebrationActive -> GreenIndicator
        isDownloading -> YellowIndicator
        isAnalyzing -> IrisIndicator
        else -> null
    }

    val isWaveActive = activeStateColor != null
    val targetWaveColor = activeStateColor ?: IdleGrayishRipple
    val animatedWaveColor by animateColorAsState(
        targetValue = targetWaveColor,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "waveColor"
    )

    val targetWaveIntensity by animateFloatAsState(
        targetValue = if (isWaveActive) 1.0f else 0.22f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "waveIntensity"
    )

    val waveBgAlpha1 = 0.28f * targetWaveIntensity
    val waveBorderAlpha1 = 0.55f * targetWaveIntensity
    val waveBgAlpha2 = 0.18f * targetWaveIntensity
    val waveBorderAlpha2 = 0.40f * targetWaveIntensity

    val infiniteTransition = rememberInfiniteTransition(label = "heroWaveSplash")

    val wave1Scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1Scale"
    )

    val wave1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1Alpha"
    )

    val wave2Scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, delayMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2Scale"
    )

    val wave2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, delayMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2Alpha"
    )

    var bounceTrigger by remember { mutableStateOf(false) }
    val bounceScale by animateFloatAsState(
        targetValue = if (bounceTrigger) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        finishedListener = {
            if (bounceTrigger) bounceTrigger = false
        },
        label = "bounceScale"
    )

    val avgProgress = if (isDownloading) {
        downloadingItems.map { it.progress }.average().toFloat().coerceIn(0.05f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = avgProgress,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "logoProgress"
    )

    val progressRingAlpha by animateFloatAsState(
        targetValue = if (isWaveActive) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "progressRingAlpha"
    )

    val animatedIconTint by animateColorAsState(
        targetValue = activeStateColor ?: TextPrimary,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "iconTint"
    )

    val outerBg = Color.White.copy(alpha = 0.72f)
    val outerBorder = Color.White.copy(alpha = 0.85f)
    val animatedDiscBorder by animateColorAsState(
        targetValue = activeStateColor?.copy(alpha = 0.65f) ?: outerBorder,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "discBorder"
    )

    Box(
        modifier = modifier
            .size(150.dp)
            .graphicsLayer {
                scaleX = bounceScale
                scaleY = bounceScale
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .graphicsLayer {
                    scaleX = wave1Scale
                    scaleY = wave1Scale
                    alpha = wave1Alpha
                }
                .clip(CircleShape)
                .background(animatedWaveColor.copy(alpha = waveBgAlpha1))
                .border(2.dp, animatedWaveColor.copy(alpha = waveBorderAlpha1), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(112.dp)
                .graphicsLayer {
                    scaleX = wave2Scale
                    scaleY = wave2Scale
                    alpha = wave2Alpha
                }
                .clip(CircleShape)
                .background(animatedWaveColor.copy(alpha = waveBgAlpha2))
                .border(1.5.dp, animatedWaveColor.copy(alpha = waveBorderAlpha2), CircleShape)
        )

        Box(
            modifier = Modifier
                .size(112.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = activeStateColor?.copy(alpha = 0.25f) ?: Color(0x1F4854BB),
                    spotColor = activeStateColor?.copy(alpha = 0.25f) ?: Color(0x1F4854BB)
                )
                .clip(CircleShape)
                .background(outerBg)
                .border(
                    width = 1.5.dp,
                    color = animatedDiscBorder,
                    shape = CircleShape
                )
                .clickable {
                    if (isDownloading || isCelebrationActive || isErrorActive || isAnalyzing) {
                        onViewQueueClick()
                    } else {
                        bounceTrigger = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (progressRingAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .graphicsLayer { alpha = progressRingAlpha }
                ) {
                    when {
                        isAnalyzing -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(112.dp),
                                color = IrisIndicator,
                                strokeWidth = 3.5.dp,
                                trackColor = Color.Transparent
                            )
                        }
                        isDownloading -> {
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.size(112.dp),
                                color = YellowIndicator,
                                strokeWidth = 3.5.dp,
                                trackColor = YellowIndicator.copy(alpha = 0.15f)
                            )
                        }
                        isCelebrationActive -> {
                            CircularProgressIndicator(
                                progress = { 1.0f },
                                modifier = Modifier.size(112.dp),
                                color = GreenIndicator,
                                strokeWidth = 3.5.dp,
                                trackColor = GreenIndicator.copy(alpha = 0.2f)
                            )
                        }
                        isErrorActive -> {
                            CircularProgressIndicator(
                                progress = { 1.0f },
                                modifier = Modifier.size(112.dp),
                                color = RedIndicator,
                                strokeWidth = 3.5.dp,
                                trackColor = RedIndicator.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }

            val innerBrush = Brush.verticalGradient(
                listOf(Color.White, Color(0xFFEEF4FF))
            )

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(innerBrush)
                    .border(1.dp, Color.White.copy(alpha = 0.9f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = "Downloader Logo",
                    tint = animatedIconTint,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        if (downloadingItems.size > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = 8.dp)
                    .sizeIn(minWidth = 22.dp, minHeight = 22.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(ObsidianPrimary)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (downloadingItems.size > 9) "9+" else "${downloadingItems.size}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.padding(horizontal = 5.dp)
                )
            }
        }
    }
}
