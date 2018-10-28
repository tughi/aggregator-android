package com.tughi.aggregator

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.data.Database
import com.tughi.aggregator.data.UiEntriesGetter
import com.tughi.aggregator.data.UiEntry
import com.tughi.aggregator.viewmodels.EntryListViewModel
import org.jetbrains.anko.doAsync

abstract class EntryListFragment : Fragment() {

    private lateinit var toolbar: Toolbar

    protected val sessionTime = System.currentTimeMillis()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = inflater.inflate(R.layout.entry_list_fragment, container, false)

        val viewModelFactory = EntryListViewModel.Factory(getUiEntriesGetter())
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(EntryListViewModel::class.java)

        val entriesRecyclerView = fragmentView.findViewById<RecyclerView>(R.id.entries)
        val progressBar = fragmentView.findViewById<ProgressBar>(R.id.progress)

        entriesRecyclerView.adapter = EntriesAdapter().also { adapter ->
            viewModel.entries.observe(this, Observer { entries ->
                adapter.submitList(entries)

                progressBar.visibility = View.GONE
            })
        }
        ItemTouchHelper(SwipeItemTouchHelper()).attachToRecyclerView(entriesRecyclerView)

        toolbar = fragmentView.findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationIcon(when (this) {
            is MyFeedFragment -> R.drawable.action_menu
            else -> R.drawable.action_back
        })
        toolbar.setNavigationOnClickListener { onNavigationClick() }

        return fragmentView
    }

    abstract fun getUiEntriesGetter(): UiEntriesGetter

    abstract fun onNavigationClick()

    protected fun setTitle(@StringRes title: Int) {
        toolbar.setTitle(title)
    }

    protected fun setTitle(title: String) {
        toolbar.title = title
    }

}

class EntryListRecyclerView(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs) {

    private val headerView = LayoutInflater.from(context).inflate(R.layout.entry_list_header, FrameLayout(context), false)
    private val headerTextView: TextView = headerView.findViewById(R.id.header)

    private var headerText: String? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (w > 0) {
            // update the overlay layout
            val widthMeasureSpec = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY)
            val heightMeasureSpec = MeasureSpec.makeMeasureSpec(h, MeasureSpec.UNSPECIFIED)
            headerView.measure(widthMeasureSpec, heightMeasureSpec)
            headerView.layout(0, 0, headerView.measuredWidth, headerView.measuredHeight)
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        val childCount = childCount
        if (childCount > 0) {
            val firstChild = getChildAt(0)
            val firstChildViewHolder = getChildViewHolder(firstChild)
            if (firstChildViewHolder is EntryListItemViewHolder) {
                // update overlay text
                val firstChildHeaderText = firstChildViewHolder.entry.formattedDate.toString()
                if (headerText != firstChildHeaderText) {
                    headerText = firstChildHeaderText
                    headerTextView.text = headerText

                    headerView.measure(MeasureSpec.makeMeasureSpec(headerView.width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(headerView.height, MeasureSpec.EXACTLY))
                    headerView.layout(0, 0, headerView.width, headerView.height)
                }

                // draw overlay
                canvas.save()
                if (childCount > 1) {
                    val secondChild = getChildAt(1)
                    val secondChildViewHolder = getChildViewHolder(secondChild) as EntryListItemViewHolder
                    val secondChildSection = secondChildViewHolder.entry.formattedDate.toString()

                    if (firstChildHeaderText != secondChildSection && secondChild.top < headerView.height) {
                        // snap overlay under the next section
                        canvas.translate(0f, (secondChild.top - headerView.height).toFloat())
                    }
                }
                headerView.draw(canvas)
                canvas.restore()
            }
        }
    }

}


private class EntriesAdapter : PagedListAdapter<UiEntry, EntryListItemViewHolder>(EntriesDiffUtil) {

    override fun getItemCount(): Int {
        return super.getItemCount() * 2
    }

    override fun getItem(position: Int): UiEntry? {
        return super.getItem(position / 2)
    }

