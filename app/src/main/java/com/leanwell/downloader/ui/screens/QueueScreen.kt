package com.leanwell.downloader.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leanwell.downloader.model.ActiveDownload
import com.leanwell.downloader.model.DownloadStatus
import com.leanwell.downloader.model.Platform
import com.leanwell.downloader.ui.components.AmbientGlowBackground
import com.leanwell.downloader.ui.components.GlassCard
import com.leanwell.downloader.ui.dialogs.DownloadErrorDetailsDialog
import com.leanwell.downloader.ui.theme.DigitalIrisSecondary
import com.leanwell.downloader.ui.theme.ObsidianPrimary
import com.leanwell.downloader.ui.theme.PrismaticMintTertiary
import com.leanwell.downloader.ui.theme.TextMuted
import com.leanwell.downloader.ui.theme.TextPrimary
import com.leanwell.downloader.ui.theme.TextSecondary
import com.leanwell.downloader.ui.theme.YouTubeRed
import com.leanwell.downloader.utils.DownloadErrorHelper
import com.leanwell.downloader.utils.ErrorNotice

@Composable
fun QueueScreen(
    downloads: List<ActiveDownload>,
    onPauseClick: (String) -> Unit,
    onResumeClick: (String) -> Unit,
    onCancelClick: (String) -> Unit,
    onPauseAllClick: () -> Unit,
    onCancelAllClick: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    isMiniPlayerVisible: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedErrorItem by remember { mutableStateOf<Pair<ActiveDownload, ErrorNotice>?>(null) }

    val freeStorageText = androidx.compose.runtime.remember {
        try {
            val downloadDir = try {
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            } catch (e: Exception) {
                android.os.Environment.getDataDirectory()
            }
            val stat = android.os.StatFs(downloadDir.path)
            val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
            val gigabytes = bytesAvailable / (1024.0 * 1024.0 * 1024.0)
            String.format(java.util.Locale.US, "%.1f GB Free", gigabytes)
        } catch (e: Exception) {
            "Storage Ready"
        }
    }

    val activeDownloadingItem = downloads.firstOrNull { it.status == DownloadStatus.DOWNLOADING }
    val speedSummaryText = when {
        activeDownloadingItem == null -> "Idle"
        activeDownloadingItem.speed.isNotBlank() && activeDownloadingItem.speed.contains("B/s", ignoreCase = true) -> activeDownloadingItem.speed
        else -> "0.0 KB/s"
    }

    AmbientGlowBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Header Row
            Text(
                text = "Download Queue",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Storage & Bandwidth Summary Bar
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = null,
                            tint = DigitalIrisSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = speedSummaryText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Storage,
                            contentDescription = null,
                            tint = PrismaticMintTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = freeStorageText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Actions & Queue Counter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val activeCount = downloads.count { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.CONVERTING }
                val hasActiveDownloads = activeCount > 0

                Text(
                    text = "$activeCount Active • ${downloads.size} Total",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = onPauseAllClick,
                        enabled = hasActiveDownloads,
                        shape = RoundedCornerShape(10.dp),
                        color = if (hasActiveDownloads) Color.White else Color(0xFFF1F5F9).copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (hasActiveDownloads) Color.White.copy(alpha = 0.8f) else Color(0xFFE2E8F0).copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                            Text(
                                text = "Pause All",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (hasActiveDownloads) TextPrimary else TextMuted.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }

                    Surface(
                        onClick = onCancelAllClick,
                        enabled = hasActiveDownloads,
                        shape = RoundedCornerShape(10.dp),
                        color = if (hasActiveDownloads) Color.White else Color(0xFFF1F5F9).copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (hasActiveDownloads) Color.White.copy(alpha = 0.8f) else Color(0xFFE2E8F0).copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                            Text(
                                text = "Cancel All",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (hasActiveDownloads) Color(0xFFEF4444) else TextMuted.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Queue List or Empty State
            if (downloads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEEF2FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                tint = DigitalIrisSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No active downloads",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Paste a video link on the Home tab to start downloading",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            ),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        bottom = if (isMiniPlayerVisible) 160.dp else 96.dp
                    )
                ) {
                    items(
                        items = downloads,
                        key = { it.id },
                        contentType = { "download_card" }
                    ) { item ->
                        DownloadCard(
                            item = item,
                            onPauseToggle = {
                                if (item.status == DownloadStatus.PAUSED) onResumeClick(item.id) else onPauseClick(item.id)
                            },
                            onRetry = { onResumeClick(item.id) },
                            onCancel = { onCancelClick(item.id) },
                            onOpenSettings = onNavigateToSettings,
                            onViewDetails = {
                                val notice = DownloadErrorHelper.parse(item.statusMessage)
                                selectedErrorItem = Pair(item, notice)
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(140.dp))
                    }
                }
            }

            // Error details modal
            selectedErrorItem?.let { (item, notice) ->
                DownloadErrorDetailsDialog(
                    itemTitle = item.title,
                    notice = notice,
                    onRetry = {
                        onResumeClick(item.id)
                        selectedErrorItem = null
                    },
                    onOpenSettings = {
                        selectedErrorItem = null
                        onNavigateToSettings()
                    },
                    onDismiss = { selectedErrorItem = null }
                )
            }
        }
    }
}

