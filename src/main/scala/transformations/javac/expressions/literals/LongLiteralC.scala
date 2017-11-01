package transformations.javac.expressions.literals

import core.bigrammar.{BiGrammar, RegexG}
import core.particles.grammars.GrammarCatalogue
import core.particles.node.{Node, NodeClass, NodeField}
import core.particles.path.Path
import core.particles.{Compilation, Contract, Language}
import transformations.bytecode.coreInstructions.integers.SmallIntegerConstantDelta
import transformations.bytecode.coreInstructions.longs.PushLongDelta
import transformations.bytecode.types.LongTypeC
import transformations.javac.expressions.{ExpressionInstance, ExpressionSkeleton}

object LongLiteralC extends ExpressionInstance {
  val key = LongLiteralKey

  override def dependencies: Set[Contract] = Set(ExpressionSkeleton, SmallIntegerConstantDelta)

  def parseLong(number: String) = java.lang.Long.parseLong(number.dropRight(1))

  override def transformGrammars(grammars: GrammarCatalogue, state: Language): Unit = {
    import grammars._
    val longGrammar : BiGrammar = RegexG("""-?\d+l""".r) ^^
      (number => parseLong(number.asInstanceOf[String]), l => Some(s"${l}l")) as ValueKey asNode LongLiteralKey
    val expressionGrammar = find(ExpressionSkeleton.ExpressionGrammar)
    expressionGrammar.addOption(longGrammar)
  }

  def literal(value: Long) = new Node(LongLiteralKey, ValueKey -> value)

  override def toByteCode(literal: Path, compilation: Compilation): Seq[Node] = {
    Seq(PushLongDelta.constant(getValue(literal).toInt))
  }

  def getValue(literal: Node) = literal(ValueKey).asInstanceOf[Long]

  override def getType(expression: Path, compilation: Compilation): Node = LongTypeC.longType

  object LongLiteralKey extends NodeClass

  object ValueKey extends NodeField

  override def description: String = "Adds the usage of long literals by putting an l after the number."
}
