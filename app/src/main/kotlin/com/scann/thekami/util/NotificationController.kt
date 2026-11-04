package com.scann.thekami.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.scann.thekami.MainActivity
import com.scann.thekami.R

object NotificationController {
    private const val channelId = "scann_shortcuts"
    private const val notificationId = 1001
    private const val preferenceName = "scann_preferences"
    private const val createNotificationKey = "create_notification_enabled"

    fun isCreateNotificationEnabled(context: Context): Boolean =
        context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
            .getBoolean(createNotificationKey, false)

    fun setCreateNotificationEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(createNotificationKey, enabled)
            .apply()
        if (enabled) showCreateNotification(context) else hideCreateNotification(context)
    }

    fun showCreateNotification(context: Context) {
        if (!canPostNotifications(context)) return
        createChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = android.net.Uri.parse("scann://create")
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            10,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_tile_create)
            .setContentTitle("Scann shortcuts")
            .setContentText("Create a QR code from your notification panel")
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_tile_create, "Create QR code", pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission may have been revoked after canPostNotifications().
        }
    }

    fun hideCreateNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    private fun canPostNotifications(context: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Scann shortcuts",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Quick actions for Scann"
                setShowBadge(false)
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}
