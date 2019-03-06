package com.tughi.aggregator.activities.main

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.R
import com.tughi.aggregator.utilities.Favicons

internal sealed class EntriesFragmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    lateinit var entry: EntriesFragmentViewModel.Entry
        protected set

    open fun onBind(entry: EntriesFragmentViewModel.Entry) {
        this.entry = entry
    }

}

internal class EntriesFragmentDividerViewHolder(itemView: View) : EntriesFragmentViewHolder(itemView)

internal abstract class EntriesFragmentEntryViewHolder(itemView: View, private val listener: EntriesFragmentAdapterListener) : EntriesFragmentViewHolder(itemView) {

    val favicon: ImageView = itemView.findViewById(R.id.favicon)
    val title: TextView = itemView.findViewById(R.id.title)
    val feedTitle: TextView = itemView.findViewById(R.id.feed_title)
    val time: TextView = itemView.findViewById(R.id.time)
    val author: TextView = itemView.findViewById(R.id.author)
    val pin: View = itemView.findViewById(R.id.pin)

    init {
        itemView.setOnClickListener {
            listener.onEntryClicked(entry, adapterPosition / 2)
        }
    }

    override fun onBind(entry: EntriesFragmentViewModel.Entry) {
        super.onBind(entry)

        feedTitle.text = entry.feedTitle
        title.text = entry.title
        time.text = entry.formattedTime

        if (entry.author != null) {
            author.visibility = View.VISIBLE
            author.text = entry.author
        } else {
            author.visibility = View.GONE
        }

        pin.visibility = if (entry.pinnedTime > 0) View.VISIBLE else View.GONE

        Favicons.load(entry.feedId, entry.faviconUrl, favicon)
    }

}

internal class EntriesFragmentReadEntryViewHolder(itemView: View, listener: EntriesFragmentAdapterListener) : EntriesFragmentEntryViewHolder(itemView, listener)

internal class EntriesFragmentUnreadEntryViewHolder(itemView: View, listener: EntriesFragmentAdapterListener) : EntriesFragmentEntryViewHolder(itemView, listener)

internal class EntriesFragmentHeaderViewHolder(itemView: View) : EntriesFragmentViewHolder(itemView) {

    val header: TextView = itemView.findViewById(R.id.header)

    override fun onBind(entry: EntriesFragmentViewModel.Entry) {
        super.onBind(entry)

        header.text = entry.formattedDate
    }

}
