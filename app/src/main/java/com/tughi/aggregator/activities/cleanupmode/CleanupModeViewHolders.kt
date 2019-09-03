package com.tughi.aggregator.activities.cleanupmode

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.R
import com.tughi.aggregator.data.CleanupMode

internal sealed class CleanupModeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    lateinit var cleanupMode: CleanupMode

    val titleTextView: TextView = itemView.findViewById(R.id.title)

    fun bind(cleanupMode: CleanupMode) {
        this.cleanupMode = cleanupMode

        titleTextView.text = cleanupMode.toString(titleTextView.context)

        onBind()
    }

    open fun onBind() {}

}

internal class UncheckedCleanupModeViewHolder(itemView: View) : CleanupModeViewHolder(itemView)

internal class DefaultCleanupModeViewHolder(itemView: View) : CleanupModeViewHolder(itemView)

internal class NeverCleanupModeViewHolder(itemView: View) : CleanupModeViewHolder(itemView)

internal class Age3DaysCleanupModeViewHolder(itemView: View) : CleanupModeViewHolder(itemView)

internal class Age1WeekCleanupModeViewHolder(itemView: View) : CleanupModeViewHolder(itemView)

internal class Age1MonthCleanupModeViewHolder(itemView: View) : CleanupModeViewHolder(itemView)

internal class Age3MonthsCleanupModeViewHolder(itemView: View) : CleanupModeViewHolder(itemView)

internal class Age6MonthsCleanupModeViewHolder(itemView: View) : CleanupModeViewHolder(itemView)

internal class Age1YearCleanupModeViewHolder(itemView: View) : CleanupModeViewHolder(itemView)

internal class Age3YearsCleanupModeViewHolder(itemView: View) : CleanupModeViewHolder(itemView)

internal class Age6YearsCleanupModeViewHolder(itemView: View) : CleanupModeViewHolder(itemView)
