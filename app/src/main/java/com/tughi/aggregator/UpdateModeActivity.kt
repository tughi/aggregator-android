package com.tughi.aggregator

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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.data.AdaptiveUpdateMode
import com.tughi.aggregator.data.DefaultUpdateMode
import com.tughi.aggregator.data.DisabledUpdateMode
import com.tughi.aggregator.data.RepeatingUpdateMode
import com.tughi.aggregator.data.UpdateMode

class UpdateModeActivity : AppActivity() {

    companion object {
        const val EXTRA_UPDATE_MODE = "update-mode"
        const val EXTRA_SHOW_DEFAULT = "show-default"
    }

    private lateinit var adapter: UpdateModeAdapter

    private lateinit var saveMenuItem: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(Activity.RESULT_CANCELED)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.action_cancel)
        }

        setContentView(R.layout.update_mode_activity)

        val currentUpdateMode = UpdateMode.deserialize(intent.getStringExtra(EXTRA_UPDATE_MODE))

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)

        val updateModes = mutableListOf(
                AdaptiveUpdateMode,
                DisabledUpdateMode
                // TODO: if (currentUpdateMode is RepeatingUpdateMode) currentUpdateMode else RepeatingUpdateMode(0)
        )
        if (intent.getBooleanExtra(EXTRA_SHOW_DEFAULT, false)) {
            updateModes.add(0, DefaultUpdateMode)
        }

        adapter = UpdateModeAdapter(updateModes, currentUpdateMode, object : OnUpdateModeClickListener {
            override fun onUpdateModeClicked(updateMode: UpdateMode) {
                adapter.selectedUpdateMode = updateMode
            }
        })
        recyclerView.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val result = super.onCreateOptionsMenu(menu)

        menu?.let {
            menuInflater.inflate(R.menu.update_mode_activity, it)
            saveMenuItem = it.findItem(R.id.save)
            return true
        }

        return result
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                // ignored
            }
            R.id.save -> {
                setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_UPDATE_MODE, adapter.selectedUpdateMode.serialize()))
            }
            else -> return super.onOptionsItemSelected(item)
        }

        finish()
        return true
    }

}

fun Fragment.startUpdateModeActivity(requestCode: Int, currentUpdateMode: UpdateMode, showDefault: Boolean = true) {
    val context = context ?: return
    val intent = Intent(context, UpdateModeActivity::class.java)
            .putExtra(UpdateModeActivity.EXTRA_UPDATE_MODE, currentUpdateMode.serialize())
            .putExtra(UpdateModeActivity.EXTRA_SHOW_DEFAULT, showDefault)
    startActivityForResult(intent, requestCode)
}

private class UpdateModeAdapter(private val updateModes: List<UpdateMode>, currentUpdateMode: UpdateMode, private val listener: OnUpdateModeClickListener) : RecyclerView.Adapter<UpdateModeViewHolder>() {

    var selectedUpdateMode: UpdateMode = currentUpdateMode
        set(value) {
            val oldValue = field
            if (value == oldValue) {
                return
            }
            field = value
            notifyItemChanged(updateModes.indexOf(oldValue))
            notifyItemChanged(updateModes.indexOf(value))
        }

    override fun getItemCount(): Int = updateModes.size

    override fun getItemViewType(position: Int): Int {
        val updateMode = updateModes[position]
        if (updateMode != selectedUpdateMode) {
            return R.layout.update_mode_item_unchecked
        }

        return when (updateMode) {
            AdaptiveUpdateMode -> R.layout.update_mode_item_checked_adaptive
            DefaultUpdateMode -> R.layout.update_mode_item_checked_default
            DisabledUpdateMode -> R.layout.update_mode_item_checked_disabled
            is RepeatingUpdateMode -> R.layout.update_mode_item_checked_repeating
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpdateModeViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return when (viewType) {
            R.layout.update_mode_item_unchecked -> UncheckedUpdateModeViewHolder(itemView).also { holder ->
                itemView.setOnClickListener {
                    listener.onUpdateModeClicked(holder.updateMode)
                }
            }
            R.layout.update_mode_item_checked_adaptive -> AdaptiveUpdateModeViewHolder(itemView)
            R.layout.update_mode_item_checked_default -> DefaultUpdateModeViewHolder(itemView)
            R.layout.update_mode_item_checked_disabled -> DisabledUpdateModeViewHolder(itemView)
            R.layout.update_mode_item_checked_repeating -> RepeatingUpdateModeViewHolder(itemView)
            else -> throw IllegalArgumentException("Unsupported view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: UpdateModeViewHolder, position: Int) {
        holder.bind(updateModes[position])
    }

}

private sealed class UpdateModeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    lateinit var updateMode: UpdateMode

    val titleTextView: TextView = itemView.findViewById(R.id.title)

    fun bind(updateMode: UpdateMode) {
        this.updateMode = updateMode

        titleTextView.text = updateMode.toString(titleTextView.context)

        onBind()
    }

    open fun onBind() {}

}

fun UpdateMode.toString(context: Context): String = when (this) {
    AdaptiveUpdateMode -> context.getString(R.string.update_mode__adaptive)
    DefaultUpdateMode -> context.getString(R.string.update_mode__default, UpdateSettings.defaultUpdateMode.toString(context))
    DisabledUpdateMode -> context.getString(R.string.update_mode__disabled)
    is RepeatingUpdateMode -> TODO()
}

private class UncheckedUpdateModeViewHolder(itemView: View) : UpdateModeViewHolder(itemView)

private class AdaptiveUpdateModeViewHolder(itemView: View) : UpdateModeViewHolder(itemView)

private class DefaultUpdateModeViewHolder(itemView: View) : UpdateModeViewHolder(itemView)

private class DisabledUpdateModeViewHolder(itemView: View) : UpdateModeViewHolder(itemView)

private class RepeatingUpdateModeViewHolder(itemView: View) : UpdateModeViewHolder(itemView)

private interface OnUpdateModeClickListener {

    fun onUpdateModeClicked(updateMode: UpdateMode)

}
