package com.tughi.aggregator.entries.conditions

sealed class Expression

object EmptyExpression : Expression()
object InvalidExpression : Expression()

class PropertyExpression(val property: Property, val operator: Operator, val value: String) : Expression() {
    enum class Property {
        TITLE,
    }

    enum class Operator {
        CONTAINS,
        ENDS_WITH,
        IS,
        STARTS_WITH,
    }
}

class BooleanExpression(val left: Expression, val operator: Operator, val right: Expression) : Expression() {
    enum class Operator {
        AND,
        OR,
    }
}
