package com.tughi.aggregator.data

import java.io.Serializable

sealed class EntriesSortOrder : Serializable {
    abstract fun serialize(): String

    companion object {
        fun deserialize(value: String) = when (value) {
            "date-asc" -> EntriesSortOrderByDateAsc
            "date-desc" -> EntriesSortOrderByDateDesc
            "title-asc" -> EntriesSortOrderByTitle
            else -> EntriesSortOrderByDateAsc
        }
    }
}

object EntriesSortOrderByDateAsc : EntriesSortOrder() {
    override fun serialize(): String = "date-asc"
}

object EntriesSortOrderByDateDesc : EntriesSortOrder() {
    override fun serialize(): String = "date-desc"
}

object EntriesSortOrderByTitle : EntriesSortOrder() {
    override fun serialize(): String = "title-asc"
}
