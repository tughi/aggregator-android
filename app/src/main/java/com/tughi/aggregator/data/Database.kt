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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.ref.WeakReference

object Database {

    private val sqlite: SupportSQLiteOpenHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(App.instance)
                    .name(DATABASE_NAME)
                    .callback(object : SupportSQLiteOpenHelper.Callback(21) {
                        override fun onConfigure(db: SupportSQLiteDatabase?) {
                            db?.apply {
                                setForeignKeyConstraintsEnabled(true)
                                enableWriteAheadLogging()
                            }
                        }

                        override fun onCreate(database: SupportSQLiteDatabase?) {
                            if (database == null) {
                                throw IllegalStateException()
                            }

                            executeScript(database, 0)
                        }

                        override fun onUpgrade(database: SupportSQLiteDatabase?, oldVersion: Int, newVersion: Int) {
                            if (database == null) {
                                throw IllegalStateException()
                            }

                            var databaseVersion = database.version
                            while (databaseVersion != newVersion) {
                                executeScript(database, databaseVersion)

                                databaseVersion = database.version
                            }
                        }

                        private fun executeScript(database: SupportSQLiteDatabase, version: Int) {
                            val scriptVersion = when {
                                version > 99 -> version.toString()
                                version > 9 -> "0$version"
                                else -> "00$version"
                            }
                            val scriptFile = "database/$scriptVersion.sql"
                            val scriptStatements = mutableListOf<String>()

                            try {
                                BufferedReader(InputStreamReader(App.instance.assets.open(scriptFile))).use { scriptReader ->
                                    val statement = StringBuilder()

                                    var line = scriptReader.readLine()
                                    while (line != null) {
                                        val trimmedLine = line.trim()
                                        if (trimmedLine.isNotEmpty()) {
                                            if (trimmedLine.startsWith("--")) {
                                                if (statement.isNotEmpty()) {
                                                    scriptStatements.add(statement.toString())
                                                }
                                                statement.clear()
                                            } else {
                                                statement.append(line).append('\n')
                                            }
                                        }

                                        line = scriptReader.readLine()
                                    }

                                    if (statement.isNotEmpty()) {
                                        scriptStatements.add(statement.toString())
                                    }
                                }
                            } catch (exception: IOException) {
                                dropDatabase(database, "Cannot upgrade database from version $version")
                            }

                            database.transaction {
                                for (statement in scriptStatements) {
                                    database.execSQL(statement)
                                }
                            }
                        }

                        override fun onDowngrade(database: SupportSQLiteDatabase?, oldVersion: Int, newVersion: Int) {
                            dropDatabase(database, "Cannot downgrade from version $oldVersion to $newVersion")
                        }

                        private fun dropDatabase(database: SupportSQLiteDatabase?, message: String) {
                            Log.e(javaClass.name, message)
                            onCorruption(database)
                            System.exit(1)
                        }

                        override fun onOpen(db: SupportSQLiteDatabase?) {
                            GlobalScope.launch(Dispatchers.IO) {
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

        synchronized(tableObservers) {
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
                GlobalScope.launch(Dispatchers.IO) {
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

}
