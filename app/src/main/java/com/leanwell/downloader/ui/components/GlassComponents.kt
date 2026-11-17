package com.leanwell.downloader.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leanwell.downloader.ui.theme.GlowIris
import com.leanwell.downloader.ui.theme.GlowMist
import com.leanwell.downloader.ui.theme.GlowSoft
import com.leanwell.downloader.ui.theme.ObsidianPrimary

@Composable
fun AmbientGlowBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val baseBg = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top-left soft lavender-iris glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GlowIris.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(w * 0.05f, h * 0.05f),
                    radius = w * 0.7f
                ),
                center = Offset(w * 0.05f, h * 0.05f),
                radius = w * 0.7f
            )

            // Center-right soft mist glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GlowMist.copy(alpha = 0.40f), Color.Transparent),
                    center = Offset(w * 0.95f, h * 0.35f),
                    radius = w * 0.8f
                ),
                center = Offset(w * 0.95f, h * 0.35f),
                radius = w * 0.8f
            )

            // Bottom soft ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GlowSoft.copy(alpha = 0.60f), Color.Transparent),
                    center = Offset(w * 0.3f, h * 0.85f),
                    radius = w * 0.6f
                ),
                center = Offset(w * 0.3f, h * 0.85f),
                radius = w * 0.6f
            )
        }

        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 4.dp,
    borderAlpha: Float = 0.75f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val bgColor = Color.White.copy(alpha = 0.82f)
    val borderColor = Color.White.copy(alpha = borderAlpha)
    val shadowColor = Color(0xFF334155).copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .then(if (elevation > 0.dp) Modifier.shadow(elevation, shape, ambientColor = shadowColor, spotColor = shadowColor) else Modifier)
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        content()
    }
}

@Composable
fun ObsidianButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = ObsidianPrimary,
        contentColor = Color.White,
        shadowElevation = 6.dp,
        modifier = modifier
            .heightIn(min = height)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(start = 8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                )
            }
        }
    }
}
