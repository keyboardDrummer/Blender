package core.deltas.grammars

import core.bigrammar._
import core.bigrammar.grammars._
import core.deltas.{GrammarForAst, NodeGrammarWriter}
import core.language.node.{GrammarKey, Key, NodeShape}

case class KeyGrammar(key: Key) extends GrammarKey
{
  override lazy val toString = key.toString
}

object TriviasGrammar extends GrammarKey
object TriviaGrammar extends GrammarKey
object BodyGrammar extends GrammarKey
object ProgramGrammar extends GrammarKey
class LanguageGrammars extends GrammarCatalogue {

  val trivia: Labelled = create(TriviasGrammar, new ManyVertical(create(TriviaGrammar, ParseWhiteSpace)))
  val bodyGrammar = create(BodyGrammar, BiFailure())
  create(ProgramGrammar, WithTrivia.withTrivia(new BiSequence(bodyGrammar, trivia, Sequence.ignoreRight, true), trivia)) //TODO Move this, bodyGrammar and trivia to a separate Delta.

  def root: Labelled = find(ProgramGrammar)

  implicit def stringToAstGrammar(value: String): BiGrammarExtension =
    new BiGrammarExtension(BiGrammarWriter.stringToGrammar(value), this)
  implicit def grammarToAstGrammar(value: BiGrammar): BiGrammarExtension = new BiGrammarExtension(value, this)

  class BiGrammarExtension(val grammar: BiGrammar, grammars: LanguageGrammars) extends NodeGrammarWriter
    with BiGrammarSequenceCombinatorsExtension
  {
    private def addTriviaIfUseful(grammar: BiGrammar, horizontal: Boolean) =
      if (grammar.containsParser()) WithTrivia.withTrivia(grammar, grammars.trivia, horizontal) else grammar

    def asLabelledNode(key: NodeShape): Labelled = grammars.create(key, new GrammarForAst(grammar).asNode(key))

    def manyVertical = new ManyVertical(addTriviaIfUseful(grammar, false))

    override def sequence(other: BiGrammar, bijective: SequenceBijective, horizontal: Boolean): BiGrammar =
      new BiSequence(grammar, addTriviaIfUseful(other, horizontal), bijective, horizontal)

    def many = new ManyHorizontal(addTriviaIfUseful(grammar, true))

    override implicit def addSequenceMethods(grammar: BiGrammar): BiGrammarExtension = new BiGrammarExtension(grammar, grammars)
  }
}


