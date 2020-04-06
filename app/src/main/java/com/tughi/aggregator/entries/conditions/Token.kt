package com.tughi.aggregator.entries.conditions

sealed class Token(val lexeme: String, val startIndex: Int, val endIndex: Int, var isUnexpected: Boolean = false)

class EndOfCondition(index: Int) : Token("", index, index)

class UnsupportedToken(lexeme: String, startIndex: Int, endIndex: Int) : Token(lexeme, startIndex, endIndex, true)

class StringToken(lexeme: String, startIndex: Int, endIndex: Int, val value: String) : Token(lexeme, startIndex, endIndex)

class LeftParenToken(index: Int) : Token("(", index, index + 1)
class RightParenToken(index: Int) : Token(")", index, index + 1)

class TitleToken(index: Int) : Token("title", index, index + "title".length)

class AndToken(index: Int) : Token("and", index, index + "and".length)
class OrToken(index: Int) : Token("or", index, index + "or".length)

class ContainsToken(index: Int) : Token("contains", index, index + "contains".length)
class EndsToken(index: Int) : Token("ends", index, index + "ends".length)
class IsToken(index: Int) : Token("is", index, index + "is".length)
class StartsToken(index: Int) : Token("starts", index, index + "starts".length)
class WithToken(index: Int) : Token("with", index, index + "with".length)
