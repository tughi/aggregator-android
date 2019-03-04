package com.tughi.aggregator.data

object CustomTypeConverters {

//    @TypeConverter
    @JvmStatic
    fun deserializeUpdateMode(updateMode: String): UpdateMode {
        return UpdateMode.deserialize(updateMode)
    }

//    @TypeConverter
    @JvmStatic
    fun serializeUpdateMode(updateMode: UpdateMode): String {
        return updateMode.serialize()
    }

}
