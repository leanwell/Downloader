package com.leanwell.downloader.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.leanwell.downloader.model.FormatOption
import com.leanwell.downloader.model.MediaType
import com.leanwell.downloader.model.Platform
import com.leanwell.downloader.ui.components.ObsidianButton
import com.leanwell.downloader.ui.theme.DigitalIrisSecondary
import com.leanwell.downloader.ui.theme.ObsidianPrimary
import com.leanwell.downloader.ui.theme.PrismaticMintTertiary
import com.leanwell.downloader.ui.theme.TextMuted
import com.leanwell.downloader.ui.theme.TextPrimary
import com.leanwell.downloader.ui.theme.TextSecondary
import com.leanwell.downloader.ui.theme.YouTubeRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatSelectionBottomSheet(
    videoTitle: String,
    channelName: String,
    duration: String,
    thumbnailUrl: String? = null,
    customAudioFormats: List<FormatOption>? = null,
    customVideoFormats: List<FormatOption>? = null,
    initialAutoEmbedMetadata: Boolean = true,
    platform: Platform = Platform.YOUTUBE,
    onDismissRequest: () -> Unit,
    onConfirmDownload: (FormatOption, Boolean) -> Unit,
    onScanHighRes: (suspend () -> List<FormatOption>?)? = null,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var selectedMediaType by remember { mutableStateOf(MediaType.AUDIO) }
    var autoEmbedMetadata by remember(initialAutoEmbedMetadata) { mutableStateOf(initialAutoEmbedMetadata) }
    val coroutineScope = rememberCoroutineScope()

    var isScanningHighRes by remember { mutableStateOf(false) }
    var highResScanResultText by remember { mutableStateOf<String?>(null) }
    var hasScannedHighRes by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val audioFormats = remember(customAudioFormats) {
        customAudioFormats ?: listOf(
            FormatOption("mp3_320", "MP3 High Quality (320 kbps)", null, "~14.2 MB", MediaType.AUDIO, isRecommended = true),
            FormatOption("mp3_192", "MP3 Standard (192 kbps)", null, "~8.6 MB", MediaType.AUDIO),
            FormatOption("m4a_aac", "M4A Original (AAC)", null, "~11.0 MB", MediaType.AUDIO)
        )
    }

    var videoFormatList by remember(customVideoFormats) {
        mutableStateOf(
            customVideoFormats ?: listOf(
                FormatOption("video_1080", "1080p Full HD", null, "~150 MB", MediaType.VIDEO, isRecommended = true),
                FormatOption("video_720", "720p HD", null, "~75 MB", MediaType.VIDEO),
                FormatOption("video_480", "480p SD", null, "~35 MB", MediaType.VIDEO),
                FormatOption("video_360", "360p Standard", null, "~20 MB", MediaType.VIDEO),
                FormatOption("video_240", "240p Compact", null, "~12 MB", MediaType.VIDEO),
                FormatOption("video_144", "144p Data Saver", null, "~6 MB", MediaType.VIDEO)
            )
        )
    }

    var selectedFormat by remember(selectedMediaType, audioFormats, videoFormatList) {
        val defaultOption = if (selectedMediaType == MediaType.AUDIO) {
            audioFormats.firstOrNull { it.isRecommended } ?: audioFormats.first()
        } else {
            videoFormatList.firstOrNull { it.isRecommended } ?: videoFormatList.first()
        }
        mutableStateOf(defaultOption)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFFFDFEFE),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .size(width = 44.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Download Options",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Media Preview Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 16:9 Thumbnail preview box
                    Box(
                        modifier = Modifier
                            .size(width = 80.dp, height = 52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!thumbnailUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = thumbnailUrl,
                                contentDescription = "Video Thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val thumbIconTint = when (platform) {
                                Platform.YOUTUBE -> YouTubeRed
                                Platform.YOUTUBE_MUSIC -> Color(0xFFFF0000)
                                Platform.SPOTIFY -> Color(0xFF1DB954)
                                Platform.FACEBOOK -> Color(0xFF1877F2)
                                Platform.TIKTOK -> Color(0xFF0F172A)
                                Platform.INSTAGRAM -> Color(0xFFE1306C)
                                Platform.TWITTER -> Color(0xFF0F172A)
                                Platform.UNIVERSAL -> DigitalIrisSecondary
                            }
                            Icon(
                                imageVector = Icons.Rounded.PlayCircle,
                                contentDescription = null,
                                tint = thumbIconTint,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(3.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = duration,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = videoTitle,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 13.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = channelName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val platformLabel = platform.displayName
                            val platformColor = when (platform) {
                                Platform.YOUTUBE -> YouTubeRed
                                Platform.YOUTUBE_MUSIC -> Color(0xFFFF0000)
                                Platform.SPOTIFY -> Color(0xFF1DB954)
                                Platform.FACEBOOK -> Color(0xFF1877F2)
                                Platform.TIKTOK -> Color(0xFF0F172A)
                                Platform.INSTAGRAM -> Color(0xFFE1306C)
                                Platform.TWITTER -> Color(0xFF0F172A)
                                Platform.UNIVERSAL -> DigitalIrisSecondary
                            }
                            Text(
                                text = "• $platformLabel",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = platformColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Segmented Switcher (Audio vs Video)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEEF2FF))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Audio Tab
                val isAudioSelected = selectedMediaType == MediaType.AUDIO
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isAudioSelected) ObsidianPrimary else Color.Transparent)
                        .clickable { selectedMediaType = MediaType.AUDIO },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Headphones,
                            contentDescription = null,
                            tint = if (isAudioSelected) Color.White else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Audio (MP3)",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isAudioSelected) Color.White else TextSecondary
                            )
                        )
                    }
                }

                // Video Tab
                val isVideoSelected = selectedMediaType == MediaType.VIDEO
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isVideoSelected) ObsidianPrimary else Color.Transparent)
                        .clickable { selectedMediaType = MediaType.VIDEO },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Videocam,
                            contentDescription = null,
                            tint = if (isVideoSelected) Color.White else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Video (MP4)",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isVideoSelected) Color.White else TextSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // High-Resolution On-Demand Scan Card (Shown on Video Tab for YouTube)
            if (selectedMediaType == MediaType.VIDEO && onScanHighRes != null && platform == Platform.YOUTUBE) {
                Surface(
                    onClick = {
                        if (!isScanningHighRes && !hasScannedHighRes) {
                            coroutineScope.launch {
                                isScanningHighRes = true
                                val updated = onScanHighRes()
                                isScanningHighRes = false
                                hasScannedHighRes = true
                                if (!updated.isNullOrEmpty()) {
                                    val hasAbove1080 = updated.any {
                                        it.id in listOf("video_1440", "video_2160", "video_4320")
                                    }
                                    if (hasAbove1080) {
                                        videoFormatList = updated
                                        highResScanResultText = "Unlocked higher resolution streams!"
                                    } else {
                                        highResScanResultText = "1080p is the highest available stream"
                                    }
                                } else {
                                    highResScanResultText = "1080p is the highest available stream"
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(DigitalIrisSecondary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = DigitalIrisSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Check for 2K / 4K / 8K resolutions",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = highResScanResultText ?: "Scan on-demand for higher video streams",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (hasScannedHighRes) DigitalIrisSecondary else TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        if (isScanningHighRes) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = DigitalIrisSecondary
                            )
                        } else if (hasScannedHighRes) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = PrismaticMintTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Format Options List
            val currentFormats = if (selectedMediaType == MediaType.AUDIO) audioFormats else videoFormatList
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                currentFormats.forEach { format ->
                    val isSelected = format.id == selectedFormat.id
                    val itemBg = if (isSelected) Color(0xFFF1F5F9) else Color.White.copy(alpha = 0.6f)
                    val itemBorder = if (isSelected) DigitalIrisSecondary else Color.White.copy(alpha = 0.6f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(itemBg)
                            .border(1.2.dp, itemBorder, RoundedCornerShape(14.dp))
                            .clickable { selectedFormat = format }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedFormat = format },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = DigitalIrisSecondary,
                                    unselectedColor = TextMuted
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = format.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = TextPrimary
                                        )
                                    )
                                }
                            }
                        }

                        Text(
                            text = format.estimatedSize,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metadata Auto-Embed Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { autoEmbedMetadata = !autoEmbedMetadata }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = autoEmbedMetadata,
                    onCheckedChange = { autoEmbedMetadata = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = ObsidianPrimary,
                        checkmarkColor = Color.White
                    ),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Auto-embed album art & ID3 tags",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Tags artist, title & cover automatically",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Download CTA Button
            ObsidianButton(
                text = if (isSubmitting) "Adding to Queue..." else "Download • ${selectedFormat.label.substringBefore(" ")} (${selectedFormat.estimatedSize})",
                onClick = {
                    if (!isSubmitting) {
                        isSubmitting = true
                        onConfirmDownload(selectedFormat, autoEmbedMetadata)
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                height = 52.dp,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
        }
    }
}
