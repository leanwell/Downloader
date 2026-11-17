package com.leanwell.downloader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = ObsidianPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF181C22),
    onPrimaryContainer = Color(0xFF80848C),
    secondary = DigitalIrisSecondary,
    onSecondary = Color.White,
    secondaryContainer = DigitalIrisContainer,
    onSecondaryContainer = Color(0xFF16238E),
    tertiary = PrismaticMintTertiary,
    onTertiary = Color.White,
    background = CanvasBackground,
    onBackground = TextPrimary,
    surface = CanvasSurface,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFD9E3F4),
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF76777B),
    outlineVariant = Color(0xFFC6C6CB)
)

/**
 * Downloader App Theme
 * Locked strictly to Light Mode per application design rules.
 */
@Composable
fun DownloaderTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
