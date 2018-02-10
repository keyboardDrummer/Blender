package deltas.javac.expressions.literals

import core.deltas._
import core.deltas.grammars.LanguageGrammars
import core.deltas.node.{Node, NodeShape}
import core.deltas.path.NodePath
import core.language.Language
import deltas.bytecode.coreInstructions.objects.PushNullDelta
import deltas.javac.expressions.{ExpressionInstance, ExpressionSkeleton}

object NullC extends ExpressionInstance {

  val _null = new Node(NullKey)

  override def transformGrammars(grammars: LanguageGrammars, state: Language): Unit = {
    import grammars._
    val expressionGrammar = find(ExpressionSkeleton.ExpressionGrammar)
    val parseNull = "null" ~> value(_null)
    expressionGrammar.inner = expressionGrammar.inner | parseNull
  }

  override def dependencies: Set[Contract] = Set(ExpressionSkeleton, PushNullDelta)

  object NullKey extends NodeShape

  override val key = NullKey

  override def getType(expression: NodePath, compilation: Compilation): Node = ???

  override def toByteCode(expression: NodePath, compilation: Compilation): Seq[Node] = {
    Seq(PushNullDelta.pushNull)
  }

  override def description: String = "Adds the usage of 'null'"
}
