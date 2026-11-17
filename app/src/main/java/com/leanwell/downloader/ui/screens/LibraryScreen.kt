package com.leanwell.downloader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.leanwell.downloader.model.MediaItem
import com.leanwell.downloader.model.MediaType
import com.leanwell.downloader.model.Platform
import com.leanwell.downloader.ui.components.AmbientGlowBackground
import com.leanwell.downloader.ui.components.GlassCard
import com.leanwell.downloader.ui.theme.DigitalIrisSecondary
import com.leanwell.downloader.ui.theme.ObsidianPrimary
import com.leanwell.downloader.ui.theme.PrismaticMintTertiary
import com.leanwell.downloader.ui.theme.TextMuted
import com.leanwell.downloader.ui.theme.TextPrimary
import com.leanwell.downloader.ui.theme.TextSecondary
import com.leanwell.downloader.ui.theme.YouTubeRed
import java.io.File

enum class LibrarySortOrder(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    SIZE_DESC("Largest"),
    NAME_ASC("A–Z")
}

@Composable
fun LibraryScreen(
    mediaItems: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    onItemMenuClick: (MediaItem) -> Unit,
    onDeleteItems: (List<MediaItem>) -> Unit = {},
    isMiniPlayerVisible: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedPlatformFilter by remember { mutableStateOf<Platform?>(null) }
    var sortOrder by remember { mutableStateOf(LibrarySortOrder.NEWEST) }
    var showSortMenu by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedItemIds by remember { mutableStateOf(setOf<String>()) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val totalStorageText = remember(mediaItems) {
        var totalBytes = 0L
        for (item in mediaItems) {
            val path = item.filePath
            if (path != null) {
                val file = java.io.File(path)
                if (file.exists()) {
                    totalBytes += file.length()
                }
            }
        }
        if (totalBytes >= 1024L * 1024L * 1024L) {
            String.format(java.util.Locale.US, "%.1f GB", totalBytes / (1024.0 * 1024.0 * 1024.0))
        } else {
            val mb = totalBytes / (1024.0 * 1024.0)
            String.format(java.util.Locale.US, "%.1f MB", mb)
        }
    }

    val filterOptions = listOf(
        "All (${mediaItems.size})",
        "Music (${mediaItems.count { it.mediaType == MediaType.AUDIO }})",
        "Videos (${mediaItems.count { it.mediaType == MediaType.VIDEO }})"
    )

    val filteredItems = remember(selectedFilter, selectedPlatformFilter, sortOrder, searchQuery, mediaItems) {
        val baseList = mediaItems.filter { item ->
            val matchesCategory = when {
                selectedFilter.startsWith("Music") -> item.mediaType == MediaType.AUDIO
                selectedFilter.startsWith("Videos") -> item.mediaType == MediaType.VIDEO
                else -> true
            }
            val matchesPlatform = selectedPlatformFilter == null || item.platform == selectedPlatformFilter
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.artistOrChannel.contains(searchQuery, ignoreCase = true) ||
                (item.playlistName?.contains(searchQuery, ignoreCase = true) == true)
            }
            matchesCategory && matchesPlatform && matchesSearch
        }

        when (sortOrder) {
            LibrarySortOrder.NEWEST -> baseList
            LibrarySortOrder.OLDEST -> baseList.reversed()
            LibrarySortOrder.NAME_ASC -> baseList.sortedBy { it.title.lowercase() }
            LibrarySortOrder.SIZE_DESC -> baseList.sortedByDescending { item ->
                parseFileSizeToBytes(item.fileSize)
            }
        }
    }

    AmbientGlowBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (isSelectionMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                                .clickable {
                                    isSelectionMode = false
                                    selectedItemIds = emptySet()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Cancel",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (selectedItemIds.isEmpty()) "Select items" else "${selectedItemIds.size} selected",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val allSelected = filteredItems.isNotEmpty() && selectedItemIds.size == filteredItems.size
                        Surface(
                            onClick = {
                                selectedItemIds = if (allSelected) {
                                    emptySet()
                                } else {
                                    filteredItems.map { it.id }.toSet()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = if (allSelected) "Deselect All" else "Select All",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                if (selectedItemIds.isNotEmpty()) {
                                    showDeleteConfirmDialog = true
                                }
                            },
                            enabled = selectedItemIds.isNotEmpty(),
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedItemIds.isNotEmpty()) YouTubeRed else Color.LightGray.copy(alpha = 0.4f),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (allSelected) "Delete All" else "Delete (${selectedItemIds.size})",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Media Library",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "${mediaItems.size} files downloaded ($totalStorageText)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                                .clickable {
                                    isSelectionMode = true
                                    selectedItemIds = emptySet()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Batch Delete",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSearchActive) ObsidianPrimary else Color.White)
                                .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                                .clickable {
                                    isSearchActive = !isSearchActive
                                    if (!isSearchActive) searchQuery = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                                contentDescription = "Search",
                                tint = if (isSearchActive) Color.White else TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isSearchActive && !isSelectionMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Search songs, videos, creators...",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = DigitalIrisSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Clear,
                                        contentDescription = "Clear",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White.copy(alpha = 0.85f),
                            focusedBorderColor = DigitalIrisSecondary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.85f),
                            cursorColor = ObsidianPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filterOptions) { option ->
                    val isSelected = (selectedFilter.startsWith("All") && option.startsWith("All")) ||
                            (selectedFilter.startsWith("Music") && option.startsWith("Music")) ||
                            (selectedFilter.startsWith("Videos") && option.startsWith("Videos"))

                    Surface(
                        onClick = { selectedFilter = option },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) ObsidianPrimary else Color.White,
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextPrimary
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    item {
                        val isAllSelected = selectedPlatformFilter == null
                        Surface(
                            onClick = { selectedPlatformFilter = null },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isAllSelected) ObsidianPrimary else Color.White,
                            border = if (isAllSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Text(
                                    text = "All Sources",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isAllSelected) Color.White else TextSecondary
                                    )
                                )
                            }
                        }
                    }

                    items(Platform.values()) { platform ->
                        val isSelected = selectedPlatformFilter == platform
                        Surface(
                            onClick = {
                                selectedPlatformFilter = if (isSelected) null else platform
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) ObsidianPrimary else Color.White,
                            border = if (isSelected) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Text(
                                    text = platform.displayName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }

                Box {
                    Surface(
                        onClick = { showSortMenu = true },
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.85f)),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Sort,
                                contentDescription = "Sort",
                                tint = DigitalIrisSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = sortOrder.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        LibrarySortOrder.values().forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = order.label,
                                        fontWeight = if (sortOrder == order) FontWeight.Bold else FontWeight.Normal,
                                        color = if (sortOrder == order) ObsidianPrimary else TextPrimary,
                                        fontSize = 12.sp
                                    )
                                },
                                onClick = {
                                    sortOrder = order
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredItems.isEmpty()) {
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
                                imageVector = Icons.Rounded.Headphones,
                                contentDescription = null,
                                tint = DigitalIrisSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching files found" else "Your library is empty",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Try searching for a different title or creator" else "Downloaded songs and videos will appear here ready for offline playback",
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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        bottom = if (isMiniPlayerVisible) 160.dp else 96.dp
                    )
                ) {
                    items(
                        items = filteredItems,
                        key = { it.id },
                        contentType = { "media_row" }
                    ) { item ->
                        val isSelected = item.id in selectedItemIds
                        MediaItemRow(
                            item = item,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onToggleSelect = {
                                selectedItemIds = if (isSelected) {
                                    selectedItemIds - item.id
                                } else {
                                    selectedItemIds + item.id
                                }
                            },
                            onClick = {
                                if (isSelectionMode) {
                                    selectedItemIds = if (isSelected) {
                                        selectedItemIds - item.id
                                    } else {
                                        selectedItemIds + item.id
                                    }
                                } else {
                                    onItemClick(item)
                                }
                            },
                            onMenuClick = { onItemMenuClick(item) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(180.dp))
                    }
                }
            }
        }

        if (showDeleteConfirmDialog) {
            val count = selectedItemIds.size
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = {
                    Text(
                        text = if (count == mediaItems.size) "Delete All Files?" else "Delete $count File${if (count > 1) "s" else ""}?",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to permanently delete ${if (count == mediaItems.size) "all downloaded files" else "$count selected file${if (count > 1) "s" else ""}"} from your device storage? This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            lineHeight = 20.sp
                        )
                    )
                },
                confirmButton = {
                    Surface(
                        onClick = {
                            val toDelete = mediaItems.filter { it.id in selectedItemIds }
                            onDeleteItems(toDelete)
                            showDeleteConfirmDialog = false
                            isSelectionMode = false
                            selectedItemIds = emptySet()
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = YouTubeRed
                    ) {
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                },
                dismissButton = {
                    Surface(
                        onClick = { showDeleteConfirmDialog = false },
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Transparent
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun MediaItemRow(
    item: MediaItem,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageRequest = remember(item.artworkUri) {
        if (!item.artworkUri.isNullOrBlank()) {
            coil.request.ImageRequest.Builder(context)
                .data(item.artworkUri)
                .size(160, 125)
                .crossfade(false)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .build()
        } else null
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = 0.dp,
        borderAlpha = 0.85f,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = ObsidianPrimary,
                        checkmarkColor = Color.White,
                        uncheckedColor = TextSecondary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 50.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFEEF2FF)),
                contentAlignment = Alignment.Center
            ) {
                if (imageRequest != null) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val thumbTint = if (item.mediaType == MediaType.AUDIO) {
                        DigitalIrisSecondary
                    } else when (item.platform) {
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
                        imageVector = if (item.mediaType == MediaType.AUDIO) Icons.Rounded.MusicNote else Icons.Rounded.Videocam,
                        contentDescription = null,
                        tint = thumbTint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = item.duration,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontSize = 8.sp
                        )
                    )
                }
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
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.artistOrChannel,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (!item.playlistName.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DigitalIrisSecondary.copy(alpha = 0.08f),
                            border = BorderStroke(0.8.dp, DigitalIrisSecondary.copy(alpha = 0.25f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = DigitalIrisSecondary,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = item.playlistName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DigitalIrisSecondary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayPlatform = item.customPlatformName ?: item.platform.displayName
                    val metadataAnnotated = remember(displayPlatform, item.formatTag, item.fileSize) {
                        androidx.compose.ui.text.buildAnnotatedString {
                            append(displayPlatform)
                            if (item.formatTag.isNotBlank()) {
                                append(" • ")
                                withStyle(
                                    androidx.compose.ui.text.SpanStyle(
                                        color = DigitalIrisSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                ) {
                                    append(item.formatTag)
                                }
                            }
                            if (item.fileSize.isNotBlank()) {
                                append(" • ")
                                append(item.fileSize)
                            }
                        }
                    }

                    Text(
                        text = metadataAnnotated,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (item.dateAdded.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.dateAdded,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            if (!isSelectionMode) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Options",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun parseFileSizeToBytes(sizeStr: String): Long {
    val clean = sizeStr.trim()
    val num = clean.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: return 0L
    return when {
        clean.contains("GB", ignoreCase = true) -> (num * 1024 * 1024 * 1024).toLong()
        clean.contains("MB", ignoreCase = true) -> (num * 1024 * 1024).toLong()
        clean.contains("KB", ignoreCase = true) -> (num * 1024).toLong()
        else -> num.toLong()
    }
}
