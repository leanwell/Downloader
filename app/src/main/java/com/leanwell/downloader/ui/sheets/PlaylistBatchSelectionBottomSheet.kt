package com.leanwell.downloader.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.leanwell.downloader.engine.ExtractedPlaylistInfo
import com.leanwell.downloader.engine.PlaylistItemInfo
import com.leanwell.downloader.engine.PlaylistQualityPreset
import com.leanwell.downloader.model.MediaType
import com.leanwell.downloader.ui.components.GlassCard
import com.leanwell.downloader.ui.components.ObsidianButton
import com.leanwell.downloader.ui.theme.DigitalIrisSecondary
import com.leanwell.downloader.ui.theme.ObsidianPrimary
import com.leanwell.downloader.ui.theme.PrismaticMintTertiary
import com.leanwell.downloader.ui.theme.TextMuted
import com.leanwell.downloader.ui.theme.TextPrimary
import com.leanwell.downloader.ui.theme.TextSecondary
import com.leanwell.downloader.ui.theme.YouTubeRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistBatchSelectionBottomSheet(
    playlistInfo: ExtractedPlaylistInfo,
    onDismissRequest: () -> Unit,
    onConfirmBatchDownload: (List<PlaylistItemInfo>, PlaylistQualityPreset, String) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var selectedPreset by remember { mutableStateOf(PlaylistQualityPreset.AUDIO_320K) }
    var selectedIds by remember { mutableStateOf(playlistInfo.items.map { it.id }.toSet()) }
    val defaultFolderName = remember(playlistInfo.title) { com.leanwell.downloader.engine.YoutubeEngine.sanitizeFilename(playlistInfo.title) }
    var folderName by remember(defaultFolderName) { mutableStateOf(defaultFolderName) }
    var isSubmitting by remember { mutableStateOf(false) }

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
                .padding(bottom = 16.dp)
        ) {
            // Header: Playlist Thumbnail + Title + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Playlist Cover
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFEEF2FF))
                        .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!playlistInfo.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = playlistInfo.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.PlaylistPlay,
                            contentDescription = null,
                            tint = YouTubeRed,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlistInfo.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${playlistInfo.author} • ${playlistInfo.itemCount} items",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

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

            // Folder Organization Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = null,
                                tint = DigitalIrisSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Playlist Folder",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        if (folderName != defaultFolderName) {
                            Surface(
                                onClick = { folderName = defaultFolderName },
                                shape = RoundedCornerShape(6.dp),
                                color = DigitalIrisSecondary.copy(alpha = 0.08f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.RestartAlt,
                                        contentDescription = "Reset",
                                        tint = DigitalIrisSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Reset",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = DigitalIrisSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it.replace(Regex("[/\\\\:*?\"<>|]"), "_") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            fontSize = 13.sp
                        ),
                        placeholder = {
                            Text(
                                text = "Folder Name",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 13.sp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DigitalIrisSecondary,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val effectiveFolder = folderName.trim().ifEmpty { defaultFolderName }
                    Text(
                        text = "Save path: Downloads/Downloader/$effectiveFolder/",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontSize = 10.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quality Preset Selector Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Download Preset",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(PlaylistQualityPreset.values()) { preset ->
                            val isSelected = selectedPreset == preset
                            Surface(
                                onClick = { selectedPreset = preset },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) ObsidianPrimary else Color.White,
                                border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (preset.mediaType == MediaType.AUDIO) Icons.Rounded.Headphones else Icons.Rounded.Videocam,
                                        contentDescription = null,
                                        tint = if (isSelected) PrismaticMintTertiary else DigitalIrisSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = preset.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else TextPrimary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Selection Bar: Counter + Select All
            val allSelected = selectedIds.size == playlistInfo.items.size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedIds.size} of ${playlistInfo.items.size} selected",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )

                Surface(
                    onClick = {
                        selectedIds = if (allSelected) emptySet() else playlistInfo.items.map { it.id }.toSet()
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.height(28.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                        Text(
                            text = if (allSelected) "Deselect All" else "Select All",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable Items List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .height((playlistInfo.items.size * 64).coerceAtMost(280).dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(playlistInfo.items, key = { _, item -> item.id }) { index, item ->
                    val isChecked = item.id in selectedIds
                    Surface(
                        onClick = {
                            selectedIds = if (isChecked) selectedIds - item.id else selectedIds + item.id
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isChecked) Color.White else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.dp,
                            if (isChecked) DigitalIrisSecondary.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.05f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) selectedIds + item.id else selectedIds - item.id
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = ObsidianPrimary,
                                    uncheckedColor = Color(0xFF94A3B8)
                                ),
                                modifier = Modifier.size(28.dp)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            // Thumbnail with duration overlay
                            Box(
                                modifier = Modifier
                                    .size(width = 64.dp, height = 40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                if (!item.thumbnailUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = item.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                if (item.durationText.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .padding(2.dp)
                                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(3.dp))
                                            .padding(horizontal = 3.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = item.durationText,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "#${index + 1} • ${item.author}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button: Download Selected Items
            ObsidianButton(
                text = if (isSubmitting) "Queueing Downloads..." else if (selectedIds.isEmpty()) "Select at least 1 item" else "Queue ${selectedIds.size} Downloads",
                onClick = {
                    if (!isSubmitting) {
                        val selectedItems = playlistInfo.items.filter { it.id in selectedIds }
                        if (selectedItems.isNotEmpty()) {
                            isSubmitting = true
                            val effectiveFolder = folderName.trim().ifEmpty { defaultFolderName }
                            onConfirmBatchDownload(selectedItems, selectedPreset, effectiveFolder)
                        }
                    }
                },
                enabled = selectedIds.isNotEmpty() && !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                height = 50.dp,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}
