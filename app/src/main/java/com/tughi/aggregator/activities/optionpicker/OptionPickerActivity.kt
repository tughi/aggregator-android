package com.tughi.aggregator.activities.optionpicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.AppActivity
import com.tughi.aggregator.R

class OptionPickerActivity : AppActivity() {

    companion object {
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_TITLE_RESOURCE = "title-resource"
        private const val EXTRA_OPTIONS = "options"
        private const val EXTRA_SELECTED_OPTION = "selected-option"
    }

    private lateinit var adapter: OptionsAdapter

    private lateinit var saveMenuItem: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(Activity.RESULT_CANCELED)

        val titleResource = intent.getIntExtra(EXTRA_TITLE_RESOURCE, 0)
        if (titleResource != 0) {
            setTitle(titleResource)
        } else {
            val title = intent.getStringExtra(EXTRA_TITLE)
            if (title != null) {
                setTitle(title)
            }
        }

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.action_cancel)
        }

        val options = intent.getOptionArrayExtra(EXTRA_OPTIONS) ?: emptyArray()
        val preselectedOption = intent.getOptionExtra(EXTRA_SELECTED_OPTION)

        adapter = OptionsAdapter(options, preselectedOption, object : OptionsAdapter.Listener {
            override fun onOptionClicked(option: Option?) {
                adapter.selectedOption = option
            }
        })

        setContentView(R.layout.option_picker_activity)

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.option_picker_activity, menu)
        saveMenuItem = menu.findItem(R.id.save)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // ignored
            }
            R.id.save -> {
                setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_SELECTED_OPTION, adapter.selectedOption))
            }
            else -> return super.onOptionsItemSelected(item)
        }

        finish()
        return true
    }

    class OptionViewHolder(itemView: View, listener: OptionsAdapter.Listener) : RecyclerView.ViewHolder(itemView) {
        private var option: Option? = null

        private val titleView: TextView = itemView.findViewById(R.id.title)
        private val descriptionView: TextView? = itemView.findViewById(R.id.description)

        init {
            itemView.setOnClickListener {
                listener.onOptionClicked(option)
            }
        }

        fun onBind(option: Option) {
            this.option = option

            titleView.text = option.name
            if (descriptionView != null && option.description != null) {
                descriptionView.text = HtmlCompat.fromHtml(option.description, HtmlCompat.FROM_HTML_MODE_LEGACY)
            }
        }
    }

    class OptionsAdapter(private val options: Array<Option>, preselectedOption: Option?, val listener: Listener) : RecyclerView.Adapter<OptionViewHolder>() {

        var selectedOption = preselectedOption
            set(value) {
                val oldValue = field
                if (value == oldValue) {
                    return
                }
                field = value
                notifyItemChanged(options.indexOf(oldValue))
                notifyItemChanged(options.indexOf(value))
            }

        override fun getItemCount(): Int = options.size

        override fun getItemViewType(position: Int): Int {
            if (options[position] == selectedOption) {
                return R.layout.option_picker_item_checked
            }
            return R.layout.option_picker_item_unchecked
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = OptionViewHolder(
            LayoutInflater.from(parent.context).inflate(viewType, parent, false),
            listener
        )

        override fun onBindViewHolder(holder: OptionViewHolder, position: Int) = holder.onBind(options[position])

        interface Listener {
            fun onOptionClicked(option: Option?)
        }

    }

    @Suppress("ArrayInDataClass")
    data class PickOptionInput(val options: Array<Option>, val selectedOption: Option?, val title: String? = null, @StringRes val titleResource: Int? = null)

    class PickOption : ActivityResultContract<PickOptionInput, Option?>() {
        override fun createIntent(context: Context, input: PickOptionInput): Intent =
            Intent(context, OptionPickerActivity::class.java)
                .putExtra(EXTRA_OPTIONS, input.options)
                .putExtra(EXTRA_SELECTED_OPTION, input.selectedOption)
                .putExtra(EXTRA_TITLE, input.title)
                .putExtra(EXTRA_TITLE_RESOURCE, input.titleResource)

        override fun parseResult(resultCode: Int, intent: Intent?): Option? {
            if (resultCode == RESULT_OK) {
                return intent!!.getOptionExtra(EXTRA_SELECTED_OPTION)
            }
            return null
        }
    }

}
