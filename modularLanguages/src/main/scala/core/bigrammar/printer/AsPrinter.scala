package core.bigrammar.printer

import core.bigrammar.WithMap
import core.bigrammar.printer.Printer.NodePrinter
import core.language.node.NodeField
import core.responsiveDocument.ResponsiveDocument

class AsPrinter(inner: NodePrinter, key: NodeField) extends NodePrinter {
  override def write(from: WithMap[Any]): TryState[ResponsiveDocument] = {
    from.namedValues.get(key).fold[TryState[ResponsiveDocument]](
      Printer.fail(s"did not find as key $key in state ${from.namedValues}"))(
      value => inner.write(WithMap(value, from.namedValues)))
  }
}