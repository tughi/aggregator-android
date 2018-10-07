package com.tughi.aggregator.data

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface EntryDao {

    @Insert
    fun addEntry(entry: Entry): Long

}
