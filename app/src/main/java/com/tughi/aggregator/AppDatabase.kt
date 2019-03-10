package com.tughi.aggregator

import android.content.Context
import com.tughi.aggregator.activities.reader.ReaderDao

abstract class AppDatabase {

    abstract fun readerDao(): ReaderDao

    companion object {

        val instance: AppDatabase by lazy { create(App.instance) }

        private fun create(context: Context): AppDatabase {
            return object : AppDatabase() {
                override fun readerDao(): ReaderDao {
                    throw UnsupportedOperationException()
                }
            }
        }

    }

}
