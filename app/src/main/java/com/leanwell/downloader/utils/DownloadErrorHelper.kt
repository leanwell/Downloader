package com.leanwell.downloader.utils

data class ErrorNotice(
    val title: String,
    val userFriendlyMessage: String,
    val isBotCheck: Boolean = false,
    val isAuthRequired: Boolean = false,
    val canRetry: Boolean = true,
    val rawDetails: String
)

object DownloadErrorHelper {

    fun parse(rawError: String?): ErrorNotice {
        val raw = rawError?.trim() ?: "Unknown error occurred"

        return when {
            raw.contains("Sign in to confirm you’re not a bot", ignoreCase = true) ||
            raw.contains("Sign in to confirm you're not a bot", ignoreCase = true) ||
            (raw.contains("bot", ignoreCase = true) && raw.contains("protect our community", ignoreCase = true)) -> {
                ErrorNotice(
                    title = "YouTube Bot Check Triggered",
                    userFriendlyMessage = "YouTube blocked this download. Sign in with YouTube in Settings or import cookies to bypass.",
                    isBotCheck = true,
                    isAuthRequired = true,
                    canRetry = true,
                    rawDetails = raw
                )
            }

            raw.contains("login to facebook", ignoreCase = true) ||
            raw.contains("log in to facebook", ignoreCase = true) ||
            (raw.contains("facebook", ignoreCase = true) && (raw.contains("private", ignoreCase = true) || raw.contains("restricted", ignoreCase = true) || raw.contains("login", ignoreCase = true))) -> {
                ErrorNotice(
                    title = "Private or Restricted Facebook Video",
                    userFriendlyMessage = "This Facebook video is private, deleted, or restricted by the creator. Downloader can only access public Facebook videos.",
                    isAuthRequired = false,
                    canRetry = false,
                    rawDetails = raw
                )
            }

            raw.contains("HTTP Error 403", ignoreCase = true) || raw.contains("Forbidden", ignoreCase = true) -> {
                ErrorNotice(
                    title = "Access Restricted (HTTP 403)",
                    userFriendlyMessage = "Stream access expired or blocked by YouTube. Active session cookies are required.",
                    isAuthRequired = true,
                    canRetry = true,
                    rawDetails = raw
                )
            }

            raw.contains("Private video", ignoreCase = true) ||
            raw.contains("members-only", ignoreCase = true) ||
            raw.contains("Sign in to confirm your age", ignoreCase = true) -> {
                ErrorNotice(
                    title = "Age-Restricted or Private Video",
                    userFriendlyMessage = "This video requires a signed-in YouTube account to view or download.",
                    isAuthRequired = true,
                    canRetry = true,
                    rawDetails = raw
                )
            }

            raw.contains("Unable to download webpage", ignoreCase = true) ||
            raw.contains("No address associated with hostname", ignoreCase = true) ||
            raw.contains("Network is unreachable", ignoreCase = true) ||
            raw.contains("timed out", ignoreCase = true) -> {
                ErrorNotice(
                    title = "Network Disconnected",
                    userFriendlyMessage = "Could not connect to media servers. Please verify your internet connection.",
                    canRetry = true,
                    rawDetails = raw
                )
            }

            raw.contains("Waiting for Wi-Fi", ignoreCase = true) -> {
                ErrorNotice(
                    title = "Waiting for Wi-Fi",
                    userFriendlyMessage = "Downloads are set to Wi-Fi only. Connect to Wi-Fi or turn off Wi-Fi Only in Settings.",
                    rawDetails = raw
                )
            }

            else -> {
                val cleanLine = raw.lineSequence()
                    .firstOrNull { it.contains("ERROR:", ignoreCase = true) }
                    ?.substringAfter("ERROR:")
                    ?.trim()
                    ?: raw.lineSequence().firstOrNull()?.trim()
                    ?: "An unexpected error disrupted this download."

                ErrorNotice(
                    title = "Download Disrupted",
                    userFriendlyMessage = cleanLine.take(160),
                    canRetry = true,
                    rawDetails = raw
                )
            }
        }
    }
}
