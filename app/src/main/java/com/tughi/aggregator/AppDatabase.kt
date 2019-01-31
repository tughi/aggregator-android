package com.tughi.aggregator

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tughi.aggregator.activities.reader.ReaderDao
import com.tughi.aggregator.data.CustomTypeConverters
import com.tughi.aggregator.data.Entry
import com.tughi.aggregator.data.EntryDao
import com.tughi.aggregator.data.Feed
import com.tughi.aggregator.data.FeedDao
import com.tughi.aggregator.utilities.DATABASE_NAME
import com.tughi.aggregator.utilities.restoreFeeds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@androidx.room.Database(
        entities = [
            Entry::class,
            Feed::class
        ],
        version = 15
)
@TypeConverters(CustomTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun entryDao(): EntryDao
    abstract fun feedDao(): FeedDao

    abstract fun readerDao(): ReaderDao

    companion object {

        val instance: AppDatabase by lazy { create(App.instance) }

        private fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)

                            GlobalScope.launch(Dispatchers.IO) {
                                restoreFeeds()
                            }
                        }
                    })
                    .build()
        }

    }

}
