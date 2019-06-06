package com.tughi.aggregator.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.tughi.aggregator.App
import com.tughi.aggregator.data.migrations.F000T020
import com.tughi.aggregator.data.migrations.F017T020
import com.tughi.aggregator.data.migrations.F019T020
import com.tughi.aggregator.utilities.DATABASE_NAME
import com.tughi.aggregator.utilities.restoreFeeds
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

object Database {

    private val sqlite: SupportSQLiteOpenHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(App.instance)
                    .name(DATABASE_NAME)
                    .callback(object : SupportSQLiteOpenHelper.Callback(20) {
                        override fun onConfigure(db: SupportSQLiteDatabase?) {
                            db?.apply {
                                setForeignKeyConstraintsEnabled(true)
                                enableWriteAheadLogging()
                            }
                        }

                        override fun onCreate(database: SupportSQLiteDatabase?) {
                            onUpgrade(database, 0, version)
                        }

                        override fun onUpgrade(database: SupportSQLiteDatabase?, oldVersion: Int, newVersion: Int) {
                            if (database == null) {
                                throw IllegalStateException()
                            }

                            when (oldVersion) {
                                0 -> F000T020.migrate(database)
                                17 -> F017T020.migrate(database)
                                19 -> F019T020.migrate(database)
                                else -> dropDatabase(database, "Cannot migrate from $oldVersion to $newVersion")
                            }
                        }

                        override fun onDowngrade(database: SupportSQLiteDatabase?, oldVersion: Int, newVersion: Int) {
                            dropDatabase(database, "Cannot migrate from $oldVersion to $newVersion")
                        }

                        private fun dropDatabase(database: SupportSQLiteDatabase?, message: String) {
                            Log.e(javaClass.name, message)
                            onCorruption(database)
                            System.exit(1)
                        }

                        override fun onOpen(db: SupportSQLiteDatabase?) {
                            GlobalScope.launch {
                                restoreFeeds()
                            }
                        }
                    })
                    .build()
    )

    private fun insert(table: String, values: ContentValues, conflictAlgorithm: Int): Long {
        val id = sqlite.writableDatabase.insert(table, conflictAlgorithm, values)
        if (id != -1L) {
            invalidateTable(table)
        }
        return id
    }

    fun insert(table: String, values: ContentValues): Long = insert(table, values, SQLiteDatabase.CONFLICT_FAIL)

    fun replace(table: String, values: ContentValues): Int {
        return if (insert(table, values, SQLiteDatabase.CONFLICT_REPLACE) != -1L) 1 else 0
    }

    fun update(table: String, values: ContentValues, selection: String?, selectionArgs: Array<Any>?): Int {
        val result = sqlite.writableDatabase.update(table, SQLiteDatabase.CONFLICT_FAIL, values, selection, selectionArgs)
        if (result > 0) {
            invalidateTable(table)
        }
        return result
    }

    fun delete(table: String, selection: String?, selectionArgs: Array<Any>?): Int {
        val result = sqlite.writableDatabase.delete(table, selection, selectionArgs)
        if (result > 0) {
            invalidateTable(table)
        }
        return result
    }

    private val tableObservers = mutableSetOf<TableObserver>()

    private val invalidatedTables = mutableSetOf<String>()

    private fun invalidateTable(table: String) {
        if (sqlite.writableDatabase.inTransaction()) {
            synchronized(invalidatedTables) {
                invalidatedTables.add(table)
            }

            return
        }

        synchronized(this) {
            if (tableObservers.size > 0) {
                val vanishedObservers = mutableListOf<TableObserver>()

                for (tableObserver in tableObservers) {
                    if (tableObserver.table == table) {
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

    fun transaction(body: () -> Unit) {
        beginTransaction()
        try {
            body()
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }

    private fun beginTransaction() = sqlite.writableDatabase.run {
        if (isWriteAheadLoggingEnabled) {
            beginTransactionNonExclusive()
        } else {
            beginTransaction()
        }
    }

    private fun setTransactionSuccessful() = sqlite.writableDatabase.setTransactionSuccessful()

    private fun endTransaction() {
        val database = sqlite.writableDatabase
        database.endTransaction()
        if (!database.inTransaction()) {
            synchronized(invalidatedTables) {
                for (table in invalidatedTables) {
                    invalidateTable(table)
                }
                invalidatedTables.clear()
            }
        }
    }

    fun <T> query(sqliteQuery: SupportSQLiteQuery, transform: (Cursor) -> T): T {
        sqlite.readableDatabase.query(sqliteQuery).use { cursor ->
            return transform(cursor)
        }
    }

    fun <T> liveQuery(query: Query, transform: (Cursor) -> T): LiveData<T> {
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
                    sqlite.readableDatabase.query(query).use { cursor ->
                        val data = transform(cursor)
                        postValue(data)
                    }
                }
            }
        }

        synchronized(tableObservers) {
            for (observedTable in query.observedTables) {
                tableObservers.add(TableObserver(observedTable, liveData))
            }
        }

        return liveData
    }

    private class TableObserver(val table: String, listener: Listener) {

        private val reference = WeakReference(listener)

        val listener
            get() = reference.get()

        interface Listener {

            fun onInvalidated()

        }

    }

    interface Migration {
        fun migrate(database: SupportSQLiteDatabase)
    }

}
