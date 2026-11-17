package com.leanwell.downloader.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.leanwell.downloader.MainActivity
import com.leanwell.downloader.model.ActiveDownload

class NotificationHelper(private val context: Context) {

    companion object {
        const val DOWNLOAD_CHANNEL_ID = "active_downloads_channel"
        const val COMPLETE_CHANNEL_ID = "completed_downloads_channel"
        const val FOREGROUND_NOTIFICATION_ID = 1001

        const val ACTION_PAUSE = "com.leanwell.downloader.ACTION_PAUSE"
        const val ACTION_RESUME = "com.leanwell.downloader.ACTION_RESUME"
        const val ACTION_CANCEL = "com.leanwell.downloader.ACTION_CANCEL"
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val downloadChannel = NotificationChannel(
                DOWNLOAD_CHANNEL_ID,
                "Active Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time progress of active media downloads"
                setShowBadge(false)
            }

            val completeChannel = NotificationChannel(
                COMPLETE_CHANNEL_ID,
                "Completed Downloads",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when a download has finished successfully"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(downloadChannel)
            notificationManager.createNotificationChannel(completeChannel)
        }
    }

    fun buildForegroundNotification(download: ActiveDownload?): Notification {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingLaunch = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingLaunch)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (download != null) {
            val percent = (download.progress * 100).toInt().coerceIn(0, 100)
            val isIndeterminate = download.progress <= 0f || download.progress >= 1f && download.speed.isEmpty()

            builder.setContentTitle(download.title)
                .setContentText(
                    if (download.statusMessage != null) {
                        download.statusMessage
                    } else {
                        "$percent% • ${download.speed} • ${download.eta}"
                    }
                )
                .setProgress(100, percent, isIndeterminate)

            // Cancel action
            val cancelIntent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_DOWNLOAD_ID, download.id)
            }
            val cancelPending = PendingIntent.getService(
                context,
                download.id.hashCode() + 1,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPending)
        } else {
            builder.setContentTitle("Downloader Engine")
                .setContentText("Queue is running...")
                .setProgress(0, 0, true)
        }

        return builder.build()
    }

    fun notifyCompleted(title: String, id: String) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingLaunch = PendingIntent.getActivity(
            context,
            id.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, COMPLETE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download Complete")
            .setContentText(title)
            .setContentIntent(pendingLaunch)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id.hashCode(), notification)
    }

    fun updateProgress(download: ActiveDownload) {
        notificationManager.notify(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification(download))
    }
}
