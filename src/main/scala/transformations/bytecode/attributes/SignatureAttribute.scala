package transformations.bytecode.attributes

import core.bigrammar.BiGrammar
import core.particles.grammars.GrammarCatalogue
import core.particles.node.{Key, Node}
import transformations.bytecode.readJar.ClassFileParser
import transformations.bytecode.readJar.ClassFileParser._

object SignatureAttribute extends ByteCodeAttribute {

  object SignatureKey extends Key
  object SignatureIndex extends Key
  override def key: Key = SignatureKey

  override def getGrammar(grammars: GrammarCatalogue): BiGrammar = {
    ("signature with index:" ~~> integer).asNode(SignatureKey, SignatureIndex)
  }

  override def constantPoolKey: String = "Signature"

  override def description: String = "Adds the signature attribute"

  override def getParser(unParsed: Node): ClassFileParser.Parser[Node] = for {
    index <- ParseShort
  } yield new Node(SignatureKey, SignatureIndex -> index.toInt)
}
