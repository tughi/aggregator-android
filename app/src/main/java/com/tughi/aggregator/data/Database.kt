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
import androidx.sqlite.db.transaction
import com.tughi.aggregator.App
import com.tughi.aggregator.utilities.DATABASE_NAME
import com.tughi.aggregator.utilities.restoreFeeds
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.Serializable
import java.lang.ref.WeakReference

object Database {

    private val sqlite: SupportSQLiteOpenHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(App.instance)
                    .name(DATABASE_NAME)
                    .callback(object : SupportSQLiteOpenHelper.Callback(17) {
                        override fun onConfigure(db: SupportSQLiteDatabase?) {
                            db?.apply {
                                setForeignKeyConstraintsEnabled(true)
                                enableWriteAheadLogging()
                            }
                        }

                        override fun onCreate(database: SupportSQLiteDatabase?) {
                            database?.transaction {
                                database.execSQL("""
                                    CREATE TABLE feeds (
                                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                                        url TEXT NOT NULL,
                                        title TEXT NOT NULL,
                                        link TEXT,
                                        language TEXT,
                                        custom_title TEXT,
                                        favicon_url TEXT,
                                        favicon_content BLOB,
                                        update_mode TEXT NOT NULL,
                                        last_update_time INTEGER NOT NULL DEFAULT 0,
                                        last_update_error TEXT,
                                        next_update_retry INTEGER NOT NULL DEFAULT 0,
                                        next_update_time INTEGER NOT NULL DEFAULT 0,
                                        http_etag TEXT,
                                        http_last_modified TEXT
                                    )
                                """)

                                database.execSQL("""
                                    CREATE TABLE entries (
                                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                                        feed_id INTEGER NOT NULL,
                                        uid TEXT NOT NULL,
                                        title TEXT,
                                        link TEXT,
                                        content TEXT,
                                        author TEXT,
                                        publish_time INTEGER,
                                        insert_time INTEGER NOT NULL,
                                        update_time INTEGER NOT NULL,
                                        read_time INTEGER NOT NULL DEFAULT 0,
                                        pinned_time INTEGER NOT NULL DEFAULT 0,
                                        FOREIGN KEY (feed_id) REFERENCES feeds (id) ON DELETE CASCADE
                                    )
                                """)

                                database.execSQL("CREATE UNIQUE INDEX entries_index__feed_id__uid ON entries (feed_id, uid)")
                            }
                        }

                        override fun onUpgrade(database: SupportSQLiteDatabase?, oldVersion: Int, newVersion: Int) {
                            database?.transaction {
                                val tables = mutableListOf<String>()
                                database.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'").use { cursor ->
                                    if (cursor.moveToFirst()) {
                                        do {
                                            tables.add(cursor.getString(0))
                                        } while (cursor.moveToNext())
                                    }
                                }

                                if (!tables.isEmpty()) {
                                    database.execSQL("PRAGMA foreign_keys = OFF")

                                    tables.forEach {
                                        database.execSQL("DROP TABLE $it")
                                    }

                                    database.execSQL("PRAGMA foreign_keys = OFF")
                                }

                                onCreate(database)
                            }

                        }

                        override fun onOpen(db: SupportSQLiteDatabase?) {
                            GlobalScope.launch {
                                restoreFeeds()
                            }
                        }
                    })
                    .build()
    )

    private val tableObservers = mutableSetOf<TableObserver>()

    fun query(sqliteQuery: SupportSQLiteQuery?): Cursor = sqlite.readableDatabase.query(sqliteQuery)

    fun insert(table: String, values: ContentValues): Long {
        val id = sqlite.writableDatabase.insert(table, SQLiteDatabase.CONFLICT_FAIL, values)
        if (id != -1L) {
            invalidateTable(table)
        }
        return id
    }

    fun update(table: String, values: ContentValues, selection: String?, selectionArgs: Array<Any>?, rowId: Any? = null): Int {
        val result = sqlite.writableDatabase.update(table, SQLiteDatabase.CONFLICT_FAIL, values, selection, selectionArgs)
        if (result > 0) {
            invalidateTable(table, rowId)
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

    private val invalidatedTables = mutableMapOf<String, MutableList<Any>>()

    private fun invalidateTable(table: String, rowId: Any? = null) {
        if (sqlite.writableDatabase.inTransaction()) {
            synchronized(invalidatedTables) {
                if (!invalidatedTables.containsKey(table)) {
                    invalidatedTables[table] = mutableListOf()
                }
                if (rowId != null) {
                    invalidatedTables[table]?.add(rowId)
                }
            }
            return
        }

        synchronized(this) {
            if (tableObservers.size > 0) {
                val vanishedObservers = mutableListOf<TableObserver>()

                for (tableObserver in tableObservers) {
                    if (tableObserver.table == table && (tableObserver.rowId == null || tableObserver.rowId == rowId)) {
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

    private fun beginTransaction() {
        val database = sqlite.writableDatabase
        if (!database.inTransaction()) {
            synchronized(invalidatedTables) {
                Log.i(javaClass.name, "Clear invalidated tables")
                invalidatedTables.clear()
            }
        }
        if (database.isWriteAheadLoggingEnabled) {
            database.beginTransactionNonExclusive()
        } else {
            database.beginTransaction()
        }
    }

    private fun setTransactionSuccessful() = sqlite.writableDatabase.setTransactionSuccessful()

    private fun endTransaction() {
        val database = sqlite.writableDatabase
        database.endTransaction()
        if (!database.inTransaction()) {
            synchronized(invalidatedTables) {
                for ((table, ids) in invalidatedTables) {
                    if (ids.isEmpty()) {
                        invalidateTable(table)
                    } else {
                        for (id in ids) {
                            invalidateTable(table, id)
                        }
                    }
                }
            }
        }
    }

    fun <T> createLiveData(observedTables: Array<ObservedTable>, loadData: () -> T): LiveData<T> {
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
            for (observedTable in observedTables) {
                tableObservers.add(TableObserver(observedTable.name, observedTable.rowId, liveData))
            }
        }

        return liveData
    }

    class ObservedTable(val name: String, val rowId: Any? = null) : Serializable

    private class TableObserver(val table: String, val rowId: Any?, listener: Listener) {

        private val reference = WeakReference(listener)

        val listener
            get() = reference.get()

        interface Listener {

            fun onInvalidated()

        }

    }

}
