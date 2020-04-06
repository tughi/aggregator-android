package com.tughi.aggregator.entries.conditions

sealed class Token(val lexeme: String, val startIndex: Int, val endIndex: Int, var isUnexpected: Boolean = false)

class EndOfCondition(index: Int) : Token("", index, index)

class UnsupportedToken(lexeme: String, startIndex: Int, endIndex: Int) : Token(lexeme, startIndex, endIndex, true)

class WordToken(lexeme: String, startIndex: Int, endIndex: Int, val isProperty: Boolean = false) : Token(lexeme, startIndex, endIndex)

class StringToken(lexeme: String, startIndex: Int, endIndex: Int, val value: String) : Token(lexeme, startIndex, endIndex)

class LeftParenToken(index: Int) : Token("(", index, index + 1)

class RightParenToken(index: Int) : Token(")", index, index + 1)
