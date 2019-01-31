package com.tughi.aggregator.activities.main

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tughi.aggregator.R

class EntriesRecyclerView(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs) {

    private val headerView = LayoutInflater.from(context).inflate(R.layout.entry_list_header, FrameLayout(context), false)
    private val headerTextView: TextView = headerView.findViewById(R.id.header)

    private var headerText: String? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (w > 0) {
            // update the overlay layout
            val widthMeasureSpec = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY)
            val heightMeasureSpec = MeasureSpec.makeMeasureSpec(h, MeasureSpec.UNSPECIFIED)
            headerView.measure(widthMeasureSpec, heightMeasureSpec)
            headerView.layout(0, 0, headerView.measuredWidth, headerView.measuredHeight)
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        val childCount = childCount
        if (childCount > 0) {
            val firstChild = getChildAt(0)
            val firstChildViewHolder = getChildViewHolder(firstChild)
            if (firstChildViewHolder is EntriesFragmentViewHolder) {
                // update overlay text
                val firstChildHeaderText = firstChildViewHolder.entry.formattedDate.toString()
                if (headerText != firstChildHeaderText) {
                    headerText = firstChildHeaderText
                    headerTextView.text = headerText

                    headerView.measure(MeasureSpec.makeMeasureSpec(headerView.width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(headerView.height, MeasureSpec.EXACTLY))
                    headerView.layout(0, 0, headerView.width, headerView.height)
                }

                // draw overlay
                canvas.save()
                if (childCount > 1) {
                    val secondChild = getChildAt(1)
                    val secondChildViewHolder = getChildViewHolder(secondChild) as EntriesFragmentViewHolder
                    val secondChildHeaderText = secondChildViewHolder.entry.formattedDate.toString()

                    if (firstChildHeaderText != secondChildHeaderText && secondChild.top < headerView.height) {
                        // snap overlay under the next section
                        canvas.translate(0f, (secondChild.top - headerView.height).toFloat())
                    }
                }
                headerView.draw(canvas)
                canvas.restore()
            }
        }
    }

}
