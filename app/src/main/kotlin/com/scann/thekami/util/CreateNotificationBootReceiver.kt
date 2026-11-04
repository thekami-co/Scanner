package com.scann.thekami.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CreateNotificationBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED &&
            NotificationController.isCreateNotificationEnabled(context)
        ) {
            NotificationController.showCreateNotification(context)
        }
    }
}
