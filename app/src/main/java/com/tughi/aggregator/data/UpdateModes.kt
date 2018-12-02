package com.tughi.aggregator.data

import com.tughi.aggregator.BuildConfig

private const val UPDATE_MODE__ADAPTIVE = "ADAPTIVE"
private const val UPDATE_MODE__DEFAULT = "DEFAULT"
private const val UPDATE_MODE__DISABLED = "DISABLED"
private const val UPDATE_MODE__ON_APP_LAUNCH = "ON_APP_LAUNCH"
private const val UPDATE_MODE__REPEATING = "REPEATING"

sealed class UpdateMode {
    abstract fun serialize(): String

    companion object {
        fun deserialize(value: String): UpdateMode {
            val parts = value.split(':', limit = 1)

            val name = parts[0]
            val params = if (parts.size == 2) parts[1] else null

            return when (name) {
                UPDATE_MODE__ADAPTIVE -> AdaptiveUpdateMode
                UPDATE_MODE__DEFAULT -> DefaultUpdateMode
                UPDATE_MODE__DISABLED -> DisabledUpdateMode
                UPDATE_MODE__ON_APP_LAUNCH -> OnAppLaunchUpdateMode
                UPDATE_MODE__REPEATING -> RepeatingUpdateMode(params ?: "60")
                else -> if (BuildConfig.DEBUG) throw IllegalArgumentException(value) else DisabledUpdateMode
            }
        }
    }
}

object AdaptiveUpdateMode : UpdateMode() {
    override fun serialize(): String = UPDATE_MODE__ADAPTIVE
}

object DefaultUpdateMode : UpdateMode() {
    override fun serialize(): String = UPDATE_MODE__DEFAULT
}

object DisabledUpdateMode : UpdateMode() {
    override fun serialize(): String = UPDATE_MODE__DISABLED
}

object OnAppLaunchUpdateMode : UpdateMode() {
    override fun serialize(): String = UPDATE_MODE__ON_APP_LAUNCH
}

data class RepeatingUpdateMode(val minutes: Int = 60) : UpdateMode() {
    constructor(params: String) : this(params.toInt())

    override fun serialize(): String = "$UPDATE_MODE__REPEATING:$minutes"
}
