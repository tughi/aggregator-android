package com.tughi.aggregator.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.lifecycle.LiveData
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.tughi.aggregator.App
import com.tughi.aggregator.utilities.DATABASE_NAME
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

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

    private val tableObservers = mutableSetOf<TableObserver>()

    private fun createDatabase(database: SupportSQLiteDatabase) {
        throw UnsupportedOperationException()
    }

    private fun upgradeDatabase(database: SupportSQLiteDatabase) {
        throw UnsupportedOperationException()
    }

    fun query(sqliteQuery: SupportSQLiteQuery?): Cursor = sqlite.readableDatabase.query(sqliteQuery)

    fun insert(table: String, values: ContentValues): Long {
        val database = sqlite.writableDatabase
        val id = database.insert(table, SQLiteDatabase.CONFLICT_FAIL, values)
        if (id != -1L) {
            if (database.inTransaction()) {
                TODO("delay table invalidation until the transaction was committed")
            } else {
                invalidateTable(table)
            }
        }
        return id
    }

    fun update(table: String, values: ContentValues, selection: String?, selectionArgs: Array<Any>?, recordId: Any? = null): Int {
        val database = sqlite.writableDatabase
        val result = database.update(table, SQLiteDatabase.CONFLICT_FAIL, values, selection, selectionArgs)
        if (result > 0) {
            if (database.inTransaction()) {
                TODO("delay table invalidation until the transaction was committed")
            } else {
                invalidateTable(table, recordId)
            }
        }
        return result
    }

    private fun invalidateTable(table: String, recordId: Any? = null) {
        synchronized(this) {
            if (tableObservers.size > 0) {
                val vanishedObservers = mutableListOf<TableObserver>()

                for (tableObserver in tableObservers) {
                    if (tableObserver.table == table && (tableObserver.recordId == null || tableObserver.recordId == recordId)) {
                        val listener = tableObserver.listener
                        if (listener != null) {
                            listener.onInvalidated()
                        } else {
                            vanishedObservers.add(tableObserver)
                        }
                    }
                }

                for (tableObserver in vanishedObservers) {
                    tableObservers.remove(tableObserver)
                }
            }
        }
    }

    fun <T> createLiveData(table: String, recordId: Any? = null, loadData: () -> T): LiveData<T> {
        val liveData = object : LiveData<T>(), TableObserver.Listener {
            override fun onActive() {
                if (value == null) {
                    update()
                }
            }

            override fun onInvalidated() {
                update()
            }

            private fun update() {
                GlobalScope.launch {
                    val data = loadData()
                    postValue(data)
                }
            }
        }

        synchronized(tableObservers) {
            tableObservers.add(TableObserver(table, recordId, liveData))
        }

        return liveData
    }

    private class TableObserver(val table: String, val recordId: Any?, listener: Listener) {

        private val reference = WeakReference(listener)

        val listener
            get() = reference.get()

        interface Listener {

            fun onInvalidated()

        }

    }

}
