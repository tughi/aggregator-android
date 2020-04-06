package com.tughi.aggregator.entries.conditions

class Condition(val text: CharSequence) {
    val tokens: List<Token>
    var hasUnexpectedTokens = false
        private set

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
    }

    override fun toString(): String = tokens.joinToString(" ") { it.lexeme }
}