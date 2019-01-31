package com.tughi.aggregator.activities.updatemode

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.R
import com.tughi.aggregator.data.AdaptiveUpdateMode
import com.tughi.aggregator.data.DefaultUpdateMode
import com.tughi.aggregator.data.DisabledUpdateMode
import com.tughi.aggregator.data.Every15MinutesUpdateMode
import com.tughi.aggregator.data.Every2HoursUpdateMode
import com.tughi.aggregator.data.Every30MinutesUpdateMode
import com.tughi.aggregator.data.Every3HoursUpdateMode
import com.tughi.aggregator.data.Every45MinutesUpdateMode
import com.tughi.aggregator.data.Every4HoursUpdateMode
import com.tughi.aggregator.data.Every6HoursUpdateMode
import com.tughi.aggregator.data.Every8HoursUpdateMode
import com.tughi.aggregator.data.EveryHourUpdateMode
import com.tughi.aggregator.data.OnAppLaunchUpdateMode
import com.tughi.aggregator.data.UpdateMode

internal class UpdateModeAdapter(private val updateModes: List<UpdateMode>, currentUpdateMode: UpdateMode, private val listener: Listener) : RecyclerView.Adapter<UpdateModeViewHolder>() {

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
            OnAppLaunchUpdateMode -> R.layout.update_mode_item_checked_on_app_launch
            Every15MinutesUpdateMode -> R.layout.update_mode_item_checked_repeating
            Every30MinutesUpdateMode -> R.layout.update_mode_item_checked_repeating
            Every45MinutesUpdateMode -> R.layout.update_mode_item_checked_repeating
            EveryHourUpdateMode -> R.layout.update_mode_item_checked_repeating
            Every2HoursUpdateMode -> R.layout.update_mode_item_checked_repeating
            Every3HoursUpdateMode -> R.layout.update_mode_item_checked_repeating
            Every4HoursUpdateMode -> R.layout.update_mode_item_checked_repeating
            Every6HoursUpdateMode -> R.layout.update_mode_item_checked_repeating
            Every8HoursUpdateMode -> R.layout.update_mode_item_checked_repeating
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
            R.layout.update_mode_item_checked_on_app_launch -> OnAppLaunchUpdateModeViewHolder(itemView)
            R.layout.update_mode_item_checked_repeating -> RepeatingUpdateModeViewHolder(itemView)
            else -> throw IllegalArgumentException("Unsupported view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: UpdateModeViewHolder, position: Int) {
        holder.bind(updateModes[position])
    }

    internal interface Listener {

        fun onUpdateModeClicked(updateMode: UpdateMode)

    }

}
