package com.tughi.aggregator

sealed class UpdateMode

object AutoUpdateMode : UpdateMode()

object DefaultUpdateMode : UpdateMode()

object DisabledUpdateMode : UpdateMode()

data class RepeatingUpdateMode(val millis: Long) : UpdateMode()
