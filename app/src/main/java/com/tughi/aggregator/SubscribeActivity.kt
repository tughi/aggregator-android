package com.tughi.aggregator

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import com.tughi.aggregator.data.Feed
import com.tughi.aggregator.viewmodels.SubscribeViewModel

class SubscribeActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_VIA_ACTION = "via_action"
    }

    private val urlTextInputLayout by lazy { findViewById<TextInputLayout>(R.id.url_wrapper) }
    private val urlEditText by lazy { urlTextInputLayout.findViewById<EditText>(R.id.url) }
    private val explanationTextView by lazy { findViewById<TextView>(R.id.explanation) }
    private val feedsRecyclerView by lazy { findViewById<RecyclerView>(R.id.feeds) }

    private val adapter = Adapter()

    private lateinit var viewModel: SubscribeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.subscribe_activity)

        viewModel = ViewModelProviders.of(this).get(SubscribeViewModel::class.java)
        viewModel.state.observe(this, Observer {
            updateUI(it)
        })

        urlEditText.setOnEditorActionListener { view, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                findFeeds()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        feedsRecyclerView.adapter = adapter

        if (savedInstanceState == null && intent.action == Intent.ACTION_SEND) {
            urlEditText.setText(intent.getStringExtra(Intent.EXTRA_TEXT))
            findFeeds()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            if (!intent.getBooleanExtra(EXTRA_VIA_ACTION, false)) {
                setHomeAsUpIndicator(R.drawable.action_cancel)
            }
        }

        if (viewModel.state.value?.loading != true) {
            urlEditText.requestFocus()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home ->
                finish()
            else ->
                return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun findFeeds() {
        viewModel.findFeeds(urlEditText.text.toString())
    }

    private fun updateUI(state: SubscribeViewModel.State) {
        if (state.loading || !state.feeds.isEmpty() || state.message != null) {
            explanationTextView.visibility = View.GONE
            feedsRecyclerView.visibility = View.VISIBLE

            adapter.submitList(mutableListOf<Any>().apply {
                addAll(state.feeds)
                if (state.message != null) {
                    add(state.message)
                }
                if (state.loading) {
                    add(true)
                }
            })
        } else {
            explanationTextView.visibility = View.VISIBLE
            feedsRecyclerView.visibility = View.GONE
        }
    }

    private open class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        open fun onBind(item: Any) {}
    }

    private class FeedViewHolder(itemView: View) : ViewHolder(itemView) {
        private val titleTextView = itemView.findViewById<TextView>(R.id.title)
        private val urlTextView = itemView.findViewById<TextView>(R.id.url)

        override fun onBind(item: Any) {
            val feed = item as Feed

            titleTextView.text = feed.title
            urlTextView.text = feed.url
        }
    }

    private class LoadingViewHolder(itemView: View) : ViewHolder(itemView)

    private class MessageViewHolder(itemView: View) : ViewHolder(itemView) {
        private val textView = itemView as TextView

        override fun onBind(item: Any) {
            textView.text = item as String
        }
    }

    private class DiffUtilCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when (oldItem) {
                is Feed -> newItem is Feed && oldItem.url == newItem.url
                is Boolean -> newItem is Boolean
                is String -> newItem is String
                else -> throw IllegalStateException("Unsupported old item")
            }
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return oldItem == newItem
        }
    }

    private class Adapter : ListAdapter<Any, ViewHolder>(DiffUtilCallback()) {
        override fun getItemViewType(position: Int): Int {
            return when (getItem(position)) {
                is Feed -> R.layout.subscribe_feed_item
                is Boolean -> R.layout.subscribe_loading_item
                is String -> R.layout.subscribe_message_item
                else -> throw IllegalStateException("Unsupported item")
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
            return when (viewType) {
                R.layout.subscribe_feed_item -> FeedViewHolder(itemView)
                R.layout.subscribe_loading_item -> LoadingViewHolder(itemView)
                R.layout.subscribe_message_item -> MessageViewHolder(itemView)
                else -> throw IllegalStateException("Unsupported item view type")
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.onBind(getItem(position))
        }
    }

}
