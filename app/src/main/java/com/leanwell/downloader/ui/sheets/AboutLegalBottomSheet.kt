package com.leanwell.downloader.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leanwell.downloader.ui.components.GlassCard
import com.leanwell.downloader.ui.theme.DigitalIrisSecondary
import com.leanwell.downloader.ui.theme.ObsidianPrimary
import com.leanwell.downloader.ui.theme.TextPrimary
import com.leanwell.downloader.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutLegalBottomSheet(
    initialTab: Int = 0, // 0: Licenses, 1: Terms
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current

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
                .padding(bottom = 24.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "About & Legal",
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

            Spacer(modifier = Modifier.height(12.dp))

            // Author & Project Identity Card (Leanwell 2026)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                uriHandler.openUri("https://leanwell.github.io/apps/")
                            } catch (e: Exception) { /* ignore */ }
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEEF2FF))
                                .border(1.dp, Color(0xFFC7D2FE), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Code,
                                contentDescription = null,
                                tint = ObsidianPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Leanwell 2026",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 15.sp
                                )
                            )
                            Text(
                                text = "leanwell.github.io/apps/",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = DigitalIrisSecondary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = "Open apps portal",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Segmented Tab Switcher (Licenses vs Terms)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEEF2FF))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 0: Open Source Licenses
                val isLicensesSelected = selectedTab == 0
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isLicensesSelected) ObsidianPrimary else Color.Transparent)
                        .clickable { selectedTab = 0 },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = if (isLicensesSelected) Color.White else TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Licenses",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isLicensesSelected) Color.White else TextSecondary
                            )
                        )
                    }
                }

                // Tab 1: Terms & Disclaimer
                val isTermsSelected = selectedTab == 1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isTermsSelected) ObsidianPrimary else Color.Transparent)
                        .clickable { selectedTab = 1 },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Policy,
                            contentDescription = null,
                            tint = if (isTermsSelected) Color.White else TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Terms & Disclaimer",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isTermsSelected) Color.White else TextSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectedTab == 0) {
                    // App License Recommendation Card
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "App License: GPL-3.0",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "GNU GPL v3.0",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = DigitalIrisSecondary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Standard open-source copyleft license for multimedia applications. Ensures all derivative works and modifications remain free and open-source, seamlessly integrates with the yt-dlp & FFmpeg ecosystem, and includes robust statutory limitation of liability for developers.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            )
                        }
                    }

                    // Open Source Engines List (yt-dlp, FFmpeg, aria2c)
                    LicenseItem(
                        name = "yt-dlp",
                        author = "yt-dlp Contributors",
                        license = "The Unlicense (Public Domain)",
                        description = "Command-line media extraction engine powering multi-platform video/audio analysis and retrieval."
                    )
                    LicenseItem(
                        name = "FFmpeg",
                        author = "FFmpeg Developers",
                        license = "LGPL v2.1+ / GPL",
                        description = "High-performance multimedia framework for audio transcoding, format conversion, and ID3 tagging."
                    )
                    LicenseItem(
                        name = "aria2",
                        author = "Tatsuhiro Tsujikawa",
                        license = "GNU GPL v2+",
                        description = "Multi-source & segmented accelerated download engine."
                    )
                } else {
                    // Third-Party Media Aggregator & Copyright Disclaimer
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = DigitalIrisSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Third-Party Media Aggregator Disclaimer",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Downloader is strictly a local client-side tool combining publicly available web extractors. The developer does NOT host, cache, distribute, stream, index, or store any copyrighted audio or video media on any server. All downloads are executed locally and directly on the user's device.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            )
                        }
                    }

                    // Strict Limitation of Liability
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = null,
                                    tint = DigitalIrisSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Limitation of Developer Liability",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "THIS APP IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND. UNDER NO CIRCUMSTANCES SHALL THE DEVELOPER BE LIABLE FOR ANY COPYRIGHT INFRINGEMENT, FINANCIAL LOSS, OR LEGAL CLAIMS ARISING FROM USER ACTIONS. End-users bear full and sole responsibility for ensuring their downloads comply with local copyright laws, intellectual property rights, and third-party terms of service.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            )
                        }
                    }

                    // No DRM Circumvention
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "No DRM Circumvention",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "This software does NOT circumvent, decrypt, or tamper with technological protection measures or DRM encryption. Content protected by Widevine, FairPlay, or other DRM schemes cannot be retrieved.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            )
                        }
                    }

                    // Personal Educational Archival & Fair Use
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Personal Educational & Archival Use",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Downloader is built solely for personal educational archiving, time-shifting, and offline research under applicable fair use doctrines. Commercial distribution, redistribution, or monetization of downloaded media without permission is strictly prohibited.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            )
                        }
                    }

                    // Privacy Guarantee
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Zero Tracking & Privacy Guarantee",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "All processing happens 100% on your device. No URLs, download history, search terms, or personal telemetry are collected, tracked, or sent to external servers.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
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
}

@Composable
private fun LicenseItem(
    name: String,
    author: String,
    license: String,
    description: String
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = license,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = DigitalIrisSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "By $author",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            )
        }
    }
}
