package com.leanwell.downloader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leanwell.downloader.ui.components.AmbientGlowBackground
import com.leanwell.downloader.ui.components.ObsidianButton
import com.leanwell.downloader.ui.theme.DigitalIrisSecondary
import com.leanwell.downloader.ui.theme.TextMuted
import com.leanwell.downloader.ui.theme.TextPrimary
import com.leanwell.downloader.ui.theme.TextSecondary

@Composable
fun OnboardingScreen(
    onGetStartedClick: () -> Unit,
    onTermsClick: () -> Unit = {},
    onPrivacyPolicyClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AmbientGlowBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Center Visual Showcase: Frosted Glass Badge & Typographic Hero
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                // Luminous Frosted Glass Badge (112 x 112 dp)
                val outerBg = Color.White.copy(alpha = 0.72f)
                val outerBorder = Color.White.copy(alpha = 0.85f)

                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = CircleShape,
                            ambientColor = Color(0x1F4854BB),
                            spotColor = Color(0x1F4854BB)
                        )
                        .clip(CircleShape)
                        .background(outerBg)
                        .border(1.5.dp, outerBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner Circle
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
                            contentDescription = "Fast Download",
                            tint = TextPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Hero Headline
                Text(
                    text = "Universal Media\nDownloader.",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        fontSize = 32.sp,
                        lineHeight = 40.sp,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Value Proposition Main Description
                Text(
                    text = "Effortlessly grab and convert audio & video in seconds from YouTube, Spotify, TikTok, Instagram, Facebook, and 1,000+ sites.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = TextSecondary,
                        fontSize = 15.sp,
                        lineHeight = 23.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }

            // Call to Actions & Trust Footnote
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ObsidianButton(
                    text = "Get Started",
                    onClick = onGetStartedClick,
                    modifier = Modifier.fillMaxWidth(),
                    height = 56.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Terms & Privacy Policy Clickable Text
                val footnoteAnnotated = buildAnnotatedString {
                    append("By using this application you agree to our ")
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = "TERMS",
                            linkInteractionListener = { onTermsClick() }
                        )
                    ) {
                        withStyle(
                            SpanStyle(
                                color = DigitalIrisSecondary,
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append("Terms & Conditions")
                        }
                    }
                    append(" and ")
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = "PRIVACY",
                            linkInteractionListener = { onPrivacyPolicyClick() }
                        )
                    ) {
                        withStyle(
                            SpanStyle(
                                color = DigitalIrisSecondary,
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append("Privacy Policy")
                        }
                    }
                }

                Text(
                    text = footnoteAnnotated,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
