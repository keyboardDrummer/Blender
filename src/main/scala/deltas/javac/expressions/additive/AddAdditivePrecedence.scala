package deltas.javac.expressions.additive

import core.deltas.grammars.LanguageGrammars
import core.deltas.node.GrammarKey
import core.deltas.{Contract, DeltaWithGrammar, Language}
import deltas.javac.expressions.ExpressionSkeleton

object AddAdditivePrecedence extends DeltaWithGrammar {

  override def dependencies: Set[Contract] = Set(ExpressionSkeleton)

  override def transformGrammars(grammars: LanguageGrammars, state: Language): Unit = {
    val expressionGrammar = grammars.find(ExpressionSkeleton.ExpressionGrammar)
    val additiveGrammar = grammars.create(Grammar, expressionGrammar.inner)
    expressionGrammar.inner = additiveGrammar
  }

  object Grammar extends GrammarKey

  override def description: String = "Creates a named grammar with the correct precedence for addition-like operators."
}