package com.tughi.aggregator.activities.optionpicker

import android.content.Intent
import android.os.Build

fun Intent.getOptionExtra(name: String): Option? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.getParcelableExtra(name, Option::class.java)
    } else {
        @Suppress("DEPRECATION")
        this.getParcelableExtra(name)
    }

fun Intent.getOptionArrayExtra(name: String): Array<Option>? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.getParcelableArrayExtra(name, Option::class.java)
    } else {
        @Suppress("DEPRECATION")
        this.getParcelableArrayExtra(name)?.map { it as Option }?.toTypedArray()
    }
