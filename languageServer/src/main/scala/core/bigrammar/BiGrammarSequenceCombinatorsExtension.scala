package core.bigrammar

import core.bigrammar.grammars._
import core.document.BlankLine
import core.language.node.Node

trait BiGrammarSequenceCombinatorsExtension extends BiGrammarWriter {

  def grammar: BiGrammar
  def topBottom(other: BiGrammar, combine: (Any, Any) => Any, split: Any => (Any, Any)): TopBottom
  def leftRight(other: BiGrammar, combine: (Any, Any) => Any, split: Any => (Any, Any)): LeftRight

  def ~(other: BiGrammar): LeftRight = leftRight(other, Sequence.packTuple, Sequence.unpackTuple)
  def %(other: BiGrammar): TopBottom = topBottom(other, Sequence.packTuple, Sequence.unpackTuple)

  def ~<(right: BiGrammar): BiGrammar = leftRight(right, Sequence.ignoreRight, Sequence.produceRight)

  def ~>(right: BiGrammar): BiGrammar = leftRight(right, Sequence.ignoreLeft, Sequence.produceLeft)

  def %>(bottom: BiGrammar): BiGrammar = topBottom(bottom, Sequence.ignoreLeft, Sequence.produceLeft)

  def %<(bottom: BiGrammar): BiGrammar = topBottom(bottom, Sequence.ignoreRight, Sequence.produceRight)

  def many: ManyHorizontal
  def manyVertical: ManyVertical

  implicit def addSequenceMethods(grammar: BiGrammar): BiGrammarSequenceCombinatorsExtension

  def ~~<(right: BiGrammar): BiGrammar = this ~< new LeftRight(printSpace, right, Sequence.ignoreLeft, Sequence.produceLeft)

  def manySeparated(separator: BiGrammar): BiGrammar = someSeparated(separator) | ValueGrammar(Seq.empty[Any])

  def ~~(right: BiGrammar): BiGrammar = {
    new LeftRight(grammar, printSpace, Sequence.ignoreRight, Sequence.produceRight) ~ right
  }

  def someSeparatedVertical(separator: BiGrammar): BiGrammar =
    someMap(this % (separator %> grammar).manyVertical)

  def manySeparatedVertical(separator: BiGrammar): BiGrammar = someSeparatedVertical(separator) | ValueGrammar(Seq.empty[Node])

  def some: BiGrammar = someMap(grammar ~ (grammar*))
  def someSeparated(separator: BiGrammar): BiGrammar = someMap(this ~ ((separator ~> grammar) *))

  private def someMap(grammar: BiGrammar): BiGrammar = {
    grammar.mapSome[(Any, Seq[Any]), Seq[Any]](
      t => Seq(t._1) ++ t._2,
      seq => if (seq.nonEmpty) Some((seq.head, seq.tail)) else None)
  }

  def inParenthesis: BiGrammar = ("(": BiGrammar) ~> grammar ~< ")"

  def ~~>(right: BiGrammar): BiGrammar = new LeftRight(grammar, printSpace, Sequence.ignoreRight, Sequence.produceLeft) ~> right

  def * : ManyHorizontal = many

  def %%(bottom: BiGrammar): BiGrammar = {
    (this %< BlankLine) % bottom
  }
}

