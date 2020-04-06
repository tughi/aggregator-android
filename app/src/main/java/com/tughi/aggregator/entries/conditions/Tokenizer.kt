package com.tughi.aggregator.entries.conditions

class Tokenizer(val condition: CharSequence) {
    private var tokenStart = 0
    private var tokenEnd = 0

    private enum class State {
        PAREN_LEFT,
        PAREN_RIGHT,
        START,
        STRING,
        STRING_END,
        STRING_ESCAPE,
        UNKNOWN,
        WORD,
    }

    fun next(): Token {
        tokenStart = tokenEnd
        var state = State.START
        var string: StringBuilder? = null
        while (true) {
            val char = if (tokenEnd < condition.length) condition[tokenEnd] else 0
            when (state) {
                State.PAREN_LEFT -> {
                    return LeftParenToken(tokenStart)
                }
                State.PAREN_RIGHT -> {
                    return RightParenToken(tokenStart)
                }
                State.START -> when (char) {
                    ' ' -> {
                        tokenStart = tokenEnd + 1
                    }
                    in 'a'..'z', in 'A'..'Z' -> {
                        state = State.WORD
                    }
                    '"' -> {
                        string = StringBuilder()
                        state = State.STRING
                    }
                    '(' -> {
                        state = State.PAREN_LEFT
                    }
                    ')' -> {
                        state = State.PAREN_RIGHT
                    }
                    0 -> {
                        return EndOfCondition(tokenStart)
                    }
                    else -> {
                        state = State.UNKNOWN
                    }
                }
                State.STRING -> when (char) {
                    '"' -> {
                        state = State.STRING_END
                    }
                    '\\' -> {
                        state = State.STRING_ESCAPE
                    }
                    0 -> {
                        return UnsupportedToken(condition.substring(tokenStart, tokenEnd), tokenStart, tokenEnd)
                    }
                    else -> {
                        string!!.append(char)
                    }
                }
                State.STRING_END -> {
                    return StringToken(condition.substring(tokenStart, tokenEnd), tokenStart, tokenEnd, string!!.toString())
                }
                State.STRING_ESCAPE -> when (char) {
                    '\\', '\'', '\"' -> {
                        string!!.append(char)
                        state = State.STRING
                    }
                    else -> {
                        return UnsupportedToken(condition.substring(tokenStart, tokenEnd), tokenStart, tokenEnd)
                    }
                }
                State.UNKNOWN -> {
                    return UnsupportedToken(condition.substring(tokenStart, tokenEnd), tokenStart, tokenEnd)
                }
                State.WORD -> when (char) {
                    in 'a'..'z', in 'A'..'Z' -> {
                    }
                    else -> return when (val lexeme = condition.substring(tokenStart, tokenEnd)) {
                        "and" -> AndToken(tokenStart)
                        "contains" -> ContainsToken(tokenStart)
                        "ends" -> EndsToken(tokenStart)
                        "is" -> IsToken(tokenStart)
                        "or" -> OrToken(tokenStart)
                        "starts" -> StartsToken(tokenStart)
                        "title" -> TitleToken(tokenStart)
                        "with" -> WithToken(tokenStart)
                        else -> UnsupportedToken(lexeme, tokenStart, tokenEnd)
                    }
                }
            }
            tokenEnd += 1
        }
    }
}