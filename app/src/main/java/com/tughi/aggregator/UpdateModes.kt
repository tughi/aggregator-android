package com.tughi.aggregator

private const val UPDATE_MODE__AUTO = "AUTO"
private const val UPDATE_MODE__DEFAULT = "DEFAULT"
private const val UPDATE_MODE__DISABLED = "DISABLED"
private const val UPDATE_MODE__REPEATING = "REPEATING"

sealed class UpdateMode {
    abstract fun serialize(): String

    companion object {
        fun deserialize(value: String): UpdateMode {
            val parts = value.split(':', limit = 1)

            val prefix = parts[0]
            val params = if (parts.size == 2) parts[1] else null

            return when (prefix) {
                UPDATE_MODE__AUTO -> AutoUpdateMode
                UPDATE_MODE__DEFAULT -> DefaultUpdateMode
                UPDATE_MODE__DISABLED -> DisabledUpdateMode
                UPDATE_MODE__REPEATING -> RepeatingUpdateMode(params ?: "60")
                else -> throw IllegalArgumentException(value)
            }
        }
    }
}

object AutoUpdateMode : UpdateMode() {
    override fun serialize(): String = UPDATE_MODE__AUTO
}

object DefaultUpdateMode : UpdateMode() {
    override fun serialize(): String = UPDATE_MODE__DEFAULT
}

object DisabledUpdateMode : UpdateMode() {
    override fun serialize(): String = UPDATE_MODE__DISABLED
}

data class RepeatingUpdateMode(val minutes: Int = 60) : UpdateMode() {
    constructor(params: String) : this(params.toInt())

    override fun serialize(): String = "$UPDATE_MODE__REPEATING:$minutes"
}
