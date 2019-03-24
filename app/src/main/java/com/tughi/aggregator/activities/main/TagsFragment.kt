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
import com.tughi.aggregator.activities.tagsettings.TagSettingsActivity
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
            override fun onSettingsClicked(tag: Tag) {
                context?.let { context ->
                    TagSettingsActivity.start(context, tag.id)
                }
            }

            override fun onTagClicked(tag: Tag) {
                fragmentManager!!.beginTransaction()
                        .setCustomAnimations(R.anim.slide_in, 0, 0, R.anim.fade_out)
                        .add(id, TagEntriesFragment.newInstance(tagId = tag.id), "tag")
                        .addToBackStack(null)
                        .commit()
            }

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
        toolbar.inflateMenu(R.menu.tags_fragment)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.add -> {
                    context?.let { context ->
                        TagSettingsActivity.start(context, null)
                    }

                    return@setOnMenuItemClickListener true
                }
            }
            return@setOnMenuItemClickListener false
        }

        return fragmentView
    }

    data class Tag(
            val id: Long,
            val name: String,
            val editable: Boolean,
            val totalEntryCount: Int,
            val unreadEntryCount: Int,
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
                    totalEntryCount = cursor.getInt(3),
                    unreadEntryCount = cursor.getInt(4)
            )
        }
    }

    class TagsViewModel : ViewModel() {
        private val databaseTags = Tags.liveQuery(Tags.QueryVisibleTagsCriteria, Tag.QueryHelper)
        private val expandedTagId = MutableLiveData<Long>()

        val tags: LiveData<List<Tag>>

        init {
            val tags = MediatorLiveData<List<Tag>>()
            tags.addSource(databaseTags) { tags.value = transform(it ?: emptyList(), expandedTagId.value) }
            tags.addSource(expandedTagId) { tags.value = transform(databaseTags.value ?: emptyList(), it) }
            this.tags = tags
        }

        private fun transform(tags: List<Tag>, expandedTagId: Long?): List<Tag> {
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

    class TagsAdapter(val listener: Listener) : ListAdapter<Tag, TagsAdapter.TagViewHolder>(TagDiffUtil) {
        override fun getItemViewType(position: Int) = when {
            getItem(position).expanded -> R.layout.tags_item_expanded
            else -> R.layout.tags_item_collapsed
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
            R.layout.tags_item_expanded -> ExpandedTagViewHolder(LayoutInflater.from(parent.context).inflate(viewType, parent, false), listener)
            else -> TagViewHolder(LayoutInflater.from(parent.context).inflate(viewType, parent, false), listener)
        }

        override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
            holder.onBind(getItem(position))
        }

        interface Listener {
            fun onSettingsClicked(tag: Tag)

            fun onTagClicked(tag: Tag)

            fun onToggleTag(tag: Tag)
        }

        open class TagViewHolder(itemView: View, listener: Listener) : RecyclerView.ViewHolder(itemView) {
            protected var tag: Tag? = null

            private val favicon: ImageView = itemView.findViewById(R.id.favicon)
            private val name: TextView = itemView.findViewById(R.id.name)
            private val count: TextView = itemView.findViewById(R.id.count)

            init {
                itemView.setOnClickListener {
                    tag?.let {
                        listener.onTagClicked(it)
                    }
                }

                itemView.findViewById<View>(R.id.toggle).setOnClickListener {
                    tag?.let {
                        listener.onToggleTag(it)
                    }
                }
            }

            open fun onBind(tag: Tag) {
                this.tag = tag

                name.text = tag.name
                if (tag.id == Tags.STARRED) {
                    favicon.setImageResource(R.drawable.favicon_star)
                    count.text = if (tag.totalEntryCount > 0) tag.totalEntryCount.toString() else ""
                } else {
                    favicon.setImageResource(R.drawable.favicon_tag)
                    count.text = if (tag.unreadEntryCount > 0) tag.unreadEntryCount.toString() else ""
                }
            }
        }

        class ExpandedTagViewHolder(itemView: View, listener: Listener) : TagViewHolder(itemView, listener) {
            private val totalEntries: TextView = itemView.findViewById(R.id.total_entries)
            private val unreadEntries: TextView = itemView.findViewById(R.id.unread_entries)

            init {
                itemView.findViewById<View>(R.id.settings).setOnClickListener {
                    tag?.let {
                        listener.onSettingsClicked(it)
                    }
                }
            }

            override fun onBind(tag: Tag) {
                super.onBind(tag)

                totalEntries.text = itemView.context.getString(R.string.tags_item__total_entries, tag.totalEntryCount)
                unreadEntries.text = itemView.context.getString(R.string.tags_item__unread_entries, tag.unreadEntryCount)
            }
        }
    }

}
