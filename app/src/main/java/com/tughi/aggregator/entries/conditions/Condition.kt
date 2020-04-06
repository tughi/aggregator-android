package com.tughi.aggregator.entries.conditions

class Condition(val text: CharSequence) {
    val tokens: List<Token>
    var hasUnexpectedTokens = false
        private set

    val expression: Expression

    init {
        val tokens = mutableListOf<Token>()
        val tokenizer = Tokenizer(text)
        while (true) {
            val token = tokenizer.next()
            tokens.add(token)
            if (token.isUnexpected) {
                hasUnexpectedTokens = true
            }
            if (token is EndOfCondition) {
                break
            }
        }
        this.tokens = tokens

        expression = try {
            Parser(tokens).parse()
        } catch (exception: Parser.UnexpectedTokenException) {
            hasUnexpectedTokens = true
            InvalidExpression
        }
    }

    override fun toString(): String = tokens.subList(0, tokens.size - 1).joinToString(" ") { it.lexeme }
}
