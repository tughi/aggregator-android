package com.tughi.aggregator.activities.subscribe

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import com.tughi.aggregator.R
import com.tughi.aggregator.activities.opml.OpmlImportActivity

class SubscribeSearchFragment : Fragment(), SubscribeSearchFragmentAdapterListener {

    private lateinit var urlTextInputLayout: TextInputLayout
    private lateinit var urlEditText: EditText
    private lateinit var introView: View
    private lateinit var feedsRecyclerView: RecyclerView

    private val adapter = SubscribeSearchFragmentAdapter(this)

    private lateinit var viewModel: SubscribeSearchFragmentViewModel

    private val requestDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            requestOpmlImport.launch(
                Intent(context, OpmlImportActivity::class.java).apply {
                    this.data = uri
                }
            )
        }
    }

    private val requestOpmlImport = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            requireActivity().finish()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.subscribe_search_fragment, container, false)

        urlTextInputLayout = view.findViewById(R.id.url_wrapper)
        urlEditText = urlTextInputLayout.findViewById(R.id.url)
        introView = view.findViewById(R.id.intro)
        feedsRecyclerView = view.findViewById(R.id.feeds)

        val activity = activity as SubscribeActivity
        viewModel = ViewModelProvider(activity)[SubscribeSearchFragmentViewModel::class.java]
        viewModel.state.observe(viewLifecycleOwner) {
            updateUI(it)
        }

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

        view.findViewById<Button>(R.id.import_opml).setOnClickListener {
            requestDocument.launch(arrayOf("*/*"))
        }

        if (savedInstanceState == null) {
            if (activity.intent.action == Intent.ACTION_SEND) {
                if (viewModel.state.value?.url == null) {
                    urlEditText.setText(activity.intent.getStringExtra(Intent.EXTRA_TEXT))
                    findFeeds()
                }
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()

        activity?.setTitle(R.string.subscribe__find_feeds)
    }

    private fun findFeeds() {
        viewModel.findFeeds(urlEditText.text.toString())
    }

    private fun updateUI(state: SubscribeSearchFragmentViewModel.State) {
        urlEditText.isEnabled = !state.loading

        if (state.loading || state.feeds.isNotEmpty() || state.message != null) {
            introView.visibility = View.GONE
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
            introView.visibility = View.VISIBLE
            feedsRecyclerView.visibility = View.GONE
        }
    }

    override fun onFeedClicked(feed: SubscribeSearchFragmentViewModel.Feed) {
        val activity = activity as SubscribeActivity
        val arguments = Bundle().apply {
            putString(SubscribeFeedFragment.ARG_URL, feed.url)
            putString(SubscribeFeedFragment.ARG_TITLE, feed.title)
            putString(SubscribeFeedFragment.ARG_LINK, feed.link)
        }
        activity.supportFragmentManager.beginTransaction()
            .replace(id, SubscribeFeedFragment().also { it.arguments = arguments })
            .addToBackStack(null)
            .commit()
    }

}

