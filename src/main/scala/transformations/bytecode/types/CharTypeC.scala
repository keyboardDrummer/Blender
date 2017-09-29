package transformations.bytecode.types

import core.bigrammar.{Keyword, BiGrammar}
import core.particles.Language
import core.particles.grammars.GrammarCatalogue
import core.particles.node.{Key, Node}

object CharTypeC extends TypeInstance
{
  object CharTypeKey extends Key
  override val key: Key = CharTypeKey
  val me = new Node(CharTypeKey)

  override def getSuperTypes(_type: Node, state: Language): Seq[Node] = ???

  override def getJavaGrammar(grammars: GrammarCatalogue): BiGrammar = "char" ~> produce(me)

  override def getByteCodeGrammar(grammars: GrammarCatalogue): BiGrammar = new Keyword("C",false) ~> produce(me)

  override def description: String = "Adds the char type."
}
