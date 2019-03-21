package com.tughi.aggregator.activities.main

import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.R
import com.tughi.aggregator.data.Tags

class TagsFragment : Fragment() {

    companion object {
        fun newInstance(): TagsFragment {
            return TagsFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = inflater.inflate(R.layout.tags_fragment, container, false)

        val viewModel = ViewModelProviders.of(this).get(TagsViewModel::class.java)

        val tagsRecyclerView = fragmentView.findViewById<RecyclerView>(R.id.tags)
        val progressBar = fragmentView.findViewById<ProgressBar>(R.id.progress)

        val tagsAdapter = TagsAdapter().also { tagsRecyclerView.adapter = it }
        viewModel.tags.observe(this, Observer { tags ->
            progressBar.visibility = if (tags != null) View.GONE else View.VISIBLE
            tagsAdapter.submitList(tags)
        })

        val toolbar = fragmentView.findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            val activity = activity as MainActivity
            activity.openDrawer()
        }

        return fragmentView
    }

    data class Tag(
            val id: Long,
            val name: String,
            val editable: Boolean,
            val count: Int
    ) {
        object QueryHelper : Tags.QueryHelper<Tag>(
                Tags.ID,
                Tags.NAME,
                Tags.EDITABLE,
                Tags.ENTRY_COUNT,
                Tags.UNREAD_ENTRY_COUNT
        ) {
            override fun createRow(cursor: Cursor) = Tag(
                    id = cursor.getLong(0),
                    name = cursor.getString(1),
                    editable = cursor.getInt(2) != 0,
                    count = if (cursor.getLong(0) == Tags.STARRED) cursor.getInt(3) else cursor.getInt(4)
            )
        }
    }

    class TagsViewModel : ViewModel() {
        val tags = Tags.liveQuery(Tags.VisibleTagsQueryCriteria, Tag.QueryHelper)
    }

    class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val favicon: ImageView = itemView.findViewById(R.id.favicon)
        val name: TextView = itemView.findViewById(R.id.name)
        val count: TextView = itemView.findViewById(R.id.count)
    }

    object TagDiffUtil : DiffUtil.ItemCallback<Tag>() {
        override fun areItemsTheSame(oldItem: Tag, newItem: Tag) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Tag, newItem: Tag) = oldItem == newItem
    }

    class TagsAdapter : ListAdapter<Tag, TagViewHolder>(TagDiffUtil) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TagViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.tags_item_collapsed, parent, false)
        )

        override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
            val tag = getItem(position)

            holder.favicon.setImageResource(when (tag.id) { // TODO: use favicon drawables
                Tags.STARRED -> R.drawable.action_star
                else -> R.drawable.action_tags
            })
            holder.name.text = tag.name
            holder.count.text = if (tag.count > 0) tag.count.toString() else ""
        }
    }

}
