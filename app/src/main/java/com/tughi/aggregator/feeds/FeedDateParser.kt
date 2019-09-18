package com.tughi.aggregator.feeds

import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.HashMap
import java.util.TimeZone
import java.util.regex.Pattern

/**
 * An unified parser for feed dates.
 */
class FeedDateParser : DateParser {

    private val parsers = arrayOf(RssDateParser(), AtomDateParser())

    override fun parse(text: String): Date? {
        for (index in parsers.indices) {
            val parser = parsers[index]
            val date = parser.parse(text)

            if (date != null) {
                if (index > 0) {
                    // reorder
                    parsers[index] = parsers[0]
                    parsers[0] = parser
                }

                return date
            }
        }

        return null
    }

}

private interface DateParser {
    fun parse(text: String): Date?
}

/**
 * A custom regex-based [DateParser] for Atom dates.
 */
private class AtomDateParser : DateParser {

    private val calendar = Calendar.getInstance()

    override fun parse(text: String): Date? {
        val matcher = PATTERN.matcher(text)
        if (matcher.find()) {
            calendar.timeInMillis = 0

            val timezone = matcher.group(10)
            if (timezone == null || timezone == "Z" || timezone == "z") {
                calendar.timeZone = TimeZone.getTimeZone("GMT")
            } else {
                calendar.timeZone = TimeZone.getTimeZone("GMT$timezone")
            }

            if (matcher.group(4) != null) {
                val second = matcher.group(8)
                if (second != null) {
                    calendar.set(Calendar.SECOND, Integer.parseInt(second))
                }

                val minute = matcher.group(6)!!
                calendar.set(Calendar.MINUTE, Integer.parseInt(minute))

                val hour = matcher.group(5)!!
                calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour))
            }

            val date = matcher.group(3)!!
            calendar.set(Calendar.DATE, Integer.parseInt(date))

            val month = matcher.group(2)!!
            calendar.set(Calendar.MONTH, Integer.parseInt(month) - 1)

            val year = matcher.group(1)!!
            calendar.set(Calendar.YEAR, Integer.parseInt(year))

            return calendar.time
        }

        return null
    }

    companion object {

        private val PATTERN: Pattern

        init {
            val regex = "(\\d{4})-(\\d{2})-(\\d{2})([Tt](\\d{2}):(\\d{2})(:(\\d{2})(\\.\\d+)?)?([Zz]|[+-]\\d{2}:\\d{2}))?"
            PATTERN = Pattern.compile(regex)
        }
    }

}

/**
 * A custom regex-based [DateFormat] for RSS dates.
 */
private class RssDateParser : DateParser {

    private var calendar = Calendar.getInstance()

    override fun parse(text: String): Date? {
        val matcher = PATTERN.matcher(text)
        if (matcher.find()) {
            calendar.timeInMillis = 0

            val timezone = matcher.group(10)
            if (timezone == null) {
                calendar.timeZone = TimeZone.getTimeZone("GMT")
            } else if (timezone.startsWith("-") || timezone.startsWith("+")) {
                calendar.timeZone = TimeZone.getTimeZone("GMT$timezone")
            } else if (timezone == "EST") {
                calendar.timeZone = TimeZone.getTimeZone("GMT-05")
            } else if (timezone == "EDT") {
                calendar.timeZone = TimeZone.getTimeZone("GMT-04")
            } else if (timezone == "CST") {
                calendar.timeZone = TimeZone.getTimeZone("GMT-06")
            } else if (timezone == "CDT") {
                calendar.timeZone = TimeZone.getTimeZone("GMT-05")
            } else if (timezone == "MST") {
                calendar.timeZone = TimeZone.getTimeZone("GMT-07")
            } else if (timezone == "MDT") {
                calendar.timeZone = TimeZone.getTimeZone("GMT-06")
            } else if (timezone == "PST") {
                calendar.timeZone = TimeZone.getTimeZone("GMT-08")
            } else if (timezone == "PDT") {
                calendar.timeZone = TimeZone.getTimeZone("GMT-07")
            } else {
                calendar.timeZone = TimeZone.getTimeZone(timezone)
            }

            val second = matcher.group(9)
            if (second != null) {
                calendar.set(Calendar.SECOND, Integer.parseInt(second))
            }

            val minute = matcher.group(7)!!
            calendar.set(Calendar.MINUTE, Integer.parseInt(minute))

            val hour = matcher.group(6)!!
            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour))

            val date = matcher.group(3)!!
            calendar.set(Calendar.DATE, Integer.parseInt(date))

            val month = matcher.group(4)!!
            calendar.set(Calendar.MONTH, MONTH_MAP[month]!!)

            val year = matcher.group(5)!!
            calendar.set(Calendar.YEAR, Integer.parseInt(year))

            return calendar.time
        }

        return null
    }

    companion object {

        private val PATTERN: Pattern

        private const val MONDAY = "Mon"
        private const val TUESDAY = "Tue"
        private const val WEDNESDAY = "Wed"
        private const val THURSDAY = "Thu"
        private const val FRIDAY = "Fri"
        private const val SATURDAY = "Sat"
        private const val SUNDAY = "Sun"

        private const val JANUARY = "Jan"
        private const val FEBRUARY = "Feb"
        private const val MARCH = "Mar"
        private const val APRIL = "Apr"
        private const val MAY = "May"
        private const val JUNE = "Jun"
        private const val JULY = "Jul"
        private const val AUGUST = "Aug"
        private const val SEPTEMBER = "Sep"
        private const val OCTOBER = "Oct"
        private const val NOVEMBER = "Nov"
        private const val DECEMBER = "Dec"

        private val MONTH_MAP: MutableMap<String, Int>

        init {
            val dayValues = arrayOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY)
            val monthValues = arrayOf(JANUARY, FEBRUARY, MARCH, APRIL, MAY, JUNE, JULY, AUGUST, SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER)
            val timezones = arrayOf("-\\d{4}", "\\+\\d{4}", "GMT\\+\\d+", "GMT-\\d+", "GMT", "UTC\\+\\d+", "UTC-\\d+", "UTC", "EST", "EDT", "CST", "CDT", "MST", "MDT", "PST", "PDT")

            val regex = "((" + join(dayValues, "|") + "), )?(\\d{1,2}) (" + join(monthValues, "|") + ") (\\d{4}) (\\d{2}):(\\d{2})(:(\\d{2}))? ?(" + join(timezones, "|") + ")?"
            PATTERN = Pattern.compile(regex)

            MONTH_MAP = HashMap(12)
            MONTH_MAP[JANUARY] = Calendar.JANUARY
            MONTH_MAP[FEBRUARY] = Calendar.FEBRUARY
            MONTH_MAP[MARCH] = Calendar.MARCH
            MONTH_MAP[APRIL] = Calendar.APRIL
            MONTH_MAP[MAY] = Calendar.MAY
            MONTH_MAP[JUNE] = Calendar.JUNE
            MONTH_MAP[JULY] = Calendar.JULY
            MONTH_MAP[AUGUST] = Calendar.AUGUST
            MONTH_MAP[SEPTEMBER] = Calendar.SEPTEMBER
            MONTH_MAP[OCTOBER] = Calendar.OCTOBER
            MONTH_MAP[NOVEMBER] = Calendar.NOVEMBER
            MONTH_MAP[DECEMBER] = Calendar.DECEMBER
        }

        private fun join(values: Array<String>, delimiter: String): String {
            val result = StringBuilder()
            for (value in values) {
                result.append(delimiter).append(value)
            }
            return result.substring(delimiter.length)
        }
    }

}
