package deltas.verilog

import core.deltas.{Delta, ParseUsingTextualGrammar}
import core.language.Language
import deltas.expression.IntLiteralDelta
import deltas.expressions.{ExpressionDelta, VariableDelta}
import deltas.statement.{BlockDelta, IfThenDelta, IfThenElseDelta, StatementDelta}

object VerilogLanguage {
  val deltas = Seq(AlwaysDelta, NonBlockingAssignmentDelta, BeginEndDelta, PortTypeSpecifierDelta, VerilogModuleDelta,
    IfThenElseDelta, IfThenDelta, BlockDelta, StatementDelta,
    IntLiteralDelta, VariableDelta, ExpressionDelta,
    ParseUsingTextualGrammar)
  val language: Language = Delta.buildLanguage(deltas)
}