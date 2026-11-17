package com.leanwell.downloader.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DynamicFeed
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leanwell.downloader.R
import com.leanwell.downloader.settings.AppSettings
import com.leanwell.downloader.ui.components.AmbientGlowBackground
import com.leanwell.downloader.ui.components.GlassCard
import com.leanwell.downloader.ui.dialogs.CookiesGuideDialog
import com.leanwell.downloader.ui.dialogs.FacebookCookiesGuideDialog
import com.leanwell.downloader.ui.dialogs.InstagramCookiesGuideDialog
import com.leanwell.downloader.ui.dialogs.YouTubeLoginDialog
import com.leanwell.downloader.ui.theme.DigitalIrisSecondary
import com.leanwell.downloader.ui.theme.ObsidianPrimary
import com.leanwell.downloader.ui.theme.PrismaticMintTertiary
import com.leanwell.downloader.ui.theme.TextMuted
import com.leanwell.downloader.ui.theme.TextPrimary
import com.leanwell.downloader.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun SettingsScreen(
    onLicensesClick: () -> Unit,
    onTermsClick: () -> Unit,
    onRevisitOnboardingClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val engineVersion by AppSettings.engineVersion.collectAsState()
    val isCheckingUpdate by AppSettings.isCheckingUpdate.collectAsState()
    val wifiOnlyEnabled by AppSettings.isWifiOnly.collectAsState()
    val autoEmbedMetadata by AppSettings.autoEmbedMetadata.collectAsState()
    val autoSwitchToQueue by AppSettings.autoSwitchToQueue.collectAsState()
    val isPipEnabled by AppSettings.isPipEnabled.collectAsState()
    val isParallelDownloads by AppSettings.isParallelDownloads.collectAsState()
    val maxParallelCount by AppSettings.maxParallelCount.collectAsState()
    val hasCookies by AppSettings.hasCookies.collectAsState()
    val cookiesStatus by AppSettings.cookiesStatus.collectAsState()
    val hasFbCookies by AppSettings.hasFbCookies.collectAsState()
    val fbCookiesStatus by AppSettings.fbCookiesStatus.collectAsState()
    val hasIgCookies by AppSettings.hasIgCookies.collectAsState()
    val igCookiesStatus by AppSettings.igCookiesStatus.collectAsState()

    var showLoginDialog by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }
    var showFbGuideDialog by remember { mutableStateOf(false) }
    var showIgGuideDialog by remember { mutableStateOf(false) }

    val cookieFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val success = AppSettings.importCookiesFromUri(context, uri)
            if (success) {
                Toast.makeText(context, "Cookies imported successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to import cookies file.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val fbCookieFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val success = AppSettings.importFbCookiesFromUri(context, uri)
            if (success) {
                Toast.makeText(context, "Facebook session cookies imported successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to import Facebook cookies file.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val igCookieFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val success = AppSettings.importIgCookiesFromUri(context, uri)
            if (success) {
                Toast.makeText(context, "Instagram session cookies imported successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to import Instagram cookies file.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val appVersionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    val storageInfo = remember { AppSettings.getStorageInfo(context) }

    AmbientGlowBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 140.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                Text(
                    text = "v$appVersionName",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Card 1: Engine & OTA Core Updates
            SectionHeader(title = "Engine & Updates")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                    .background(DigitalIrisSecondary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Terminal,
                                    contentDescription = null,
                                    tint = DigitalIrisSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "yt-dlp Core Engine",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "Version: $engineVersion",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        Text(
                            text = if (isCheckingUpdate) "Checking..." else "Active",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = PrismaticMintTertiary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        onClick = {
                            if (!isCheckingUpdate) {
                                AppSettings.checkForEngineUpdate(context) { resultMsg ->
                                    Toast.makeText(context, resultMsg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isCheckingUpdate,
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isCheckingUpdate) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = ObsidianPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Checking for engine updates...",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Sync,
                                    contentDescription = null,
                                    tint = TextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Check for Engine Updates",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Card 2: Social Authentication & Cookies (Single Unified Card)
            SectionHeader(title = "Social Authentication & Cookies")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 1. YouTube Authentication
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF0000).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_youtube),
                                    contentDescription = "YouTube",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "YouTube Session",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = cookiesStatus,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        Text(
                            text = if (hasCookies) "Active" else "Public",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (hasCookies) Color(0xFFFF0000) else TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action 1: Sign In via YouTube (Primary)
                    Surface(
                        onClick = { showLoginDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Login,
                                    contentDescription = null,
                                    tint = Color(0xFFFF0000),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Sign In via YouTube",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                            }

                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action 2: Import cookies.txt with Help Guide on right
                    Surface(
                        onClick = {
                            cookieFilePicker.launch(arrayOf("text/*", "*/*"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.FileDownload,
                                    contentDescription = null,
                                    tint = Color(0xFFFF0000),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Import cookies.txt",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                            }

                            IconButton(
                                onClick = { showGuideDialog = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                                    contentDescription = "How to get cookies.txt",
                                    tint = Color(0xFFFF0000),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Action 3: Clear Cookies
                    if (hasCookies) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            onClick = {
                                AppSettings.clearCookies(context)
                                Toast.makeText(context, "YouTube session cookies cleared.", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFDE8E8),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFE02424),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Clear YouTube Cookies",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFE02424)
                                    )
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = Color.Black.copy(alpha = 0.06f)
                    )

                    // 2. Facebook Authentication
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1877F2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "f",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                                    modifier = Modifier.offset(x = 1.dp, y = (-1).dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Facebook Session",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = fbCookiesStatus,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        Text(
                            text = if (hasFbCookies) "Active" else "Public",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (hasFbCookies) Color(0xFF1877F2) else TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        onClick = {
                            fbCookieFilePicker.launch(arrayOf("text/*", "*/*"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.FileDownload,
                                    contentDescription = null,
                                    tint = Color(0xFF1877F2),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Import Facebook cookies.txt",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                            }

                            IconButton(
                                onClick = { showFbGuideDialog = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                                    contentDescription = "How to get Facebook cookies.txt",
                                    tint = Color(0xFF1877F2),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (hasFbCookies) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            onClick = {
                                AppSettings.clearFbCookies(context)
                                Toast.makeText(context, "Facebook session cookies cleared.", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFDE8E8),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFE02424),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Clear Facebook Cookies",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFE02424)
                                    )
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = Color.Black.copy(alpha = 0.06f)
                    )

                    // 3. Instagram Authentication
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
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
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Instagram Session",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = igCookiesStatus,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        Text(
                            text = if (hasIgCookies) "Active" else "Public",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (hasIgCookies) Color(0xFFE1306C) else TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        onClick = {
                            igCookieFilePicker.launch(arrayOf("text/*", "*/*"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.FileDownload,
                                    contentDescription = null,
                                    tint = Color(0xFFE1306C),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Import Instagram cookies.txt",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                )
                            }

                            IconButton(
                                onClick = { showIgGuideDialog = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                                    contentDescription = "How to get Instagram cookies.txt",
                                    tint = Color(0xFFE1306C),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (hasIgCookies) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            onClick = {
                                AppSettings.clearIgCookies(context)
                                Toast.makeText(context, "Instagram session cookies cleared.", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFDE8E8),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFE02424),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Clear Instagram Cookies",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFE02424)
                                    )
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = Color.Black.copy(alpha = 0.06f)
                    )

                    // 4. Hardware Encrypted Keystore & Auto-Deletion Notice (Nested at bottom inside main card)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = DigitalIrisSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Hardware Encrypted: All session cookies are encrypted with Android Keystore and automatically deleted after 30 days for your security.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Card 3: Downloads & Storage
            SectionHeader(title = "Downloads & Storage")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    // Wi-Fi Only Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Wifi,
                                contentDescription = null,
                                tint = DigitalIrisSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Download over Wi-Fi only",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "Pauses downloads when on cellular data",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Switch(
                            checked = wifiOnlyEnabled,
                            onCheckedChange = { AppSettings.setWifiOnly(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ObsidianPrimary
                            )
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.5f), thickness = 0.8.dp)

                    // Auto-Embed Metadata Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = DigitalIrisSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Auto-Embed Media Tags & Art",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "Embeds thumbnails and artist tags via FFmpeg",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Switch(
                            checked = autoEmbedMetadata,
                            onCheckedChange = { AppSettings.setAutoEmbedMetadata(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ObsidianPrimary
                            )
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.5f), thickness = 0.8.dp)

                    // Auto-Switch to Queue Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Inbox,
                                contentDescription = null,
                                tint = DigitalIrisSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Auto-Switch to Queue",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "Navigate to queue screen immediately when a download starts",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Switch(
                            checked = autoSwitchToQueue,
                            onCheckedChange = { AppSettings.setAutoSwitchToQueue(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ObsidianPrimary
                            )
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.5f), thickness = 0.8.dp)

                    // Parallel Downloads Toggle (Prominently featured in Downloads & Storage)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DynamicFeed,
                                contentDescription = null,
                                tint = DigitalIrisSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Parallel Downloads",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "Download multiple media files simultaneously",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Switch(
                            checked = isParallelDownloads,
                            onCheckedChange = { AppSettings.setParallelDownloads(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ObsidianPrimary
                            )
                        )
                    }

                    // Max Concurrent Downloads Selector (Visible when parallel downloads enabled)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isParallelDownloads,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Maximum Concurrent Tasks",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        fontSize = 12.sp
                                    )
                                )
                                Text(
                                    text = "$maxParallelCount parallel",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = DigitalIrisSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(2, 3, 4, 5).forEach { count ->
                                    val isSelected = maxParallelCount == count
                                    Surface(
                                        onClick = { AppSettings.setMaxParallelCount(count) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) ObsidianPrimary else Color.White,
                                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "$count",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White else TextPrimary
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.5f), thickness = 0.8.dp)

                    // Picture-in-Picture (PiP) Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PictureInPictureAlt,
                                contentDescription = null,
                                tint = DigitalIrisSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Picture-in-Picture (PiP)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "Allow floating video window when navigating away",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Switch(
                            checked = isPipEnabled,
                            onCheckedChange = { AppSettings.setPipEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ObsidianPrimary
                            )
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.5f), thickness = 0.8.dp)

                    // Storage Location
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                AppSettings.openStorageFolder(context)
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = null,
                                tint = DigitalIrisSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Storage Location",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "${storageInfo.displayPath} • ${storageInfo.freeSpaceGb}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = "Open folder",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.5f), thickness = 0.8.dp)

                    // Clean Temporary Cache
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val (cleanedCount, freedBytes) = AppSettings.cleanTemporaryFiles(context)
                                val freedMb = freedBytes / (1024.0 * 1024.0)
                                val msg = if (cleanedCount > 0) {
                                    String.format(Locale.US, "Cleaned %d temporary files (%.1f MB freed)", cleanedCount, freedMb)
                                } else {
                                    "No orphaned temporary files found."
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = null,
                                tint = DigitalIrisSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Clean Temporary Fragments",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "Purges incomplete .part, .ytdl, and cache fragments",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = "Clean cache",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Card 4: About & Legal
            SectionHeader(title = "About & Legal")
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onLicensesClick)
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = DigitalIrisSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Open Source Licenses",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "yt-dlp, FFmpeg, AndroidX, Jetpack Compose",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.5f), thickness = 0.8.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onTermsClick)
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Policy,
                                contentDescription = null,
                                tint = DigitalIrisSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Terms & Fair Use Disclaimer",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "For personal and educational archiving use",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.5f), thickness = 0.8.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onRevisitOnboardingClick)
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = DigitalIrisSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Welcome & Onboarding Tour",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "Revisit the app guide and introduction walkthrough",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.5f), thickness = 0.8.dp)

                    // Architecture & Build Diagnostics
                    val activeAbi = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
                    val isDebug = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
                    val buildTypeLabel = if (isDebug) "Debug" else "Optimized Release"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Terminal,
                                contentDescription = null,
                                tint = DigitalIrisSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Architecture & Optimization",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "v1.0 • $activeAbi • $buildTypeLabel",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = PrismaticMintTertiary.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrismaticMintTertiary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "ABI Split",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrismaticMintTertiary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }

            // Bottom clearance for floating navbar and mini player dock
            Spacer(modifier = Modifier.height(140.dp))
        }

        if (showLoginDialog) {
            YouTubeLoginDialog(
                onDismiss = { showLoginDialog = false },
                onSuccess = { showLoginDialog = false }
            )
        }

        if (showGuideDialog) {
            CookiesGuideDialog(
                onDismiss = { showGuideDialog = false }
            )
        }

        if (showFbGuideDialog) {
            FacebookCookiesGuideDialog(
                onDismiss = { showFbGuideDialog = false }
            )
        }

        if (showIgGuideDialog) {
            InstagramCookiesGuideDialog(
                onDismiss = { showIgGuideDialog = false }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            fontSize = 13.sp
        ),
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}
