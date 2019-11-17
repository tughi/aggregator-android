package com.tughi.aggregator

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object Notifications {

    fun setupChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL__NEW_ENTRIES, context.getString(R.string.notification_channel__new_entries), NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(notificationChannel)

            for (otherNotificationChannel in notificationManager.notificationChannels) {
                if (otherNotificationChannel.id != notificationChannel.id) {
                    notificationManager.deleteNotificationChannel(otherNotificationChannel.id)
                }
            }
        }
    }

}
