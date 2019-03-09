package com.tughi.aggregator

import android.content.Context
import com.tughi.aggregator.activities.reader.ReaderDao
import com.tughi.aggregator.data.FeedDao

abstract class AppDatabase {

    abstract fun feedDao(): FeedDao

    abstract fun readerDao(): ReaderDao

    fun beginTransaction() {
        throw UnsupportedOperationException()
    }

    fun setTransactionSuccessful() {
        throw UnsupportedOperationException()
    }

    fun endTransaction() {
        throw UnsupportedOperationException()
    }

    companion object {

        val instance: AppDatabase by lazy { create(App.instance) }

        private fun create(context: Context): AppDatabase {
            return object : AppDatabase() {
                override fun feedDao(): FeedDao {
                    throw UnsupportedOperationException()
                }

                override fun readerDao(): ReaderDao {
                    throw UnsupportedOperationException()
                }
            }
        }

    }

}
