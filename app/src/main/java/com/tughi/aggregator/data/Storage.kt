package com.tughi.aggregator.data

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.tughi.aggregator.App
import com.tughi.aggregator.utilities.DATABASE_NAME

object Storage {

    private val sqlite: SupportSQLiteOpenHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(App.instance)
                    .name(DATABASE_NAME)
                    .callback(object : SupportSQLiteOpenHelper.Callback(16) {
                        override fun onCreate(db: SupportSQLiteDatabase?) {
                            createDatabase(db ?: throw IllegalStateException("Null database"))
                        }

                        override fun onUpgrade(db: SupportSQLiteDatabase?, oldVersion: Int, newVersion: Int) {
                            upgradeDatabase(db ?: throw IllegalStateException("Null database"))
                        }
                    })
                    .build()
    )

    private fun createDatabase(database: SupportSQLiteDatabase) {
        throw UnsupportedOperationException()
    }

    private fun upgradeDatabase(database: SupportSQLiteDatabase) {
        throw UnsupportedOperationException()
    }

    val readableDatabase: SupportSQLiteDatabase
        get() = sqlite.readableDatabase

    val writableDatabase: SupportSQLiteDatabase
        get() = sqlite.writableDatabase

}
