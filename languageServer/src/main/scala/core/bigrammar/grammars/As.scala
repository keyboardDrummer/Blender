package core.bigrammar.grammars

import core.bigrammar.PrintBiGrammar.withParenthesis
import core.bigrammar.printer.AsPrinter
import core.bigrammar.printer.Printer.NodePrinter
import core.bigrammar.{BiGrammar, BiGrammarToParser, WithMap}
import core.language.node.{NodeField, SourceRange}
import core.responsiveDocument.ResponsiveDocument
import langserver.types.Position
import languageServer.HumanPosition
import BiGrammarToParser._

case class As(var inner: BiGrammar, field: NodeField) extends CustomGrammar
{
  override def children: Seq[BiGrammar] = Seq(inner)

  override def withChildren(newChildren: Seq[BiGrammar]) = As(newChildren.head, field)

  override def containsParser(recursive: BiGrammar => Boolean): Boolean = recursive(inner)

  override def print(toDocumentInner: BiGrammar => ResponsiveDocument): ResponsiveDocument = withParenthesis(inner) ~ s".As($field)"

  override def createPrinter(recursive: BiGrammar => NodePrinter): NodePrinter = new AsPrinter(recursive(inner), field)

  override def toParser(recursive: BiGrammar => EditorParser[Result]): EditorParser[Result] = {
    val innerParser: EditorParser[Result] = recursive(inner).
      map(withMap => WithMap(Unit, withMap.namedValues + (field -> withMap.value)))
        innerParser.withRange[Result]((left, right, result: Result) => {
      val start: Position = new HumanPosition(left.position.line, left.position.column)
      val end: Position = new HumanPosition(right.position.line, right.position.column)
      WithMap[Any](result.value, result.namedValues + (FieldPosition(field) -> SourceRange(start, end)))
    })
  }
}
