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
        val expression = parseOrExpression()
        consume<EndOfCondition>()
        return expression
    }

    private fun parseOrExpression(): Expression {
        var expression = parseAndExpression()
        while (peek() is OrToken) {
            consume<OrToken>()
            val secondExpression = parseAndExpression()
            expression = BooleanExpression(expression, BooleanExpression.Operator.OR, secondExpression)
        }
        return expression
    }

    private fun parseAndExpression(): Expression {
        var expression = parseSimpleExpression()
        while (peek() is AndToken) {
            consume<AndToken>()
            val secondExpression = parseSimpleExpression()
            expression = BooleanExpression(expression, BooleanExpression.Operator.AND, secondExpression)
        }
        return expression
    }

    private fun parseSimpleExpression(): Expression {
        val property = when (val token = consume<Token>()) {
            is TitleToken -> PropertyExpression.Property.TITLE
            else -> throw UnexpectedTokenException(token)
        }
        val operator = when (val token = consume<Token>()) {
            is ContainsToken -> PropertyExpression.Operator.CONTAINS
            is EndsToken -> when (val token2 = consume<Token>()) {
                is WithToken -> PropertyExpression.Operator.ENDS_WITH
                else -> throw UnexpectedTokenException(token2)
            }
            is IsToken -> PropertyExpression.Operator.IS
            is StartsToken -> when (val token2 = consume<Token>()) {
                is WithToken -> PropertyExpression.Operator.STARTS_WITH
                else -> throw UnexpectedTokenException(token2)
            }
            else -> throw UnexpectedTokenException(token)
        }
        val value = consume<StringToken>().value
        return PropertyExpression(property, operator, value)
    }

    class UnexpectedTokenException(token: Token) : Exception() {
        init {
            token.isUnexpected = true
        }
    }
}
