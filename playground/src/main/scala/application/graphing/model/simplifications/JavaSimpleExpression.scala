package application.graphing.model.simplifications

import core.deltas.Contract
import deltas.expression.IntLiteralDelta
import deltas.javac.expressions._
import deltas.javac.expressions.additive.{AdditionDelta, SubtractionDelta}
import deltas.javac.expressions.equality.EqualityDelta
import deltas.javac.expressions.literals.{BooleanLiteralDelta, NullDelta}
import deltas.javac.expressions.relational.LessThanDelta
import deltas.javac.methods.assignment.AssignmentPrecedence
import deltas.javac.statements.StatementSkeleton

object JavaSimpleExpression extends DeltaGroup {

  override def dependencies: Set[Contract] =
    Set(LessThanDelta, AdditionDelta, BooleanLiteralDelta, IntLiteralDelta, SubtractionDelta, TernaryDelta, ParenthesisDelta, NullDelta, EqualityDelta) ++
      Set[Contract](AssignmentPrecedence) //TODO not sure

  override def dependants: Set[Contract] = Set(StatementSkeleton)
}