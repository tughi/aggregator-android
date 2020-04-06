package com.tughi.aggregator.entries.conditions

class Parser(private val tokens: List<Token>) {
    private var index = -1

    private fun peek() = tokens[index + 1]

    private inline fun <reified T> consume(): T {
        index += 1
        val token = tokens[index]
        if (token is T) {
            return token
        }
        throw UnexpectedTokenException(token)
    }

    fun parse(): Expression {
        if (peek() is EndOfCondition) {
            return EmptyExpression
        }
        val expression = parseSimpleExpression()
        consume<EndOfCondition>()
        return expression
    }

    private fun parseSimpleExpression(): Expression {
        val property = when (val token = consume<Token>()) {
            is TitleToken -> TitleProperty
            else -> throw UnexpectedTokenException(token)
        }
        val operator = when (val token = consume<Token>()) {
            is ContainsToken -> ContainsOperator
            is EndsToken -> when (val token2 = consume<Token>()) {
                is WithToken -> EndsWithOperator
                else -> throw UnexpectedTokenException(token2)
            }
            is IsToken -> IsOperator
            is StartsToken -> when (val token2 = consume<Token>()) {
                is WithToken -> StartsWithOperator
                else -> throw UnexpectedTokenException(token2)
            }
            else -> throw UnexpectedTokenException(token)
        }
        val value = consume<StringToken>().value
        return SimpleExpression(property, operator, value)
    }

    class UnexpectedTokenException(token: Token) : Exception() {
        init {
            token.isUnexpected = true
        }
    }
}
