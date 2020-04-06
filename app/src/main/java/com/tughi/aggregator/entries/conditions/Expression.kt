package com.tughi.aggregator.entries.conditions

sealed class Expression

object EmptyExpression : Expression()
object InvalidExpression : Expression()

sealed class Property
object TitleProperty : Property()

sealed class Operator
object ContainsOperator : Operator()
object EndsWithOperator : Operator()
object IsOperator : Operator()
object StartsWithOperator : Operator()

class SimpleExpression(val property: Property, val operator: Operator, val value: String) : Expression()
