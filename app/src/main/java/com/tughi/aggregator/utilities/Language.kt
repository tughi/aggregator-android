package com.tughi.aggregator.utilities

import java.util.regex.Pattern

object Language {

    private val RTL_LANGUAGE_PATTERN = Pattern.compile("(ar|fa|he|ku|ur)([-_].*)?")

    fun isRightToLeft(language: String?): Boolean {
        return language != null && RTL_LANGUAGE_PATTERN.matcher(language).matches()
    }

}
