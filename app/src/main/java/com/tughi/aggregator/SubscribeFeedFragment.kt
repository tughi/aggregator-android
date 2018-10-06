package com.tughi.aggregator

import androidx.fragment.app.Fragment

class SubscribeFeedFragment : Fragment() {

    companion object {
        const val ARG_TITLE = "title"
        const val ARG_URL = "url"
    }

    override fun onResume() {
        super.onResume()

        activity?.setTitle(R.string.title_add_feed)
    }

}