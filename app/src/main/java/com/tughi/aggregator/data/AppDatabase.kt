package com.tughi.aggregator.data

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import com.tughi.aggregator.utilities.DATABASE_NAME

@Database(
        entities = [
            Feed::class
        ],
        version = 1,
        exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun feedDao(): FeedDao

    companion object {

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            val request = OneTimeWorkRequestBuilder<Seeder>().build()
                            WorkManager.getInstance().enqueue(request)
                        }
                    })
                    .build()
        }

    }

    class Seeder : Worker() {
        override fun doWork(): Result {
            val database = AppDatabase.getInstance(applicationContext)
            val feeDao = database.feedDao()

            try {
                database.beginTransaction()

                feeDao.addFeed(Feed(
                        url = "url1",
                        title = "Aggregator News"
                ))
                feeDao.addFeed(Feed(
                        url = "url2",
                        title = "MarsTechnico"
                ))
                feeDao.addFeed(Feed(
                        url = "url3",
                        title = "Slashdok"
                ))
                feeDao.addFeed(Feed(
                        url = "url4",
                        title = "The Virge"
                ))

                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }

            return Result.SUCCESS
        }
    }

}