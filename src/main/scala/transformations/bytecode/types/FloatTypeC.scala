package transformations.bytecode.types

import core.bigrammar.{BiGrammar, Keyword}
import core.particles.Language
import core.particles.grammars.GrammarCatalogue
import core.particles.node.{Node, NodeClass}

object FloatTypeC extends TypeInstance
{
  object FloatTypeKey extends NodeClass
  override val key = FloatTypeKey
  val floatType = new Node(FloatTypeKey)

  override def getSuperTypes(_type: Node, state: Language): Seq[Node] = ???

  override def getJavaGrammar(grammars: GrammarCatalogue): BiGrammar = {
    import grammars._
    "float" ~> value(floatType)
  }

  override def getByteCodeGrammar(grammars: GrammarCatalogue): BiGrammar = {
    import grammars._
    new Keyword("F",false) ~> value(floatType)
  }

  override def description: String = "Adds the float type."
}
