package deltas.javac.expressions.prefix

import core.deltas.grammars.LanguageGrammars
import core.language.node.{Node, NodeField, NodeShape}
import core.deltas.path.NodePath
import core.language.{Compilation, Language}
import core.smarts.ConstraintBuilder
import core.smarts.scopes.objects.Scope
import core.smarts.types.objects.Type
import deltas.bytecode.extraBooleanInstructions.NotInstructionDelta
import deltas.expressions.ExpressionDelta
import deltas.javac.expressions.{ByteCodeExpressionSkeleton, ConvertsToByteCode, ExpressionInstance, ToByteCodeSkeleton}
import deltas.javac.types.BooleanTypeDelta

object NotDelta extends ExpressionInstance with ConvertsToByteCode {

  object NotKey extends NodeShape

  object NotExpression extends NodeField

  override val shape = NotKey

  override def getType(expression: NodePath, compilation: Compilation): Node = BooleanTypeDelta.booleanType

  override def toByteCode(expression: NodePath, compilation: Compilation): Seq[Node] = {
    ToByteCodeSkeleton.getToInstructions(compilation)(expression) ++ Seq(NotInstructionDelta.not)
  }

  override def transformGrammars(grammars: LanguageGrammars, state: Language): Unit = {
    import grammars._
    val coreGrammar = find(ExpressionDelta.LastPrecedenceGrammar)
    coreGrammar.addAlternative("!" ~> coreGrammar.as(NotExpression) asNode NotKey)
  }

  override def description: String = "Adds the ! (not) operator."

  override def constraints(compilation: Compilation, builder: ConstraintBuilder, expression: NodePath, _type: Type, parentScope: Scope): Unit = {
    val targetType = ByteCodeExpressionSkeleton.getType(compilation, builder, expression(NotExpression).asInstanceOf[NodePath], parentScope)
    builder.typesAreEqual(targetType, BooleanTypeDelta.constraintType)
    builder.typesAreEqual(_type, BooleanTypeDelta.constraintType)
  }
}
