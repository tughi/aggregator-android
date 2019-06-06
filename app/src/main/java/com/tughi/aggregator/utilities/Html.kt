package com.tughi.aggregator.utilities

object Html {

    fun encode(text: String): String {
        val textLength = text.length
        if (textLength == 0) {
            return text
        }

        val encoded = StringBuilder(textLength)

        for (index in 0 until textLength) {
            val char = text.get(index)
            when (char) {
                '<' -> encoded.append("&lt;")
                '&' -> encoded.append("&amp;")
                else -> encoded.append(char)
            }
        }

        return encoded.toString()
    }

}