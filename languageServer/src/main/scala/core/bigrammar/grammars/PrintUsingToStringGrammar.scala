package core.bigrammar.grammars

import core.bigrammar.BiGrammarToParser.Result
import core.bigrammar.WithMap
import core.bigrammar.printer.TryState
import core.responsiveDocument.ResponsiveDocument

abstract class PrintUsingToStringGrammar(verifyWhenPrinting: Boolean = true)
  extends StringGrammar(verifyWhenPrinting) {

  override def write(from: Result): TryState[ResponsiveDocument] =
    super.write(WithMap(from.value.toString, from.namedValues))
}
