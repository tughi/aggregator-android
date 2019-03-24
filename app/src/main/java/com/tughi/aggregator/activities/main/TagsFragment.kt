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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
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

        val tagsAdapterListener = object : TagsAdapter.Listener {
            override fun onToggleTag(tag: Tag) {
                viewModel.toggleTag(tag)
            }
        }

        val tagsAdapter = TagsAdapter(tagsAdapterListener).also { tagsRecyclerView.adapter = it }
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
            val count: Int,
            val expanded: Boolean = false
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
        private val databaseTags = Tags.liveQuery(Tags.VisibleTagsQueryCriteria, Tag.QueryHelper)
        private val expandedTagId = MutableLiveData<Long>()

        val tags: LiveData<List<Tag>>

        init {
            val tags = MediatorLiveData<List<Tag>>()
            tags.addSource(databaseTags) { tags.value = computeTags(it ?: emptyList(), expandedTagId.value) }
            tags.addSource(expandedTagId) { tags.value = computeTags(databaseTags.value ?: emptyList(), it) }
            this.tags = tags
        }

        private fun computeTags(tags: List<Tag>, expandedTagId: Long?): List<Tag> {
            if (tags.isEmpty() || expandedTagId == null) {
                return tags
            }
            return tags.map {
                if (it.id == expandedTagId) {
                    it.copy(expanded = true)
                } else {
                    it
                }
            }
        }

        fun toggleTag(tag: Tag) {
            expandedTagId.value = if (expandedTagId.value == tag.id) null else tag.id
        }
    }

    object TagDiffUtil : DiffUtil.ItemCallback<Tag>() {
        override fun areItemsTheSame(oldItem: Tag, newItem: Tag) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Tag, newItem: Tag) = oldItem == newItem
    }

    class TagsAdapter(val listener: Listener) : ListAdapter<Tag, TagsAdapter.ViewHolder>(TagDiffUtil) {
        override fun getItemViewType(position: Int): Int {
            val tag = getItem(position)
            return if (tag.expanded) R.layout.tags_item_expanded else R.layout.tags_item_collapsed
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                        viewType,
                        parent,
                        false
                ),
                listener
        )

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val tag = getItem(position)

            holder.tag = tag

            holder.favicon.setImageResource(when (tag.id) {
                Tags.STARRED -> R.drawable.favicon_star
                else -> R.drawable.favicon_tag
            })
            holder.name.text = tag.name
            holder.count.text = if (tag.count > 0) tag.count.toString() else ""
        }

        interface Listener {
            fun onToggleTag(tag: Tag)
        }

        class ViewHolder(itemView: View, listener: Listener) : RecyclerView.ViewHolder(itemView) {
            var tag: Tag? = null

            val favicon: ImageView = itemView.findViewById(R.id.favicon)
            val name: TextView = itemView.findViewById(R.id.name)
            val count: TextView = itemView.findViewById(R.id.count)

            init {
                itemView.findViewById<View>(R.id.toggle).setOnClickListener {
                    tag?.let {
                        listener.onToggleTag(it)
                    }
                }
            }
        }
    }

}
