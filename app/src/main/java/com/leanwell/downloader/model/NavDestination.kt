package com.leanwell.downloader.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavDestination(
    val label: String,
    val icon: ImageVector
) {
    HOME("Home", Icons.Rounded.Home),
    QUEUE("Queue", Icons.Rounded.Inbox),
    LIBRARY("Library", Icons.Rounded.VideoLibrary),
    SETTINGS("Settings", Icons.Rounded.Settings)
}
