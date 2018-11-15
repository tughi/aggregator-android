package com.tughi.aggregator

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tughi.aggregator.data.*
import com.tughi.aggregator.utilities.DATABASE_NAME

@androidx.room.Database(
        entities = [
            Entry::class,
            Feed::class
        ],
        version = 12
)
@TypeConverters(CustomTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun entryDao(): EntryDao
    abstract fun feedDao(): FeedDao

    companion object {

        val instance: AppDatabase by lazy { create(App.instance) }

        private fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                    .fallbackToDestructiveMigration()
                    .build()
        }

    }

}
