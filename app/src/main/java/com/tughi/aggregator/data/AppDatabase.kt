package com.tughi.aggregator.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tughi.aggregator.utilities.DATABASE_NAME

@Database(
        entities = [
            Feed::class
        ],
        version = 1
)
abstract class AppDatabase : RoomDatabase() {

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
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(database: SupportSQLiteDatabase) {
                            arrayOf(
                                    ContentValues().apply {
                                        put("url", "url1")
                                        put("title", "Aggregator News")
                                    },
                                    ContentValues().apply {
                                        put("url", "url2")
                                        put("title", "MarsTechnico")
                                    },
                                    ContentValues().apply {
                                        put("url", "url3")
                                        put("title", "Slashdok")
                                    },
                                    ContentValues().apply {
                                        put("url", "url4")
                                        put("title", "The Virge")
                                    }
                            ).forEach { values ->
                                database.insert("feeds", SQLiteDatabase.CONFLICT_REPLACE, values)
                            }
                        }
                    })
                    .build()
        }

    }

}