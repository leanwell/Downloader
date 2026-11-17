package com.leanwell.downloader.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.leanwell.downloader.model.NavDestination
import com.leanwell.downloader.ui.theme.DigitalIrisSecondary
import com.leanwell.downloader.ui.theme.ObsidianPrimary
import com.leanwell.downloader.ui.theme.TextMuted
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun FrostedBottomNavBar(
    selectedDestination: NavDestination,
    onDestinationSelected: (NavDestination) -> Unit,
    modifier: Modifier = Modifier,
    queueBadgeCount: Int = 0,
    hazeState: HazeState? = null
) {
    val destinations = NavDestination.entries
    val selectedIndex = destinations.indexOf(selectedDestination).coerceAtLeast(0)

    var startIndex by remember { mutableFloatStateOf(selectedIndex.toFloat()) }
    var targetIndex by remember { mutableFloatStateOf(selectedIndex.toFloat()) }

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "navIndicatorOffset"
    )

    LaunchedEffect(selectedIndex) {
        startIndex = animatedIndex
        targetIndex = selectedIndex.toFloat()
    }

    val navShape = RoundedCornerShape(24.dp)
    val diagonalBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.20f),
            Color.White.copy(alpha = 0.80f)
        ),
        start = Offset.Zero,
        end = Offset.Infinite
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(14.dp, navShape, ambientColor = Color(0x1F0F172A), spotColor = Color(0x1F0F172A))
            .clip(navShape)
            .then(
                if (hazeState != null) {
                    Modifier.hazeEffect(
                        state = hazeState,
                        style = HazeDefaults.style(
                            backgroundColor = Color.Transparent,
                            tint = HazeDefaults.tint(Color.White.copy(alpha = 0.55f)),
                            blurRadius = 32.dp
                        )
                    )
                } else {
                    Modifier.background(Color.White.copy(alpha = 0.88f))
                }
            )
            .border(1.5.dp, diagonalBorderBrush, navShape)
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val totalWidth = maxWidth
            val tabWidth = totalWidth / destinations.size

            // Liquid morphing indicator: Circle -> Oval Stretch -> Circle -> Resting Pill
            val totalDistance = abs(targetIndex - startIndex)
            val distanceToTarget = abs(targetIndex - animatedIndex)
            val progress = if (totalDistance > 0.01f) {
                (1f - (distanceToTarget / totalDistance)).coerceIn(0f, 1f)
            } else 1f

            val restingWidth = tabWidth - 8.dp
            val restingHeight = 48.dp
            val restingCorner = 16.dp
            val circleSize = 36.dp

            val currentWidth: androidx.compose.ui.unit.Dp
            val currentHeight: androidx.compose.ui.unit.Dp
            val currentCorner: androidx.compose.ui.unit.Dp

            if (progress >= 0.999f || totalDistance < 0.01f) {
                currentWidth = restingWidth
                currentHeight = restingHeight
                currentCorner = restingCorner
            } else if (progress < 0.25f) {
                // Phase 1: Contract from resting pill into circle centered on tab
                val t = (progress / 0.25f).coerceIn(0f, 1f)
                currentWidth = lerp(restingWidth, circleSize, t)
                currentHeight = lerp(restingHeight, circleSize, t)
                currentCorner = lerp(restingCorner, 18.dp, t)
            } else if (progress < 0.75f) {
                // Phase 2: Slide and stretch into horizontal oval
                val stretch = sin(((progress - 0.25f) / 0.50f) * Math.PI.toFloat()).coerceIn(0f, 1f)
                val maxOvalWidth = (tabWidth * 0.92f).coerceAtLeast(52.dp)
                val ovalHeight = 32.dp
                currentWidth = lerp(circleSize, maxOvalWidth, stretch)
                currentHeight = lerp(circleSize, ovalHeight, stretch)
                currentCorner = lerp(18.dp, 16.dp, stretch)
            } else {
                // Phase 3: Contract from oval/circle and bloom into resting pill at destination
                val t = ((progress - 0.75f) / 0.25f).coerceIn(0f, 1f)
                currentWidth = lerp(circleSize, restingWidth, t)
                currentHeight = lerp(circleSize, restingHeight, t)
                currentCorner = lerp(18.dp, restingCorner, t)
            }

            val centerX = (tabWidth * animatedIndex) + (tabWidth / 2)
            val leftOffset = centerX - (currentWidth / 2)
            val topOffset = (52.dp - currentHeight) / 2

            Box(
                modifier = Modifier
                    .offset(x = leftOffset, y = topOffset)
                    .width(currentWidth)
                    .height(currentHeight)
                    .clip(RoundedCornerShape(currentCorner))
                    .background(Color(0xFF0F172A).copy(alpha = 0.08f))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                destinations.forEach { destination ->
                    val isSelected = destination == selectedDestination
                    val interactionSource = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                onDestinationSelected(destination)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val iconColor = if (isSelected) ObsidianPrimary else TextMuted

                            if (destination == NavDestination.QUEUE && queueBadgeCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = DigitalIrisSecondary,
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = if (queueBadgeCount > 99) "99+" else queueBadgeCount.toString(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.label,
                                        tint = iconColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label,
                                    tint = iconColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Text(
                                text = destination.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = iconColor
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