@Composable
fun DownloadCard(
    item: ActiveDownload,
    onPauseToggle: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onOpenSettings: () -> Unit,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        elevation = 0.dp,
        borderAlpha = 0.85f
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: Thumbnail & Metadata Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 68.dp, height = 46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEEF2FF)),
                    contentAlignment = Alignment.Center
                ) {
                    val platformTint = when (item.platform) {
                        Platform.YOUTUBE -> YouTubeRed
                        Platform.YOUTUBE_MUSIC -> Color(0xFFFF0000)
                        Platform.SPOTIFY -> Color(0xFF1DB954)
                        Platform.FACEBOOK -> Color(0xFF1877F2)
                        Platform.TIKTOK -> Color(0xFF0F172A)
                        Platform.INSTAGRAM -> Color(0xFFE1306C)
                        Platform.TWITTER -> Color(0xFF0F172A)
                        Platform.UNIVERSAL -> DigitalIrisSecondary
                    }
                    val platformIcon = when (item.platform) {
                        Platform.SPOTIFY -> Icons.Rounded.MusicNote
                        Platform.UNIVERSAL -> Icons.Rounded.Language
                        Platform.TIKTOK -> Icons.Rounded.MusicNote
                        Platform.INSTAGRAM -> Icons.Rounded.CameraAlt
                        else -> Icons.Rounded.PlayCircle
                    }
                    Icon(
                        imageVector = platformIcon,
                        contentDescription = null,
                        tint = platformTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    val displayPlatform = item.customPlatformName ?: item.platform.displayName
                    val extraQuality = remember(item.formatLabel, item.qualityLabel) {
                        if (item.qualityLabel.isNotBlank() &&
                            !item.formatLabel.contains(item.qualityLabel, ignoreCase = true) &&
                            !item.qualityLabel.contains(item.formatLabel, ignoreCase = true)
                        ) {
                            item.qualityLabel
                        } else null
                    }
                    val subtitleAnnotated = remember(displayPlatform, item.formatLabel, extraQuality) {
                        androidx.compose.ui.text.buildAnnotatedString {
                            append(displayPlatform)
                            if (item.formatLabel.isNotBlank()) {
                                append(" • ")
                                withStyle(
                                    androidx.compose.ui.text.SpanStyle(
                                        color = DigitalIrisSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                ) {
                                    append(item.formatLabel)
                                }
                            }
                            if (!extraQuality.isNullOrBlank()) {
                                append(" • ")
                                append(extraQuality)
                            }
                        }
                    }
                    Text(
                        text = subtitleAnnotated,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Controls
                if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.PAUSED) {
                    IconButton(
                        onClick = onPauseToggle,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (item.status == DownloadStatus.PAUSED) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                contentDescription = if (item.status == DownloadStatus.PAUSED) "Resume" else "Pause",
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else if (item.status == DownloadStatus.ERROR) {
                    IconButton(
                        onClick = onRetry,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Retry",
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cancel",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Progress & State
            when (item.status) {
                DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED -> {
                    val animatedProgress by animateFloatAsState(
                        targetValue = item.progress.coerceIn(0f, 1f),
                        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                        label = "downloadProgress"
                    )

                    if (item.progress <= 0f && item.status == DownloadStatus.DOWNLOADING && item.speed.isBlank()) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = DigitalIrisSecondary,
                            trackColor = Color.White.copy(alpha = 0.6f)
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = if (item.status == DownloadStatus.PAUSED) Color(0xFFEAB308) else DigitalIrisSecondary,
                            trackColor = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val subtitleText = if (item.status == DownloadStatus.PAUSED) {
                            "Paused"
                        } else if (item.progress <= 0f) {
                            item.statusMessage ?: if (item.speed.isNotBlank()) item.speed else "Connecting..."
                        } else {
                            val speedPart = if (item.speed.isNotBlank() && item.speed.contains("B/s", ignoreCase = true)) {
                                item.speed
                            } else {
                                item.statusMessage ?: "Downloading..."
                            }
                            val etaPart = if (item.eta.isNotBlank()) " • ${item.eta}" else ""
                            "${(item.progress * 100).toInt()}% • $speedPart$etaPart"
                        }

                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            text = if (item.downloadedBytes.isNotBlank()) "${item.downloadedBytes} / ${item.totalBytes}" else item.totalBytes,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                DownloadStatus.CONVERTING -> {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = DigitalIrisSecondary,
                        trackColor = Color.White.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = DigitalIrisSecondary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.statusMessage ?: "Converting & Embedding Album Art...",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = DigitalIrisSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                DownloadStatus.QUEUED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Waiting in queue...",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Icon(
                            imageVector = Icons.Rounded.DragHandle,
                            contentDescription = "Reorder",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                DownloadStatus.COMPLETED -> {
                    LinearProgressIndicator(
                        progress = { 1.0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = PrismaticMintTertiary,
                        trackColor = Color.White.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Download Complete",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = PrismaticMintTertiary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = item.totalBytes,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                DownloadStatus.ERROR -> {
                    val notice = remember(item.statusMessage) {
                        DownloadErrorHelper.parse(item.statusMessage)
                    }

                    Surface(
                        onClick = onViewDetails,
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = notice.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF991B1B)
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = "Details ›",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFDC2626),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = notice.userFriendlyMessage,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF7F1D1D),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tap notice to view full log",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFB91C1C).copy(alpha = 0.8f),
                                        fontSize = 10.sp
                                    )
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (notice.isAuthRequired) {
                                        Surface(
                                            onClick = onOpenSettings,
                                            shape = RoundedCornerShape(8.dp),
                                            color = ObsidianPrimary,
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(horizontal = 10.dp)
                                            ) {
                                                Text(
                                                    text = "Settings",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color.White,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 11.sp
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    Surface(
                                        onClick = onRetry,
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 10.dp)
                                        ) {
                                            Text(
                                                text = "Retry",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF991B1B),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {}
            }
        }
    }
}
