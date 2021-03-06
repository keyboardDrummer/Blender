package miksilo.modularLanguages.deltas.trivia

import miksilo.modularLanguages.core.bigrammar.{BiGrammar, GrammarPath, GrammarReference, RootGrammar}
import miksilo.modularLanguages.core.bigrammar.grammars.{BiChoice, BiFailure, BiSequence, WithTrivia}
import miksilo.modularLanguages.core.deltas.grammars.LanguageGrammars
import miksilo.modularLanguages.core.deltas.{Contract, DeltaWithGrammar}
import miksilo.languageServer.core.language.Language
import miksilo.modularLanguages.core.node.NodeGrammar

//noinspection ZeroIndexToHead
object TriviaInsideNode extends DeltaWithGrammar {

  override def description: String = "Moves trivia grammars left of a node to the inside of the node"

  override def transformGrammars(_grammars: LanguageGrammars, language: Language): Unit = {
    val grammars = _grammars
    import grammars._
    var visited = Set.empty[BiGrammar]
    val descendants = root.descendants
    for(path <- descendants)
    {
      path.value match {
        case trivia: WithTrivia =>
          if (!visited.contains(path.value)) {
            visited += path.value

            val grammar = trivia.inner
            if (hasLeftNode(new RootGrammar(grammar))) {
              path.set(trivia.inner)
              injectTrivia(grammars, path, trivia.horizontal)
            }
          }
        case _ =>
      }
    }
  }

  private def hasLeftNode(path: GrammarPath) = {
    val leftChildren = path.value.getLeftChildren
    leftChildren.exists(p => p.isInstanceOf[NodeGrammar])
  }

  def injectTrivia(grammars: LanguageGrammars, grammar: GrammarReference, horizontal: Boolean): Unit = {
    if (grammar.value.isInstanceOf[WithTrivia])
      return //TODO if we consider the grammars as a graph and only move WithTrivia's from all incoming edges at once, then we wouldn't need this hack.

    grammar.value match {
      case sequence: BiSequence =>
        val left = sequence.getLeftChildren.drop(1).head
        val child = grammar.children.find(ref => ref.value == left).get
        injectTrivia(grammars, child, horizontal)
      case _:NodeGrammar =>
        if (!grammar.children.head.value.isLeftRecursive) {
          placeTrivia(grammars, grammar.children.head, horizontal)
        }
      case _: BiChoice =>
        injectTrivia(grammars, grammar.children(0), horizontal)
        injectTrivia(grammars, grammar.children(1), horizontal)
      case _: BiFailure =>
      case _ =>
        if (grammar.children.length == 1)
          injectTrivia(grammars, grammar.children.head, horizontal)
        else placeTrivia(grammars, grammar, horizontal)
    }
  }

  def placeTrivia(grammars: LanguageGrammars, grammar: GrammarReference, horizontal: Boolean): Unit = {
    if (!grammar.value.isInstanceOf[WithTrivia] && grammar.value.containsParser()) {
      grammar.set(new WithTrivia(grammar.value, grammars.trivia, horizontal))
    }
  }

  override def dependencies: Set[Contract] = Set.empty
}
