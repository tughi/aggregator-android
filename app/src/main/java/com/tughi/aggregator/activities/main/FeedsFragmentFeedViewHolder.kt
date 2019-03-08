package com.tughi.aggregator.activities.main

import android.content.Intent
import android.text.format.DateUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.feedsettings.FeedSettingsActivity
import com.tughi.aggregator.services.AutoUpdateScheduler
import com.tughi.aggregator.utilities.Favicons

internal sealed class FeedsFragmentFeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val favicon: ImageView = itemView.findViewById(R.id.favicon)
    val title: TextView = itemView.findViewById(R.id.title)
    val count: TextView = itemView.findViewById(R.id.count)
    val toggle: View = itemView.findViewById(R.id.toggle)

    lateinit var feed: FeedsFragmentViewModel.Feed

    open fun onBind(feed: FeedsFragmentViewModel.Feed) {
        this.feed = feed

        title.text = feed.title
        if (feed.unreadEntryCount == 0) {
            count.text = ""
            count.visibility = View.INVISIBLE
        } else {
            count.text = feed.unreadEntryCount.toString()
            count.visibility = View.VISIBLE
        }

        if (feed.updating) {
            favicon.setImageResource(R.drawable.action_refresh)
        } else {
            Favicons.load(feed.id, feed.faviconUrl, favicon)
        }
    }

}

internal class FeedsFragmentCollapsedFeedViewHolder(itemView: View) : FeedsFragmentFeedViewHolder(itemView) {

    val toggleImageView: ImageView = itemView.findViewById(R.id.toggle)

    override fun onBind(feed: FeedsFragmentViewModel.Feed) {
        super.onBind(feed)

        if (feed.nextUpdateRetry > 2) {
            toggleImageView.setImageResource(R.drawable.action_warning)
        } else {
            toggleImageView.setImageResource(R.drawable.action_show_more)
        }
    }

}

internal class FeedsFragmentExpandedFeedViewHolder(itemView: View) : FeedsFragmentFeedViewHolder(itemView) {

    val lastUpdateTime: TextView = itemView.findViewById(R.id.last_update_time)
    val lastUpdateError: TextView = itemView.findViewById(R.id.last_update_error)
    val nextUpdateTime: TextView = itemView.findViewById(R.id.next_update_time)
    val settingsButton: View = itemView.findViewById(R.id.settings)
    val updateButton: View = itemView.findViewById(R.id.update)

    init {
        settingsButton.setOnClickListener {
            val context = it.context
            context.startActivity(
                    Intent(context, FeedSettingsActivity::class.java)
                            .putExtra(FeedSettingsActivity.EXTRA_FEED_ID, feed.id)
            )
        }
    }

    override fun onBind(feed: FeedsFragmentViewModel.Feed) {
        super.onBind(feed)

        val context = itemView.context

        if (feed.lastUpdateTime == 0L) {
            lastUpdateTime.setText(R.string.feed_list_item__last_update_time__never)
        } else {
            DateUtils.getRelativeDateTimeString(context, feed.lastUpdateTime, DateUtils.DAY_IN_MILLIS, DateUtils.DAY_IN_MILLIS, 0).let {
                lastUpdateTime.text = context.getString(R.string.feed_list_item__last_update_time, it)
            }
        }

        when (feed.nextUpdateTime) {
            AutoUpdateScheduler.NEXT_UPDATE_TIME__DISABLED -> nextUpdateTime.setText(R.string.feed_list_item__next_update_time__disabled)
            AutoUpdateScheduler.NEXT_UPDATE_TIME__ON_APP_LAUNCH -> nextUpdateTime.setText(R.string.feed_list_item__next_update_time__on_app_launch)
            else -> DateUtils.getRelativeDateTimeString(context, feed.nextUpdateTime, DateUtils.DAY_IN_MILLIS, DateUtils.DAY_IN_MILLIS, 0).let {
                nextUpdateTime.text = context.getString(R.string.feed_list_item__next_update_time, it)
            }
        }

        if (feed.lastUpdateError != null) {
            lastUpdateError.visibility = View.VISIBLE
            lastUpdateError.text = context.getString(R.string.feed_list_item__last_update_error, feed.nextUpdateRetry, feed.lastUpdateError)
        } else {
            lastUpdateError.visibility = View.GONE
        }

        updateButton.isEnabled = !feed.updating
    }

}
