package core.deltas

import core.parsers.editorParsers.{SourceRange, StopImmediately}
import deltas.json.JsonLanguage
import languageServer.{LanguageServerTest, MiksiloLanguageServer}
import lsp._
import org.scalatest.funsuite.AnyFunSuite

class IncrementalParsingJsonServerTest extends AnyFunSuite with LanguageServerTest {

  val server = new MiksiloLanguageServer(LanguageFromDeltas(Seq(ParseUsingTextualGrammar(StopImmediately)) ++ JsonLanguage.deltas))

  test("zero length caching") {

    val program = """{"a":{ }}"""

    val insert = """"b":3""".stripMargin

    val document = new TextDocumentItem("item","language", 0L, program)
    val documentId = VersionedTextDocumentIdentifier(document.uri, document.version)
    server.didOpen(document)
    val start = HumanPosition(1, 7)
    var diagnostics: PublishDiagnostics = null
    val client = new LanguageClient {
      override def sendDiagnostics(newDiagnostics: PublishDiagnostics): Unit = diagnostics = newDiagnostics

      override def trackMetric(name: String, value: Double): Unit = {}
    }
    server.setClient(client)

    server.didChange(DidChangeTextDocumentParams(documentId,
      Seq(TextDocumentContentChangeEvent(Some(SourceRange(start, start)), None, insert))))

    assert(diagnostics.diagnostics.isEmpty)
  }
}