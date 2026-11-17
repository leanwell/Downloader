package com.leanwell.downloader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.leanwell.downloader.model.MediaItem
import com.leanwell.downloader.model.MediaType
import com.leanwell.downloader.ui.theme.DigitalIrisSecondary
import com.leanwell.downloader.ui.theme.ObsidianPrimary
import com.leanwell.downloader.ui.theme.TextPrimary
import com.leanwell.downloader.ui.theme.TextSecondary
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

@Composable
fun FrostedMiniPlayerBar(
    mediaItem: MediaItem,
    isPlaying: Boolean,
    progress: Float, // 0f to 1f
    onPlayPauseToggle: () -> Unit,
    onSkipNext: () -> Unit,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val playerShape = RoundedCornerShape(20.dp)
    val borderColor = Color.White.copy(alpha = 0.85f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .shadow(12.dp, playerShape, ambientColor = Color(0x1F0F172A), spotColor = Color(0x1F0F172A))
            .clip(playerShape)
            .then(
                if (hazeState != null) {
                    Modifier.hazeEffect(
                        state = hazeState,
                        style = HazeDefaults.style(
                            backgroundColor = Color.Transparent,
                            tint = HazeDefaults.tint(Color.White.copy(alpha = 0.55f)),
                            blurRadius = 28.dp
                        )
                    )
                } else {
                    Modifier.background(Color.White.copy(alpha = 0.90f))
                }
            )
            .border(1.dp, borderColor, playerShape)
            .clickable(onClick = onClick)
    ) {
        // Mini Player Content
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini Album Art
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEEF2FF))
                        .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!mediaItem.artworkUri.isNullOrBlank()) {
                        AsyncImage(
                            model = mediaItem.artworkUri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = if (mediaItem.mediaType == MediaType.VIDEO) Icons.Rounded.Videocam else Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = DigitalIrisSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Track Title & Channel/Artist
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = mediaItem.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            initialDelayMillis = 1000,
                            repeatDelayMillis = 1500,
                            velocity = 35.dp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = mediaItem.artistOrChannel.ifBlank { mediaItem.platform.displayName },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = TextSecondary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            initialDelayMillis = 1500,
                            repeatDelayMillis = 1500,
                            velocity = 30.dp
                        )
                    )
                }

                // Controls: Play/Pause, Next Track, Close
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onPlayPauseToggle,
                        modifier = Modifier
                            .size(36.dp)
                            .background(ObsidianPrimary, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onSkipNext,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next Track",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Player",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Minimal Progress Bar along bottom edge of mini-player
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = DigitalIrisSecondary,
                trackColor = Color(0xFFE2E8F0)
            )
        }
    }
}
