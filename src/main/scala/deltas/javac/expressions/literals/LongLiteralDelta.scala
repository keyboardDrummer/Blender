package deltas.javac.expressions.literals

import core.bigrammar.BiGrammar
import core.bigrammar.grammars.RegexGrammar
import core.deltas.grammars.LanguageGrammars
import core.deltas.node.{Node, NodeShape, NodeField}
import core.deltas.path.Path
import core.deltas.{Compilation, Contract, Language}
import deltas.bytecode.coreInstructions.integers.SmallIntegerConstantDelta
import deltas.bytecode.coreInstructions.longs.PushLongDelta
import deltas.bytecode.types.LongTypeC
import deltas.javac.expressions.{ExpressionInstance, ExpressionSkeleton}

object LongLiteralDelta extends ExpressionInstance {
  val key = LongLiteralKey

  override def dependencies: Set[Contract] = Set(ExpressionSkeleton, SmallIntegerConstantDelta)

  private def parseLong(number: String) = java.lang.Long.parseLong(number.dropRight(1))

  override def transformGrammars(grammars: LanguageGrammars, state: Language): Unit = {
    import grammars._
    val longGrammar : BiGrammar = RegexGrammar("""-?\d+l""".r).map[String, Long](
      number => parseLong(number), l => s"${l}l") as ValueKey asNode LongLiteralKey
    val expressionGrammar = find(ExpressionSkeleton.ExpressionGrammar)
    expressionGrammar.addOption(longGrammar)
  }

  def literal(value: Long) = new Node(LongLiteralKey, ValueKey -> value)

  override def toByteCode(literal: Path, compilation: Compilation): Seq[Node] = {
    Seq(PushLongDelta.constant(getValue(literal).toInt))
  }

  def getValue(literal: Node): Long = literal(ValueKey).asInstanceOf[Long]

  override def getType(expression: Path, compilation: Compilation): Node = LongTypeC.longType

  object LongLiteralKey extends NodeShape

  object ValueKey extends NodeField

  override def description: String = "Adds the usage of long literals by putting an l after the number."
}