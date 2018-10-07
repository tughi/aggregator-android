package com.tughi.aggregator.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tughi.aggregator.utilities.DATABASE_NAME

@Database(
        entities = [
            Entry::class,
            Feed::class
        ],
        version = 3
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun entryDao(): EntryDao
    abstract fun feedDao(): FeedDao

    companion object {

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: create(context).also { instance = it }
            }
        }

        private fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                    .fallbackToDestructiveMigration()
                    .build()
        }

    }

}
