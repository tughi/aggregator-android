package com.tughi.aggregator

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import com.tughi.aggregator.data.Feed
import com.tughi.aggregator.viewmodels.SubscribeSearchViewModel

class SubscribeSearchFragment : Fragment() {

    private lateinit var urlTextInputLayout: TextInputLayout
    private lateinit var urlEditText: EditText
    private lateinit var explanationTextView: TextView
    private lateinit var feedsRecyclerView: RecyclerView

    private val adapter = Adapter()

    private lateinit var viewModel: SubscribeSearchViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.subscribe_search_fragment, container, false)

        urlTextInputLayout = view.findViewById(R.id.url_wrapper)
        urlEditText = urlTextInputLayout.findViewById(R.id.url)
        explanationTextView = view.findViewById(R.id.explanation)
        feedsRecyclerView = view.findViewById(R.id.feeds)

        val activity = activity as SubscribeActivity
        viewModel = ViewModelProviders.of(activity).get(SubscribeSearchViewModel::class.java)
        viewModel.state.observe(this, Observer {
            updateUI(it)
        })

        urlEditText.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val inputMethodManager = textView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(textView.windowToken, 0)
                textView.clearFocus()

                findFeeds()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        feedsRecyclerView.adapter = adapter

        if (savedInstanceState == null) {
            if (activity.intent.action == Intent.ACTION_SEND) {
                if (viewModel.state.value?.url == null) {
                    urlEditText.setText(activity.intent.getStringExtra(Intent.EXTRA_TEXT))
                    findFeeds()
                }
            } else {
                urlEditText.requestFocus()
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()

        activity?.setTitle(R.string.title_find_feeds)
    }

    private fun findFeeds() {
        viewModel.findFeeds(urlEditText.text.toString())
    }

    private fun updateUI(state: SubscribeSearchViewModel.State) {
        urlEditText.isEnabled = !state.loading

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

    private open class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        open fun onBind(item: Any) {}
    }

    private class FeedViewHolder(itemView: View) : ViewHolder(itemView), View.OnClickListener {
        private val titleTextView = itemView.findViewById<TextView>(R.id.title)
        private val urlTextView = itemView.findViewById<TextView>(R.id.url)

        private lateinit var feed: Feed

        init {
            itemView.setOnClickListener(this)
        }

        override fun onBind(item: Any) {
            feed = item as Feed
            titleTextView.text = feed.title
            urlTextView.text = feed.url
        }

        override fun onClick(v: View?) {
            val activity = itemView.context as SubscribeActivity
            val arguments = Bundle().apply {
                putString(SubscribeFeedFragment.ARG_TITLE, feed.title)
                putString(SubscribeFeedFragment.ARG_URL, feed.url)
            }
            activity.supportFragmentManager.beginTransaction()
                    .replace(android.R.id.content, SubscribeFeedFragment().also { it.arguments = arguments })
                    .addToBackStack(null)
                    .commit()
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

    private class LoadingViewHolder(itemView: View) : ViewHolder(itemView)

    private class MessageViewHolder(itemView: View) : ViewHolder(itemView) {
        private val textView = itemView as TextView

        override fun onBind(item: Any) {
            textView.text = item as String
        }
    }

}