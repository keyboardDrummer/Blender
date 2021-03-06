package miksilo.modularLanguages.core.bigrammar.grammars

import miksilo.modularLanguages.core.bigrammar.BiGrammarToParser._

case class Keyword(var value: String, reserved: Boolean = true, verifyWhenPrinting: Boolean = false)
  extends StringGrammar(verifyWhenPrinting) {
  if (value.length == 0)
    throw new RuntimeException("value must have non-zero length")

  override def getParserBuilder(keywords: scala.collection.Set[String]): Parser[String] = {
    if (reserved)
      literalOrKeyword(value)
    else
      literal(value)
  }
}
