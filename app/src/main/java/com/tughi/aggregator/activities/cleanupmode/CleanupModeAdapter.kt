package com.tughi.aggregator.activities.cleanupmode

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.R
import com.tughi.aggregator.data.Age1MonthCleanupMode
import com.tughi.aggregator.data.Age1WeekCleanupMode
import com.tughi.aggregator.data.Age1YearCleanupMode
import com.tughi.aggregator.data.Age3DaysCleanupMode
import com.tughi.aggregator.data.Age3MonthsCleanupMode
import com.tughi.aggregator.data.Age3YearsCleanupMode
import com.tughi.aggregator.data.Age6MonthsCleanupMode
import com.tughi.aggregator.data.Age6YearsCleanupMode
import com.tughi.aggregator.data.CleanupMode
import com.tughi.aggregator.data.DefaultCleanupMode
import com.tughi.aggregator.data.NeverCleanupMode

internal class CleanupModeAdapter(private val cleanupModes: List<CleanupMode>, currentCleanupMode: CleanupMode, private val listener: Listener) : RecyclerView.Adapter<CleanupModeViewHolder>() {

    var selectedCleanupMode: CleanupMode = currentCleanupMode
        set(value) {
            val oldValue = field
            if (value == oldValue) {
                return
            }
            field = value
            notifyItemChanged(cleanupModes.indexOf(oldValue))
            notifyItemChanged(cleanupModes.indexOf(value))
        }

    override fun getItemCount(): Int = cleanupModes.size

    override fun getItemViewType(position: Int): Int {
        val cleanupMode = cleanupModes[position]
        if (cleanupMode != selectedCleanupMode) {
            return R.layout.cleanup_mode_item_unchecked
        }

        return when (cleanupMode) {
            DefaultCleanupMode -> R.layout.cleanup_mode_item_checked_default
            Age3DaysCleanupMode -> R.layout.cleanup_mode_item_checked_age_3_days
            Age1WeekCleanupMode -> R.layout.cleanup_mode_item_checked_age_1_week
            Age1MonthCleanupMode -> R.layout.cleanup_mode_item_checked_age_1_month
            Age3MonthsCleanupMode -> R.layout.cleanup_mode_item_checked_age_3_months
            Age6MonthsCleanupMode -> R.layout.cleanup_mode_item_checked_age_6_months
            Age1YearCleanupMode -> R.layout.cleanup_mode_item_checked_age_1_year
            Age3YearsCleanupMode -> R.layout.cleanup_mode_item_checked_age_3_years
            Age6YearsCleanupMode -> R.layout.cleanup_mode_item_checked_age_6_years
            NeverCleanupMode -> R.layout.cleanup_mode_item_checked_never
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CleanupModeViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return when (viewType) {
            R.layout.cleanup_mode_item_unchecked -> UncheckedCleanupModeViewHolder(itemView).also { holder ->
                itemView.setOnClickListener {
                    listener.onCleanupModeClicked(holder.cleanupMode)
                }
            }
            R.layout.cleanup_mode_item_checked_default -> DefaultCleanupModeViewHolder(itemView)
            R.layout.cleanup_mode_item_checked_never -> NeverCleanupModeViewHolder(itemView)
            R.layout.cleanup_mode_item_checked_age_3_days -> Age3DaysCleanupModeViewHolder(itemView)
            R.layout.cleanup_mode_item_checked_age_1_week -> Age1WeekCleanupModeViewHolder(itemView)
            R.layout.cleanup_mode_item_checked_age_1_month -> Age1MonthCleanupModeViewHolder(itemView)
            R.layout.cleanup_mode_item_checked_age_3_months -> Age3MonthsCleanupModeViewHolder(itemView)
            R.layout.cleanup_mode_item_checked_age_6_months -> Age6MonthsCleanupModeViewHolder(itemView)
            R.layout.cleanup_mode_item_checked_age_1_year -> Age1YearCleanupModeViewHolder(itemView)
            R.layout.cleanup_mode_item_checked_age_3_years -> Age3YearsCleanupModeViewHolder(itemView)
            R.layout.cleanup_mode_item_checked_age_6_years -> Age6YearsCleanupModeViewHolder(itemView)
            else -> throw IllegalArgumentException("Unsupported view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: CleanupModeViewHolder, position: Int) {
        holder.bind(cleanupModes[position])
    }

    internal interface Listener {

        fun onCleanupModeClicked(cleanupMode: CleanupMode)

    }

}
