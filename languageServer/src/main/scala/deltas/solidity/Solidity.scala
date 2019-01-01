package deltas.solidity

import core.deltas.{LanguageFromDeltas, ParseUsingTextualGrammar}
import deltas.expression.IntLiteralDelta
import deltas.expressions.{ExpressionDelta, VariableDelta}
import deltas.javac.CallVariableDelta
import deltas.javac.classes.SelectFieldDelta
import deltas.javac.expressions.additive.{AdditionDelta, AdditivePrecedenceDelta, SubtractionDelta}
import deltas.javac.expressions.relational.{AddRelationalPrecedenceDelta, EqualsComparisonDelta, GreaterThanDelta, GreaterThanOrEqualDelta}
import deltas.javac.methods.{MemberSelectorDelta, ReturnExpressionDelta}
import deltas.javac.methods.assignment._
import deltas.javac.methods.call.CallDelta
import deltas.javac.statements.ExpressionAsStatementDelta
import deltas.javac.trivia.SlashSlashLineCommentsDelta
import deltas.statement.{BlockDelta, IfThenDelta, StatementDelta}

object Solidity {

  private val genericDeltas = Seq(
    SlashSlashLineCommentsDelta,
    CallVariableDelta, CallDelta, MemberSelectorDelta,
    IfThenDelta,
    BlockDelta, ReturnExpressionDelta, ExpressionAsStatementDelta, StatementDelta,
    SelectFieldDelta, MemberSelectorDelta,
    EqualsComparisonDelta,
    GreaterThanOrEqualDelta, GreaterThanDelta, AddRelationalPrecedenceDelta,
    DecrementAssignmentDelta, SubtractionDelta,
    IncrementAssignmentDelta, AdditionDelta, AdditivePrecedenceDelta,
    AssignToVariable, VariableDelta, EqualsAssignmentDelta, AssignmentPrecedence,
    IntLiteralDelta,
    ExpressionDelta)

  val deltas = Seq(ParseUsingTextualGrammar,
    NumberLiteralUnitsDelta,
    EmitStatementDelta,
    UsingForDeclarationDelta, EventDelta, CustomModifierDelta, EnumDelta,
    SolidityConstructorDelta, SolidityFunctionDelta, StateVariableDeclarationDelta) ++
    Seq(SolidityContractDelta, PragmaDelta) ++
    Seq(MultipleImportsDelta, SingleImportDelta, FileImportDelta) ++
    Seq(SolidityFile) ++
    Seq(ElementaryTypeDelta, DynamicArrayTypeDelta, ObjectTypeDelta, TypeDelta) ++
    genericDeltas

  val language = LanguageFromDeltas
}