    override fun getItemViewType(position: Int): Int {
        if (position % 2 == 0) {
            if (position != 0 && getItem(position)?.formattedDate == getItem(position - 2)?.formattedDate) {
                return R.layout.entry_list_divider
            }
            return R.layout.entry_list_header
        }
        if (getItem(position)?.readTime != 0L) {
            return R.layout.entry_list_read_item
        }
        return R.layout.entry_list_unread_item
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryListItemViewHolder = when (viewType) {
        R.layout.entry_list_divider -> DividerViewHolder(LayoutInflater.from(parent.context).inflate(viewType, parent, false))
        R.layout.entry_list_header -> HeaderViewHolder(LayoutInflater.from(parent.context).inflate(viewType, parent, false))
        R.layout.entry_list_read_item -> ReadEntryViewHolder(LayoutInflater.from(parent.context).inflate(viewType, parent, false))
        R.layout.entry_list_unread_item -> UnreadEntryViewHolder(LayoutInflater.from(parent.context).inflate(viewType, parent, false))
        else -> throw IllegalStateException()
    }

    override fun onBindViewHolder(holder: EntryListItemViewHolder, position: Int) {
        val entry = getItem(position)

        if (entry != null) {
            holder.onBind(entry)
        } else {
            holder.itemView.visibility = View.INVISIBLE
        }
    }

}

private sealed class EntryListItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    lateinit var entry: UiEntry
        protected set

    open fun onBind(entry: UiEntry) {
        this.entry = entry
    }

}

private class DividerViewHolder(itemView: View) : EntryListItemViewHolder(itemView)

private abstract class EntryViewHolder(itemView: View) : EntryListItemViewHolder(itemView), View.OnClickListener {

    val favicon: ImageView = itemView.findViewById(R.id.favicon)
    val title: TextView = itemView.findViewById(R.id.title)
    val feedTitle: TextView = itemView.findViewById(R.id.feed_title)
    val time: TextView = itemView.findViewById(R.id.time)

    init {
        itemView.setOnClickListener(this)
    }

    override fun onBind(entry: UiEntry) {
        super.onBind(entry)

        itemView.visibility = View.VISIBLE

        feedTitle.text = entry.feedTitle
        title.text = entry.title
        favicon.setImageResource(R.drawable.favicon_placeholder)
        time.text = entry.formattedTime.toString()
    }

    override fun onClick(view: View?) {
        val entry = entry

        val context = itemView.context
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.link)))

        val applicationContext = context.applicationContext
        doAsync {
            Database.from(applicationContext).entryDao()
                    .setReadTime(entry.id, System.currentTimeMillis())
        }
    }

}

private class ReadEntryViewHolder(itemView: View) : EntryViewHolder(itemView)

private class UnreadEntryViewHolder(itemView: View) : EntryViewHolder(itemView)

private class HeaderViewHolder(itemView: View) : EntryListItemViewHolder(itemView) {

    val header: TextView = itemView.findViewById(R.id.header)

    override fun onBind(entry: UiEntry) {
        super.onBind(entry)

        header.text = entry.formattedDate.toString()
    }

}

private object EntriesDiffUtil : DiffUtil.ItemCallback<UiEntry>() {

    override fun areItemsTheSame(oldItem: UiEntry, newItem: UiEntry): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: UiEntry, newItem: UiEntry): Boolean {
        return oldItem == newItem
    }

}

private class SwipeItemTouchHelper : ItemTouchHelper.Callback() {

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int =
            when (viewHolder) {
                is EntryViewHolder -> makeMovementFlags(0, ItemTouchHelper.START or ItemTouchHelper.END)
                else -> 0
            }


    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        if (viewHolder is EntryViewHolder) {
            val entry = viewHolder.entry
            doAsync {
                Database.from(App.instance).entryDao()
                        .setReadTime(entry.id, if (entry.readTime != 0L) 0 else System.currentTimeMillis())
            }
        }
    }

}
