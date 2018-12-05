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
            val parts = value.split(':', limit = 2)

            val name = parts[0]
            val params = if (parts.size == 2) parts[1] else null

            return when (name) {
                UPDATE_MODE__ADAPTIVE -> AdaptiveUpdateMode
                UPDATE_MODE__DEFAULT -> DefaultUpdateMode
                UPDATE_MODE__DISABLED -> DisabledUpdateMode
                UPDATE_MODE__ON_APP_LAUNCH -> OnAppLaunchUpdateMode
                UPDATE_MODE__REPEATING -> when (params) {
                    Every15MinutesUpdateMode.MINUTES -> Every15MinutesUpdateMode
                    Every30MinutesUpdateMode.MINUTES -> Every30MinutesUpdateMode
                    Every45MinutesUpdateMode.MINUTES -> Every45MinutesUpdateMode
                    EveryHourUpdateMode.MINUTES -> EveryHourUpdateMode
                    Every2HoursUpdateMode.MINUTES -> Every2HoursUpdateMode
                    Every3HoursUpdateMode.MINUTES -> Every3HoursUpdateMode
                    Every4HoursUpdateMode.MINUTES -> Every4HoursUpdateMode
                    Every6HoursUpdateMode.MINUTES -> Every6HoursUpdateMode
                    Every8HoursUpdateMode.MINUTES -> Every8HoursUpdateMode
                    else -> if (BuildConfig.DEBUG) throw IllegalArgumentException(value) else Every8HoursUpdateMode
                }
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

object Every15MinutesUpdateMode : UpdateMode() {
    const val MINUTES = "15"
    override fun serialize(): String = "$UPDATE_MODE__REPEATING:$MINUTES"
}

object Every30MinutesUpdateMode : UpdateMode() {
    const val MINUTES = "30"
    override fun serialize(): String = "$UPDATE_MODE__REPEATING:$MINUTES"
}

object Every45MinutesUpdateMode : UpdateMode() {
    const val MINUTES = "45"
    override fun serialize(): String = "$UPDATE_MODE__REPEATING:$MINUTES"
}

object EveryHourUpdateMode : UpdateMode() {
    const val MINUTES = "60"
    override fun serialize(): String = "$UPDATE_MODE__REPEATING:$MINUTES"
}

object Every2HoursUpdateMode : UpdateMode() {
    const val MINUTES = "120"
    override fun serialize(): String = "$UPDATE_MODE__REPEATING:$MINUTES"
}

object Every3HoursUpdateMode : UpdateMode() {
    const val MINUTES = "180"
    override fun serialize(): String = "$UPDATE_MODE__REPEATING:$MINUTES"
}

object Every4HoursUpdateMode : UpdateMode() {
    const val MINUTES = "240"
    override fun serialize(): String = "$UPDATE_MODE__REPEATING:$MINUTES"
}

object Every6HoursUpdateMode : UpdateMode() {
    const val MINUTES = "360"
    override fun serialize(): String = "$UPDATE_MODE__REPEATING:$MINUTES"
}

object Every8HoursUpdateMode : UpdateMode() {
    const val MINUTES = "480"
    override fun serialize(): String = "$UPDATE_MODE__REPEATING:$MINUTES"
}
