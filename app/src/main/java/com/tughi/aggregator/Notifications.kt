package com.tughi.aggregator

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tughi.aggregator.activities.notifications.NewEntriesActivity
import com.tughi.aggregator.data.Entries
import com.tughi.aggregator.data.MyFeedEntriesQueryCriteria
import com.tughi.aggregator.data.UnreadEntriesQueryCriteria
import com.tughi.aggregator.preferences.MyFeedSettings
import com.tughi.aggregator.preferences.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object Notifications {

    private val scope = CoroutineScope(Dispatchers.Main)

    fun setupChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL__MY_FEED, context.getString(R.string.notification_channel__my_feed), NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(notificationChannel)

            for (otherNotificationChannel in notificationManager.notificationChannels) {
                if (otherNotificationChannel.id != notificationChannel.id) {
                    notificationManager.deleteNotificationChannel(otherNotificationChannel.id)
                }
            }
        }
    }

    fun refreshNewEntriesNotification(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)

        if (MyFeedSettings.notification && notificationManager.channelImportance(NOTIFICATION_CHANNEL__MY_FEED) > NotificationManagerCompat.IMPORTANCE_NONE) {
            contentScope.launch {
                val entriesQueryCriteria = MyFeedEntriesQueryCriteria(minInsertTime = User.lastSeen, sessionTime = 0L, showRead = false, sortOrder = Entries.SortOrder.ByDateAscending)
                val count = Entries.queryCount(UnreadEntriesQueryCriteria(entriesQueryCriteria), Count.QueryHelper)

                launch(Dispatchers.Main) {
                    if (count > 0) {
                        val intent = Intent(context, NewEntriesActivity::class.java)

                        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL__MY_FEED)
                            .setSmallIcon(R.drawable.notification)
                            .setColor(App.accentColor)
                            .setContentTitle(context.resources.getQuantityString(R.plurals.notification__my_feed__new_entries, count, count))
                            .setContentText(context.resources.getQuantityString(R.plurals.notification__new_entries__tap, count))
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setContentIntent(PendingIntent.getActivity(context, 0, intent, 0))
                            .setWhen(System.currentTimeMillis())
                            .setAutoCancel(true)
                            .build()

                        notificationManager.notify(NOTIFICATION__NEW_ENTRIES__MY_FEED, notification)
                    } else {
                        notificationManager.cancel(NOTIFICATION__NEW_ENTRIES__MY_FEED)
                    }
                }
            }
        }
    }

    fun NotificationManagerCompat.channelImportance(name: String): Int {
        if (Build.VERSION.SDK_INT < 26) {
            return NotificationManagerCompat.IMPORTANCE_DEFAULT
        }
        if (importance == NotificationManagerCompat.IMPORTANCE_NONE) {
            return NotificationManagerCompat.IMPORTANCE_NONE
        }
        val channel = getNotificationChannel(name) ?: return NotificationManagerCompat.IMPORTANCE_DEFAULT
        return channel.importance
    }

    class Count {
        object QueryHelper : Entries.QueryHelper<Count>() {
            override fun createRow(cursor: Cursor) = Count()
        }
    }

}
