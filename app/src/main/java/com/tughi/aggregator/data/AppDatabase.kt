package com.tughi.aggregator.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tughi.aggregator.utilities.DATABASE_NAME

@androidx.room.Database(
        entities = [
            Entry::class,
            Feed::class
        ],
        version = 10
)
@TypeConverters(CustomTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun entryDao(): EntryDao
    abstract fun feedDao(): FeedDao

    companion object {

        @Volatile
        private var instance: AppDatabase? = null

        fun from(context: Context): AppDatabase {
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
