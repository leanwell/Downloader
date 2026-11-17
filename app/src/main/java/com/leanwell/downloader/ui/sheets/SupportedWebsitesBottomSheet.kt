package com.leanwell.downloader.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leanwell.downloader.ui.components.GlassCard
import com.leanwell.downloader.ui.theme.DigitalIrisSecondary
import com.leanwell.downloader.ui.theme.TextPrimary
import com.leanwell.downloader.ui.theme.TextSecondary
import com.leanwell.downloader.ui.theme.YouTubeRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportedWebsitesBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Supported Platforms",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Where you can grab audio and video links from",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
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

            Spacer(modifier = Modifier.height(18.dp))

            // Platform 1: YouTube
            PlatformFeatureCard(
                name = "YouTube",
                tagline = "Videos, Shorts, Music & Playlists",
                icon = Icons.Rounded.PlayCircle,
                iconTint = YouTubeRed,
                iconBg = YouTubeRed.copy(alpha = 0.12f),
                features = listOf(
                    "Standard & Short-form videos (youtube.com, youtu.be)",
                    "Resolutions from 144p up to 4K & 8K Ultra HD",
                    "Pristine audio extraction in 320 kbps MP3 & M4A AAC",
                    "Cookie authentication for private or member-only content"
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Platform 1B: YouTube Music
            PlatformFeatureCard(
                name = "YouTube Music",
                tagline = "Tracks, Albums & Playlists",
                icon = Icons.Rounded.PlayCircle,
                iconTint = Color(0xFFFF0000),
                iconBg = Color(0xFFFF0000).copy(alpha = 0.12f),
                features = listOf(
                    "Direct support for music.youtube.com links",
                    "Audio-first workflow defaulted to 320 kbps MP3",
                    "Batch download full playlists into custom subfolders",
                    "Seamless stream extraction and tag embedding"
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Platform 1C: Spotify (SpotDL Resolver)
            PlatformFeatureCard(
                name = "Spotify",
                tagline = "Tracks, Playlists & Albums (SpotDL Resolver)",
                icon = Icons.Rounded.MusicNote,
                iconTint = Color(0xFF1DB954),
                iconBg = Color(0xFF1DB954).copy(alpha = 0.12f),
                features = listOf(
                    "Single tracks, full albums & playlists (open.spotify.com)",
                    "Matches audio streams from YouTube / YouTube Music",
                    "Preserves Spotify track titles, artists, and high-res cover art",
                    "Custom playlist folder organization on device"
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Platform 2: Facebook with genuine FB logo
            PlatformFeatureCard(
                name = "Facebook",
                tagline = "Reels, Watch & Public Videos",
                customLogo = { FacebookLogo() },
                features = listOf(
                    "Facebook Reels & Stories (facebook.com/reel/...)",
                    "Watch & Page videos (fb.watch, facebook.com/watch...)",
                    "High Definition (1080p/720p HD) and Standard SD formats",
                    "Session cookies supported in Settings for restricted content"
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Platform 3: TikTok
            PlatformFeatureCard(
                name = "TikTok",
                tagline = "Watermark-Free HD Videos & Music",
                customLogo = { TikTokLogo() },
                features = listOf(
                    "Standard & short-form clips (tiktok.com, vm.tiktok.com)",
                    "Clean HD video download without watermarks",
                    "Audio extraction to 320 kbps MP3",
                    "Fast metadata inspection and immediate queuing"
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Platform 4: Instagram
            PlatformFeatureCard(
                name = "Instagram",
                tagline = "Reels, Posts, Stories & Audio",
                customLogo = { InstagramLogo() },
                features = listOf(
                    "Instagram Reels, video posts & IGTV (instagram.com/reel/...)",
                    "Original best quality MP4 stream preservation",
                    "Background MP3 audio extraction",
                    "Instagram session cookies in Settings for private followed accounts"
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Platform 5: Twitter / X
            PlatformFeatureCard(
                name = "Twitter / X",
                tagline = "Embedded Videos & High-FPS GIFs",
                customLogo = { TwitterLogo() },
                features = listOf(
                    "Tweet video clips and animations (twitter.com, x.com)",
                    "High Definition (1080p/720p) and Data Saver SD choices",
                    "Direct MP4 conversion from loop streams",
                    "No login required for public tweets"
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Platform 6: Universal Engine
            PlatformFeatureCard(
                name = "Universal Core Engine",
                tagline = "1,000+ Online Sites & Media Streams",
                icon = Icons.Rounded.Language,
                iconTint = Color(0xFF0F172A),
                iconBg = Color(0xFF0F172A).copy(alpha = 0.08f),
                features = listOf(
                    "Powered by the latest yt-dlp & FFmpeg embedded core",
                    "Direct MP4, WebM, and audio stream links",
                    "Automatic tag and album art embedding",
                    "OTA updates via Settings keep extractors fresh"
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tip Card with separated platform bullets
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFEEF2FF))
                    .border(1.dp, DigitalIrisSecondary.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "• ",
                            color = YouTubeRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "YouTube: For age-restricted, private, or members-only videos, connect your account or import cookies.txt in Settings.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        )
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "• ",
                            color = Color(0xFF1877F2),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Facebook: Public reels download directly. For private groups or friend-restricted posts, import your cookies in Settings.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        )
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "• ",
                            color = Color(0xFFE1306C),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Instagram: Public reels & posts download instantly. Private accounts you follow require Instagram cookies in Settings.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        )
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "• ",
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "TikTok & Twitter/X: Watermark-free clips download directly with zero login necessary.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FacebookLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xFF1877F2)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "f",
            style = MaterialTheme.typography.titleLarge.copy(
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp
            ),
            modifier = Modifier.offset(x = 1.dp, y = (-2).dp)
        )
    }
}

@Composable
fun TikTokLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = "TikTok",
            tint = Color(0xFF25F4EE),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun InstagramLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.CameraAlt,
            contentDescription = "Instagram",
            tint = Color.White,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
fun TwitterLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "X",
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        )
    }
}

@Composable
private fun PlatformFeatureCard(
    name: String,
    tagline: String,
    icon: ImageVector? = null,
    iconTint: Color = Color.Unspecified,
    iconBg: Color = Color.Transparent,
    customLogo: (@Composable () -> Unit)? = null,
    features: List<String>,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (customLogo != null) {
                    customLogo()
                } else if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
                    )
                    Text(
                        text = tagline,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bullet list instead of check icons
            features.forEach { feature ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = 4.dp, end = 10.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(TextSecondary.copy(alpha = 0.85f))
                    )
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    )
                }
            }
        }
    }
}
