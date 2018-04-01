package core.gridParser

import scala.util.parsing.combinator.{Parsers, RegexParsers}
import scala.util.parsing.input.CharSequenceReader



case class Location(row: Int, column: Int) {
  def +(size: Size): Location = Location(row + size.height, column + size.width)
}
object Zero extends Size(0,0)

trait ParseResult[R] {
  def flatMap[R2](f: ParseSuccess[R] => ParseResult[R2]): ParseResult[R2] = {
    ???
  }
}

case class ParseSuccess[R](size: Size, result: R) extends ParseResult[R]
class ParseFailure[R] extends ParseResult[R]

case class Succeed[T, R](value: R) extends GridParser[T, R] {
  override def parse(grid: Grid[T]): ParseResult[R] = ParseSuccess(Zero, value)
}
class Fail[T, R] extends GridParser[T, R] {
  override def parse(grid: Grid[T]): ParseResult[R] = {
    new ParseFailure()
  }
}

trait GridParser[T, R] {

  object MyParsers extends Parsers {
    type Elem = T
  }

  def parse(grid: Grid[T]): ParseResult[R]
}

case class Size(width: Int, height: Int)

object CharParsers extends RegexParsers {
}

case class Row[R](parser: CharParsers.Parser[R]) extends GridParser[Char, R] {

  override def parse(grid: Grid[Char]): ParseResult[R] = {
    val reader = new CharSequenceReader(new CharSequence {
      override def length(): Int = grid.getRowWidth(0)

      override def subSequence(start: Int, end: Int): CharSequence = ???

      override def charAt(index: Int): Char =
        grid.get(0, index).getOrElse(throw new IndexOutOfBoundsException())
    })
    val result: CharParsers.ParseResult[R] = parser(reader)
    result match {
      case success: CharParsers.Success[R] =>
        val position = success.next.pos
        val column = position.column
        ParseSuccess(Size(0, column), success.result)
      case f => new ParseFailure
    }
  }
}

case class Wrapped[R](parser: CharParsers.Parser[R]) extends GridParser[Char, R] {

  private val parserWithWhitespace = parser <~ """\s+""".r
  override def parse(grid: Grid[Char]): ParseResult[R] = {

    val reader = new CharSequenceReader(new CharSequence {
      override def length(): Int = {
        0.until(grid.height).map(row => grid.getRowWidth(row)).sum
      }

      override def subSequence(start: Int, end: Int): CharSequence = ???

      override def charAt(index: Int): Char =
        grid.get(index)
    })

    val result: CharParsers.ParseResult[R] = parserWithWhitespace(reader)
    result match {
      case success: CharParsers.Success[R] =>
        val position = success.next.pos
        val column = position.column
        ParseSuccess(Size(position.line, position.column), success.result)
      case f => new ParseFailure
    }
  }
}

case class ParseWhitespaceOrEmpty[T](size: Size) extends GridParser[T, Unit] {

  override def parse(grid: Grid[T]): ParseResult[Unit] = {
    for(row <- 0.until(size.height)) {
      def canParseRow: Boolean = 0.until(size.width).forall(column => grid.isEmpty(row, column))
      if (!canParseRow)
        new ParseFailure[Unit]()
    }
    ParseSuccess(size, Unit)
  }
}

case class Indent[T](minimumWidth: Int, canBeWider: Boolean) extends GridParser[T, Unit] {

  override def parse(grid: Grid[T]): ParseResult[Unit] = {
    val whitespace = Some(grid.whitespace)
    var row = 0

    def canParseRow: Boolean = 0.until(minimumWidth).forall(column => grid.get(row, column) == whitespace)
    while(canParseRow) {
      row += 1
    }

    var column = minimumWidth
    if (canBeWider) {
      def canParseColumn: Boolean = 0.until(row).forall(columnRow => grid.get(columnRow, column) == whitespace)
      while(canParseColumn) {
        column += 1
      }
    }

    ParseSuccess(Size(row, column), Unit)
  }
}

case class LeftRight[T, R, R2](left: GridParser[T, R], right: GridParser[T, R2]) extends GridParser[T, (R, R2)] {
  override def parse(leftGrid: Grid[T]): ParseResult[(R, R2)] = {
    for {
      leftSuccess: ParseSuccess[R] <- left.parse(leftGrid)
      rightGrid = leftGrid.zoomColumn(leftSuccess.size.width)
      rightSuccess: ParseSuccess[R2] <- right.parse(rightGrid)
      difference = leftSuccess.size.height - rightSuccess.size.height
    } yield if (difference != 0) new ParseFailure() else ParseSuccess(
      rightSuccess.size,
      (leftSuccess.result, rightSuccess.result))
  }
}

case class TopBottom[T, R, R2](top: GridParser[T, R], bottom: GridParser[T, R2]) extends GridParser[T, (R, R2)] {
  override def parse(topGrid: Grid[T]): ParseResult[R] = {
    for {
      topSuccess: ParseSuccess[R] <- top.parse(topGrid)
      bottomGrid = topGrid.zoomRow(topSuccess.size.height)
      bottomSuccess: ParseSuccess[R2] <- bottom.parse(bottomGrid)
      difference = topSuccess.size.width - bottomSuccess.size.width
      remainderGrid = difference.compareTo(0) match {
        case 1 => bottomGrid.zoomColumn(bottomSuccess.size.width).clipWidth(difference)
        case 0 => new EmptyGrid[T]
        case -1 => topGrid.zoomColumn(topSuccess.size.width).clipWidth(-1 * difference)
      }
      ParseWhitespaceOrEmpty(remainderGrid.size).parse(remainderGrid)
    } yield ParseSuccess(
        if (difference > 0) topSuccess.size else bottomSuccess.size,
        (topSuccess.result, bottomSuccess.result))
  }
}
