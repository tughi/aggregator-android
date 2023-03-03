package com.tughi.aggregator.activities.opml

import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.tughi.aggregator.AppActivity
import com.tughi.aggregator.R
import com.tughi.aggregator.contentScope
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.feeds.OpmlFeed
import com.tughi.aggregator.feeds.OpmlGenerator
import com.tughi.aggregator.utilities.has
import kotlinx.coroutines.launch

class OpmlExportActivity : AppActivity() {
    private lateinit var viewModel: OpmlFeedsViewModel

    private val requestDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("text/xml")) { uri ->
        if (uri != null) {
            viewModel.feeds.value?.let { feeds ->
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    contentScope.launch {
                        OpmlGenerator.generate(feeds, outputStream)
                    }

                    finish()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.opml_export_activity)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.action_back)
        }

        viewModel = ViewModelProvider(this).get()

        val exportButton = findViewById<Button>(R.id.export)
        exportButton.setOnClickListener {
            requestDocument.launch("aggregator-feeds")
        }

        viewModel.feeds.observe(this) { feeds ->
            exportButton.isEnabled = feeds.has { feed -> feed.selected }
        }

        if (savedInstanceState == null) {
            loadFeeds()
        }
    }

    private fun loadFeeds() {
        contentScope.launch {
            val opmlFeeds = Feeds.query(Feeds.AllCriteria, OpmlFeed.QueryHelper)
            viewModel.feeds.postValue(opmlFeeds)
        }
    }
}
