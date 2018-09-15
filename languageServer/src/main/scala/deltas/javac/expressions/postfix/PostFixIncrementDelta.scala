package deltas.javac.expressions.postfix

import core.deltas.Contract
import core.deltas.grammars.LanguageGrammars
import core.deltas.path.NodePath
import core.language.node.{Node, NodeField, NodeShape}
import core.language.{Compilation, Language}
import core.smarts.ConstraintBuilder
import core.smarts.scopes.objects.Scope
import core.smarts.types.objects.Type
import deltas.bytecode.coreInstructions.integers.{IncrementIntegerDelta, LoadIntegerDelta}
import deltas.bytecode.types.IntTypeDelta
import deltas.expressions.ExpressionDelta
import deltas.javac.expressions.{ConvertsToByteCode, ExpressionInstance}
import deltas.javac.methods.MethodDelta

object PostFixIncrementDelta extends ExpressionInstance with ConvertsToByteCode {

  override val shape = PostfixIncrementKey

  override def dependencies: Set[Contract] = Set(ExpressionDelta, MethodDelta, IncrementIntegerDelta)

  override def getType(expression: NodePath, compilation: Compilation): Node = IntTypeDelta.intType

  override def toByteCode(plusPlus: NodePath, compilation: Compilation): Seq[Node] = {
    val methodCompiler = MethodDelta.getMethodCompiler(compilation)
    val name: String = plusPlus(Target).asInstanceOf[String]
    val variableAddress = methodCompiler.getVariables(plusPlus)(name).offset
    Seq(LoadIntegerDelta.load(variableAddress), IncrementIntegerDelta.integerIncrement(variableAddress, 1))
  }

  override def transformGrammars(grammars: LanguageGrammars, state: Language): Unit = {
    import grammars._
    val coreGrammar = find(ExpressionDelta.LastPrecedenceGrammar)
    val postFixIncrement = identifier.as(Target) ~< "++" asNode PostfixIncrementKey
    coreGrammar.addAlternative(postFixIncrement)
  }

  object PostfixIncrementKey extends NodeShape

  object Target extends NodeField

  override def description: String = "Adds the postfix ++ operator."

  override def constraints(compilation: Compilation, builder: ConstraintBuilder, plusPlus: NodePath, _type: Type, parentScope: Scope): Unit = {
    builder.typesAreEqual(IntTypeDelta.constraintType, _type)
    val name = plusPlus(Target).asInstanceOf[String]
    builder.resolve(name, plusPlus.getLocation(Target), parentScope, Some(_type))
  }
}
