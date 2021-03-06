package miksilo.editorParser.parsers.editorParsers

import miksilo.editorParser.parsers.core.{ParseText, OffsetPointer, TextPointer}
import miksilo.editorParser.parsers.editorParsers.Position.PositionOrdering

case class TextEdit(range: SourceRange, newText: String)
case class Fix(title: String, edit: TextEdit)

/**
  * Position in a text document expressed as zero-based line and character offset.
  */
case class Position(line: Int, character: Int) {
  def span(steps: Int): SourceRange = {
    SourceRange(this, Position(line, character + steps))
  }
}

case class OffsetPointerRange(from: OffsetPointer, until: OffsetPointer) {
  def toSourceRange: SourceRange = SourceRange(from.lineCharacter, until.lineCharacter)
  def toOffsetRange: OffsetRange = OffsetRange(from.offset, until.offset)

  def contains(offset: Int): Boolean = {
    from.offset <= offset && offset <= until.offset
  }
}

case class OffsetRange(from: Int, until: Int) {
  def contains(offset: Int): Boolean = {
    from <= offset && offset <= until
  }
  def toRange(text: ParseText): SourceRange = SourceRange(text.getPosition(from), text.getPosition(until))
}
case class FileOffsetRange(uri: String, range: OffsetRange)

/**
  * A range in a text document.
  */
case class SourceRange(start: Position, end: Position) {

  def contains(position: Position): Boolean = {
    PositionOrdering.lteq(start, position) && PositionOrdering.lteq(position, end)
  }

  def contains(position: SourceRange): Boolean = {
    PositionOrdering.lteq(start, position.start) && PositionOrdering.lteq(position.end, end)
  }
}

object Position {
  implicit object PositionOrdering extends Ordering[Position] {

    private val ordering = Ordering.by[Position, (Int, Int)](x => (x.line, x.character))
    override def compare(x: Position, y: Position): Int = {
      ordering.compare(x, y)
    }
  }
}

trait ParseError {
  def fix: Option[Fix] = None
  def message: String
  def from: TextPointer
  def to: TextPointer
  def range: SourceRange = SourceRange(from.lineCharacter, to.lineCharacter)

  def canMerge: Boolean = false
  def penalty: Double
  def score: Double = -penalty * 1
  def append(other: ParseError): Option[ParseError] = None

  override def toString = s"$message AT $from"
}
