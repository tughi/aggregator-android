package com.tughi.aggregator.activities.entrytagrules

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.AppActivity
import com.tughi.aggregator.R
import com.tughi.aggregator.data.EntryTagRules
import com.tughi.aggregator.data.EntryTagRulesQueryCriteria

class EntryTagRulesActivity : AppActivity() {

    companion object {
        private const val EXTRA_FEED_ID = "feed_id"

        fun startForResult(fragment: Fragment, resultCode: Int, feedId: Long?) {
            fragment.context?.let { context ->
                fragment.startActivityForResult(
                        Intent(context, EntryTagRulesActivity::class.java)
                                .putExtra(EXTRA_FEED_ID, feedId),
                        resultCode
                )
            }
        }
    }

    private val feedId: Long? by lazy { intent.getLongExtra(EXTRA_FEED_ID, 0).let { if (it > 0) it else null } }
    private val viewModel: EntryTagRulesViewModel by lazy {
        val viewModelFactory = EntryTagRulesViewModel.Factory(feedId)
        ViewModelProviders.of(this, viewModelFactory).get(EntryTagRulesViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.entry_tag_rules_activity)

        val recyclerView = findViewById<RecyclerView>(R.id.list)
        val progressBar = findViewById<ProgressBar>(R.id.progress)

        val adapter = EntryTagRulesAdapter(object : EntryTagRulesAdapter.Listener {
            override fun onEntryTagRuleClick(entryTagRule: EntryTagRule) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        })
        recyclerView.adapter = adapter

        viewModel.entryTagRules.observe(this, Observer { list ->
            if (list == null) {
                adapter.list = emptyList()
                progressBar.visibility = View.VISIBLE
            } else {
                adapter.list = list
                progressBar.visibility = View.GONE
            }
        })
    }

    class EntryTagRulesViewModel(feedId: Long?) : ViewModel() {
        val entryTagRules = EntryTagRules.liveQuery(EntryTagRulesQueryCriteria(feedId), EntryTagRule.QueryHelper)

        class Factory(private val feedId: Long?) : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(EntryTagRulesViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return EntryTagRulesViewModel(feedId) as T
                }
                throw UnsupportedOperationException()
            }
        }
    }

    class EntryTagRule(
            val id: Long,
            val tagName: String
    ) {
        object QueryHelper : EntryTagRules.QueryHelper<EntryTagRule>(
                EntryTagRules.ID,
                EntryTagRules.TAG_NAME
        ) {
            override fun createRow(cursor: Cursor) = EntryTagRule(
                    cursor.getLong(0),
                    cursor.getString(1)
            )
        }
    }

    class EntryTagRuleViewHolder(itemView: View, listener: EntryTagRulesAdapter.Listener) : RecyclerView.ViewHolder(itemView) {
        private var entryTagRule: EntryTagRule? = null

        private val tagNameView = itemView.findViewById<TextView>(R.id.tag_name)

        fun onBind(entryTagRule: EntryTagRule) {
            this.entryTagRule = entryTagRule

            tagNameView.text = entryTagRule.tagName
        }
    }

    class EntryTagRulesAdapter(private val listener: Listener) : RecyclerView.Adapter<EntryTagRuleViewHolder>() {
        var list: List<EntryTagRule> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun getItemCount() = list.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = EntryTagRuleViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.entry_tag_rules_item, parent, false),
                listener
        )

        override fun onBindViewHolder(holder: EntryTagRuleViewHolder, position: Int) = holder.onBind(list[position])

        interface Listener {
            fun onEntryTagRuleClick(entryTagRule: EntryTagRule)
        }
    }

}
