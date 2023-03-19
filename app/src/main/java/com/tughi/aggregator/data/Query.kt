package com.tughi.aggregator.data

import androidx.sqlite.db.SupportSQLiteProgram
import androidx.sqlite.db.SupportSQLiteQuery
import com.tughi.aggregator.BuildConfig
import java.util.StringTokenizer

class Query(private val query: String, private val queryArgs: Array<Any?> = emptyArray(), val observedTables: Set<String>) : SupportSQLiteQuery {

    override val sql: String
        get() = query

    override fun bindTo(statement: SupportSQLiteProgram) {
        queryArgs.forEachIndexed { index, any ->
            when (any) {
                null -> statement.bindNull(index + 1)
                is ByteArray -> statement.bindBlob(index + 1, any)
                is Double -> statement.bindDouble(index + 1, any)
                is Int -> statement.bindLong(index + 1, any.toLong())
                is Long -> statement.bindLong(index + 1, any)
                is String -> statement.bindString(index + 1, any)
                else -> throw UnsupportedOperationException("Cannot bind ${any::class} argument")
            }
        }
    }

    override val argCount: Int
        get() = queryArgs.size

    class Builder(private val columns: Array<out Repository.Column>, private val from: String) {
        companion object {
            val databaseTables = setOf("entry", "entry_tag", "entry_tag_rule", "feed", "my_feed_tag", "tag")
        }

        private val observedTables = mutableSetOf<String>().also {
            for (token in StringTokenizer(from)) {
                if (token in databaseTables) {
                    it.add(token as String)
                }
            }

            for (column in columns) {
                it.addAll(column.projectionTables)
            }
        }

        private var distinct: Boolean = false
        private var where: String? = null
        private var whereArgs: Array<Any?> = emptyArray()
        private var groupBy: String? = null
        private var orderBy: String? = null
        private var limit: Int? = null
        private var offset: Int? = null

        fun addObservedTables(vararg tables: String) {
            for (table in tables) {
                if (BuildConfig.DEBUG) {
                    if (!databaseTables.contains(table)) {
                        throw IllegalArgumentException("Not a database table: $table")
                    }
                }

                observedTables.add(table)
            }
        }

        fun where(where: String, whereArgs: Array<Any?>): Builder {
            this.where = where
            this.whereArgs = whereArgs
            return this
        }

        fun groupBy(groupBy: String): Builder {
            this.groupBy = groupBy
            return this
        }

        fun orderBy(orderBy: String): Builder {
            this.orderBy = orderBy
            return this
        }

        fun limit(limit: Int): Builder {
            this.limit = limit
            return this
        }

        fun offset(offset: Int): Builder {
            this.offset = offset
            return this
        }

        private fun createQuery(): String {
            val query = StringBuilder()

            query.append("SELECT ")
            if (distinct) {
                query.append("DISTINCT ")
            }
            if (columns.isEmpty()) {
                query.append("*")
            } else {
                query.append(Array(columns.size) { "${columns[it].projection} AS ${columns[it].name}" }.joinToString(separator = ", "))
            }
            query.append(" FROM ").append(from)

            if (where != null) query.append(" WHERE ").append(where)
            if (groupBy != null) query.append(" GROUP BY ").append(groupBy)
            if (orderBy != null) query.append(" ORDER BY ").append(orderBy)

            limit?.also { query.append(" LIMIT ").append(it) }
            offset?.also { query.append(" OFFSET ").append(it) }

            return query.toString()
        }

        fun build(): Query {
            val query = createQuery()
            return Query(query = query, queryArgs = whereArgs, observedTables = observedTables)
        }

        fun buildCount(): Query {
            val query = "SELECT COUNT(1) FROM (${createQuery()})"
            return Query(query = query, queryArgs = whereArgs, observedTables = observedTables)
        }
    }

}
