package com.tughi.aggregator.activities.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tughi.aggregator.R

class TagsFragment : Fragment() {

    companion object {
        fun newInstance(): TagsFragment {
            return TagsFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.tags_fragment, container, false)
    }

}
