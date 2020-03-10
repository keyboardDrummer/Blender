package languageServer

import core.LazyLogging
import core.language.DocumentEventListener
import core.parsers.core.ParseText
import core.parsers.editorParsers.{Position, SourceRange}
import languageServer.InMemoryTextDocument._
import lsp.TextDocumentContentChangeEvent

import scala.collection.mutable.ArrayBuffer

object InMemoryTextDocument {
  val newLine = "\n"
}

class InMemoryTextDocument(uri: String) extends LazyLogging {
  val parseText = new ParseText

  def this(uri: String, contents: String) = {
    this(uri)
    parseText.arrayOfChars = contents.toCharArray
  }

  def applyUnsafeChanges(changes: Seq[TextDocumentContentChangeEvent], handlerOption: Option[DocumentEventListener] = None): Unit = {
    try {
      applyChanges(changes, handlerOption)
    } catch {
      case error: IllegalArgumentException =>
        logger.error("Failed to apply changes because: " + error.getMessage)
    }
  }

  def applyChanges(changes: Seq[TextDocumentContentChangeEvent], handlerOption: Option[DocumentEventListener] = None): Unit = {
    for(change <- changes) {
      change.range match {
        case None =>
          val previousLength = parseText.arrayOfChars.length
          parseText.arrayOfChars = changes.head.text.toCharArray
          handlerOption.foreach(handler =>
            handler.handleChange(0, previousLength, changes.head.text))
        case Some(range) =>
          parseText.applyRangeChange(change.text, range)
          handlerOption.foreach(handler =>
            handler.handleChange(parseText.getOffset(range.start), parseText.getOffset(range.end), change.text))
      }
    }
  }

  def mkString = new String(parseText.arrayOfChars)
}
