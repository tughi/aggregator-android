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
import androidx.core.content.res.ResourcesCompat
import com.tughi.aggregator.activities.main.MainActivity
import com.tughi.aggregator.data.Entries
import com.tughi.aggregator.data.MyFeedEntriesQueryCriteria
import com.tughi.aggregator.data.UnreadEntriesQueryCriteria
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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

    fun refreshNewEntriesNotification(context: Context) {
        GlobalScope.launch {
            val entriesQueryCriteria = MyFeedEntriesQueryCriteria(0L, true, sortOrder = Entries.SortOrder.ByDateAscending)
            val count = Entries.queryCount(UnreadEntriesQueryCriteria(entriesQueryCriteria), Count.QueryHelper)

            launch(Dispatchers.Main) {
                if (count > 0) {
                    val intent = Intent(context, MainActivity::class.java)

                    val accentColor = ResourcesCompat.getColor(context.resources,
                            when (App.style.value!!.accent) {
                                App.Style.Accent.BLUE -> R.color.accent__blue
                                App.Style.Accent.GREEN -> R.color.accent__green
                                App.Style.Accent.ORANGE -> R.color.accent__orange
                                App.Style.Accent.PURPLE -> R.color.accent__purple
                                App.Style.Accent.RED -> R.color.accent__red
                            },
                            null)

                    val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL__NEW_ENTRIES)
                            .setSmallIcon(R.drawable.notification)
                            .setColor(accentColor)
                            .setContentTitle(context.resources.getQuantityString(R.plurals.notification__new_entries, count, count))
                            .setContentText(context.resources.getQuantityString(R.plurals.notification__new_entries__tap, count))
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setContentIntent(PendingIntent.getActivity(context, 0, intent, 0))
                            // TODO: .setDeleteIntent()
                            .setWhen(System.currentTimeMillis())
                            .setAutoCancel(true)
                            .build()

                    NotificationManagerCompat.from(context).notify(NOTIFICATION__NEW_ENTRIES, notification)
                } else {
                    NotificationManagerCompat.from(context).cancel(NOTIFICATION__NEW_ENTRIES)
                }
            }
        }
    }

    data class Count(val unreadEntries: Int) {
        object QueryHelper : Entries.QueryHelper<Count>() {
            override fun createRow(cursor: Cursor) = Count(0)
        }
    }
}
