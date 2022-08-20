package com.tughi.aggregator.utilities

import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView
import androidx.collection.LruCache
import com.tughi.aggregator.R
import com.tughi.aggregator.contentScope
import com.tughi.aggregator.data.Feeds
import kotlinx.coroutines.Dispatchers
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
                    val targetReference = WeakReference(target)

                    contentScope.launch {
                        val feed = Feeds.queryOne(Feeds.QueryRowCriteria(feedId), Feed.QueryHelper) ?: return@launch
                        val decodedBitmap = when {
                            feed.faviconContent != null -> BitmapFactory.decodeByteArray(feed.faviconContent, 0, feed.faviconContent.size)
                            else -> null
                        }

                        if (decodedBitmap != null) {
                            launch(Dispatchers.Main) {
                                if (cache.get(faviconUrl) == null) {
                                    cache.put(faviconUrl, decodedBitmap)
                                }

                                targetReference.get()?.run {
                                    setImageBitmap(decodedBitmap)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    class Feed(val faviconContent: ByteArray?) {
        object QueryHelper : Feeds.QueryHelper<Feed>(
            Feeds.FAVICON_CONTENT
        ) {
            override fun createRow(cursor: Cursor) = Feed(
                faviconContent = cursor.getBlob(0)
            )
        }
    }

}
