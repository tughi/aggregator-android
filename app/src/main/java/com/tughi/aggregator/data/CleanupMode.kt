package com.tughi.aggregator.data

sealed class CleanupMode {
    abstract fun serialize(): String

    companion object {
        fun deserialize(value: String?): CleanupMode {
            return when (value ?: "") {
                DefaultCleanupMode.VALUE -> DefaultCleanupMode
                Age3DaysCleanupMode.VALUE -> Age3DaysCleanupMode
                Age1WeekCleanupMode.VALUE -> Age1WeekCleanupMode
                Age1MonthCleanupMode.VALUE -> Age1MonthCleanupMode
                Age3MonthsCleanupMode.VALUE -> Age3MonthsCleanupMode
                Age6MonthsCleanupMode.VALUE -> Age6MonthsCleanupMode
                Age1YearCleanupMode.VALUE -> Age1YearCleanupMode
                Age3YearsCleanupMode.VALUE -> Age3YearsCleanupMode
                Age6YearsCleanupMode.VALUE -> Age6YearsCleanupMode
                NeverCleanupMode.VALUE -> NeverCleanupMode
                else -> Age1MonthCleanupMode
            }
        }
    }
}

object DefaultCleanupMode : CleanupMode() {
    internal const val VALUE = "DEFAULT"

    override fun serialize() = VALUE
}

object Age3DaysCleanupMode : CleanupMode() {
    internal const val VALUE = "AGE:DAYS:3"

    override fun serialize() = VALUE
}

object Age1WeekCleanupMode : CleanupMode() {
    internal const val VALUE = "AGE:WEEKS:1"

    override fun serialize() = VALUE
}

object Age1MonthCleanupMode : CleanupMode() {
    internal const val VALUE = "AGE:MONTHS:1"

    override fun serialize() = VALUE
}

object Age3MonthsCleanupMode : CleanupMode() {
    internal const val VALUE = "AGE:MONTHS:3"

    override fun serialize() = VALUE
}

object Age6MonthsCleanupMode : CleanupMode() {
    internal const val VALUE = "AGE:MONTHS:6"

    override fun serialize() = VALUE
}

object Age1YearCleanupMode : CleanupMode() {
    internal const val VALUE = "AGE:YEARS:1"

    override fun serialize() = VALUE
}

object Age3YearsCleanupMode : CleanupMode() {
    internal const val VALUE = "AGE:YEARS:3"

    override fun serialize() = VALUE
}

object Age6YearsCleanupMode : CleanupMode() {
    internal const val VALUE = "AGE:YEARS:6"

    override fun serialize() = VALUE
}

object NeverCleanupMode : CleanupMode() {
    internal const val VALUE = "NEVER"

    override fun serialize() = VALUE
}

