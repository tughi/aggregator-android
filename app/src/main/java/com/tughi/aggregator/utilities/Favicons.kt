package com.tughi.aggregator.utilities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView
import androidx.collection.LruCache
import com.tughi.aggregator.AppDatabase
import com.tughi.aggregator.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

object Favicons {

    private val cache: LruCache<String, Bitmap>

    init {
        val maxMemory = Math.min(Runtime.getRuntime().maxMemory(), Integer.MAX_VALUE.toLong()).toInt()
        val cacheSize = maxMemory / 8

        cache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.allocationByteCount
            }
        }
    }

    fun load(feedId: Long, faviconUrl: String?, target: ImageView) {
        if (faviconUrl == null) {
            target.setImageResource(R.drawable.favicon_placeholder)
        } else {
            val bitmap = cache.get(faviconUrl)

            if (bitmap != null) {
                target.setImageBitmap(bitmap)
            } else {
                target.setImageResource(R.drawable.favicon_placeholder)

                if (!faviconUrl.isEmpty()) {
                    val placeholder = target.drawable
                    val targetReference = WeakReference(target)

                    GlobalScope.launch {
                        val feedDao = AppDatabase.instance.feedDao()
                        val feed = feedDao.queryFeed(feedId)
                        val bitmap = if (feed.faviconContent != null) BitmapFactory.decodeByteArray(feed.faviconContent, 0, feed.faviconContent.size) else null

                        launch(Dispatchers.Main) {
                            if (bitmap != null) {
                                if (cache.get(faviconUrl) == null) {
                                    cache.put(faviconUrl, bitmap)
                                }
                                val target = targetReference.get()
                                if (target != null && target.drawable == placeholder) {
                                    target.setImageBitmap(bitmap)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
