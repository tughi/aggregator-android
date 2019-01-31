package com.tughi.aggregator.activities.updatemode

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.R
import com.tughi.aggregator.data.UpdateMode

internal sealed class UpdateModeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    lateinit var updateMode: UpdateMode

    val titleTextView: TextView = itemView.findViewById(R.id.title)

    fun bind(updateMode: UpdateMode) {
        this.updateMode = updateMode

        titleTextView.text = updateMode.toString(titleTextView.context)

        onBind()
    }

    open fun onBind() {}

}

internal class UncheckedUpdateModeViewHolder(itemView: View) : UpdateModeViewHolder(itemView)

internal class AdaptiveUpdateModeViewHolder(itemView: View) : UpdateModeViewHolder(itemView)

internal class DefaultUpdateModeViewHolder(itemView: View) : UpdateModeViewHolder(itemView)

internal class DisabledUpdateModeViewHolder(itemView: View) : UpdateModeViewHolder(itemView)

internal class OnAppLaunchUpdateModeViewHolder(itemView: View) : UpdateModeViewHolder(itemView)

internal class RepeatingUpdateModeViewHolder(itemView: View) : UpdateModeViewHolder(itemView)

