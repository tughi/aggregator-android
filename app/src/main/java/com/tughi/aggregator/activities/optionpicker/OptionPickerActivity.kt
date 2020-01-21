package com.tughi.aggregator.activities.optionpicker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.AppActivity
import com.tughi.aggregator.R

class OptionPickerActivity : AppActivity() {

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_TITLE_RESOURCE = "title-resource"
        const val EXTRA_OPTIONS = "options"
        const val EXTRA_SELECTED_OPTION = "selected-option"

        fun startForResult(activity: Activity, requestCode: Int, options: Array<Option>, selectedOption: Option?, title: String? = null, @StringRes titleResource: Int? = null) {
            activity.startActivityForResult(
                    Intent(activity, OptionPickerActivity::class.java)
                            .putExtra(EXTRA_OPTIONS, options)
                            .putExtra(EXTRA_SELECTED_OPTION, selectedOption)
                            .putExtra(EXTRA_TITLE, title)
                            .putExtra(EXTRA_TITLE_RESOURCE, titleResource),
                    requestCode
            )
        }
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

        val options = intent.getParcelableArrayExtra(EXTRA_OPTIONS)?.map { it as Option }?.toTypedArray() ?: emptyArray()
        val preselectedOption = intent.getParcelableExtra<Option>(EXTRA_SELECTED_OPTION)

        adapter = OptionsAdapter(options, preselectedOption, object : OptionsAdapter.Listener {
            override fun onOptionClicked(option: Option?) {
                adapter.selectedOption = option
            }
        })

        setContentView(R.layout.option_picker_activity)

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val result = super.onCreateOptionsMenu(menu)

        menu?.let {
            menuInflater.inflate(R.menu.option_picker_activity, it)
            saveMenuItem = it.findItem(R.id.save)
            return true
        }

        return result
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            when (item.itemId) {
                android.R.id.home -> {
                    // ignored
                }
                R.id.save -> {
                    setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_SELECTED_OPTION, adapter.selectedOption))
                }
                else -> return super.onOptionsItemSelected(item)
            }
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

            titleView.setText(option.name)
            if (descriptionView != null && option.description != null) {
                descriptionView.setText(option.description)
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

}
