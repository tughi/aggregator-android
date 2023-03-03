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
import com.tughi.aggregator.DATABASE_NAME
import com.tughi.aggregator.contentScope
import com.tughi.aggregator.utilities.restoreFeeds
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import kotlin.system.exitProcess

object Database {

    private val sqlite: SupportSQLiteOpenHelper = FrameworkSQLiteOpenHelperFactory().create(
        SupportSQLiteOpenHelper.Configuration.builder(App.instance)
            .name(DATABASE_NAME)
            .callback(object : SupportSQLiteOpenHelper.Callback(23) {
                override fun onConfigure(db: SupportSQLiteDatabase) {
                    db.setForeignKeyConstraintsEnabled(true)
                    db.enableWriteAheadLogging()
                }

                override fun onCreate(database: SupportSQLiteDatabase) {
                    executeScript(database, 0)
                }

                override fun onUpgrade(database: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
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

                override fun onDowngrade(database: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    dropDatabase(database, "Cannot downgrade from version $oldVersion to $newVersion")
                }

                private fun dropDatabase(database: SupportSQLiteDatabase, message: String) {
                    Log.e(javaClass.name, message)
                    onCorruption(database)
                    exitProcess(1)
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    contentScope.launch {
                        restoreFeeds()
                    }
                }
            })
            .build()
    )

    private fun insert(table: String, values: ContentValues, conflictAlgorithm: Int): Long {
        val id = sqlite.writableDatabase.insert(table, conflictAlgorithm, values)
        if (id != -1L) {
            invalidate(table)
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
            invalidate(table)
        }
        return result
    }

    fun delete(table: String, selection: String?, selectionArgs: Array<Any>?): Int {
        val result = sqlite.writableDatabase.delete(table, selection, selectionArgs)
        if (result > 0) {
            invalidate(table)
        }
        return result
    }

    private val lastInvalidationTime = mutableMapOf<String, Long>()

    private val invalidationObservers = mutableSetOf<InvalidationObserver>()

    private val transactionInvalidations = mutableSetOf<String>()

    private fun invalidate(vararg tables: String) {
        if (sqlite.writableDatabase.inTransaction()) {
            synchronized(transactionInvalidations) {
                transactionInvalidations.addAll(tables)
            }
        } else {
            synchronized(lastInvalidationTime) {
                val now = System.currentTimeMillis()
                tables.forEach {
                    lastInvalidationTime[it] = now
                }
            }

            synchronized(invalidationObservers) {
                if (invalidationObservers.size > 0) {
                    for (tableObserver in invalidationObservers) {
                        tableObserver.onInvalidated(tables)
                    }
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
            val tables: Array<String>
            synchronized(transactionInvalidations) {
                tables = transactionInvalidations.toTypedArray()
                transactionInvalidations.clear()
            }
            invalidate(*tables)
        }
    }

    fun <T> forEach(sqliteQuery: SupportSQLiteQuery, createRow: (Cursor) -> T, block: (T) -> Unit) {
        sqlite.readableDatabase.query(sqliteQuery).use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    block(createRow(cursor))
                } while (cursor.moveToNext())
            }
        }
    }

    fun <T> query(sqliteQuery: SupportSQLiteQuery, transform: (Cursor) -> T): T {
        sqlite.readableDatabase.query(sqliteQuery).use { cursor ->
            return transform(cursor)
        }
    }

    fun <T> liveQuery(query: Query, transform: (Cursor) -> T): LiveData<T> {
        return object : LiveData<T>(), InvalidationObserver {
            private var inactivationTime = 0L

            override fun onActive() {
                synchronized(invalidationObservers) {
                    invalidationObservers.add(this)
                }

                var invalidated = false
                synchronized(lastInvalidationTime) {
                    query.observedTables.forEach {
                        val time = lastInvalidationTime[it]
                        if (time == null) {
                            lastInvalidationTime[it] = System.currentTimeMillis()
                            invalidated = true
                        } else if (time > inactivationTime) {
                            invalidated = true
                        }
                    }
                }

                if (invalidated) {
                    update()
                }
            }

            override fun onInactive() {
                synchronized(invalidationObservers) {
                    invalidationObservers.remove(this)
                }
                inactivationTime = System.currentTimeMillis()
            }

            override fun onInvalidated(tables: Array<out String>) {
                for (index in tables.indices) {
                    if (query.observedTables.contains(tables[index])) {
                        update()
                        break
                    }
                }
            }

            private fun update() {
                contentScope.launch {
                    sqlite.readableDatabase.query(query).use { cursor ->
                        val data = transform(cursor)
                        postValue(data)
                    }
                }
            }
        }
    }

    private interface InvalidationObserver {
        fun onInvalidated(tables: Array<out String>)
    }

}
