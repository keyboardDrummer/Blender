package core.bigrammar.grammars

import core.bigrammar.BiGrammarToParser.Result
import core.bigrammar.printer.Printer.NodePrinter
import core.bigrammar.{BiGrammar, BiGrammarToParser}
import core.responsiveDocument.ResponsiveDocument
import BiGrammarToParser._

class WithDefault(inner: BiGrammar, _default: Any, name: String) extends CustomGrammar {
  override def print(toDocumentInner: BiGrammar => ResponsiveDocument): ResponsiveDocument = toDocumentInner(inner) ~ "withDefault: " ~ _default.toString

  override def createPrinter(recursive: BiGrammar => NodePrinter): NodePrinter = recursive(inner)

  override def toParser(recursive: BiGrammar => Self[Result]): Self[Result] =
    recursive(inner).withDefault[Result](valueToResult(_default), name)

  override def children: Seq[BiGrammar] = Seq(inner)

  override def withChildren(newChildren: Seq[BiGrammar]): BiGrammar = new core.bigrammar.grammars.WithDefault(newChildren.head, _default, name)

  override def containsParser(recursive: BiGrammar => Boolean): Boolean = recursive(inner)
}
