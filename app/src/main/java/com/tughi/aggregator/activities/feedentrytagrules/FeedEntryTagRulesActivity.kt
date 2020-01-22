package com.tughi.aggregator.activities.feedentrytagrules

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
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
import com.tughi.aggregator.activities.entrytagrulesettings.EntryTagRuleSettingsActivity
import com.tughi.aggregator.data.EntryTagRules
import com.tughi.aggregator.data.FeedEntryTagRulesQueryCriteria

class FeedEntryTagRulesActivity : AppActivity() {

    companion object {
        private const val EXTRA_FEED_ID = "feed_id"

        fun startForResult(fragment: Fragment, resultCode: Int, feedId: Long) {
            fragment.context?.let { context ->
                fragment.startActivityForResult(
                        Intent(context, FeedEntryTagRulesActivity::class.java)
                                .putExtra(EXTRA_FEED_ID, feedId),
                        resultCode
                )
            }
        }
    }

    private val feedId: Long by lazy { intent.getLongExtra(EXTRA_FEED_ID, 0) }
    private val viewModel: FeedEntryTagRulesViewModel by lazy {
        val viewModelFactory = FeedEntryTagRulesViewModel.Factory(feedId)
        ViewModelProviders.of(this, viewModelFactory).get(FeedEntryTagRulesViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.action_back)
        }

        setContentView(R.layout.feed_entry_tag_rules_activity)

        val recyclerView = findViewById<RecyclerView>(R.id.list)
        val progressBar = findViewById<ProgressBar>(R.id.progress)

        val adapter = EntryTagRulesAdapter(object : EntryTagRulesAdapter.Listener {
            override fun onEntryTagRuleClick(entryTagRule: EntryTagRule) {
                EntryTagRuleSettingsActivity.start(this@FeedEntryTagRulesActivity, entryTagRule.id, feedId)
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)

        if (menu != null) {
            menuInflater.inflate(R.menu.feed_entry_tag_rules_activity, menu)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add -> {
                EntryTagRuleSettingsActivity.start(this, null, feedId)
            }
            android.R.id.home -> {
                finish()
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
        return true
    }

    class FeedEntryTagRulesViewModel(feedId: Long) : ViewModel() {
        val entryTagRules = EntryTagRules.liveQuery(FeedEntryTagRulesQueryCriteria(feedId), EntryTagRule.QueryHelper)

        class Factory(private val feedId: Long) : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(FeedEntryTagRulesViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return FeedEntryTagRulesViewModel(feedId) as T
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
        private val conditionView = itemView.findViewById<TextView>(R.id.condition)

        init {
            itemView.setOnClickListener {
                entryTagRule?.let {
                    listener.onEntryTagRuleClick(it)
                }
            }
        }

        fun onBind(entryTagRule: EntryTagRule) {
            this.entryTagRule = entryTagRule

            tagNameView.text = entryTagRule.tagName
            conditionView.setText(R.string.entry_tag_rules__condition__any)
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
