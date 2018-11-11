package core.parsers

import langserver.types.Position
import languageServer.HumanPosition

import scala.util.matching.Regex
import scala.util.parsing.input.{OffsetPosition, Positional}

trait StringParsers extends Parsers {
  type Input = StringReader

  def position[T <: Positional]: Parser[Position] = new Parser[Position] {
    override def parse(input: Input, state: ParseState): ParseResult[Position] = {
      ParseSuccess(new HumanPosition(input.position.line, input.position.column), input, NoFailure)
    }

    override def getDefault(cache: DefaultCache): Option[Position] = None
  }

  def literal(value: String): Literal = Literal(value)
  def regex(value: Regex): RegexFrom = RegexFrom(value)

  implicit class Literal(value: String) extends Parser[String] {
    override def parse(inputs: StringReader, cache: ParseState): ParseResult[String] = {
      var index = 0
      val array = inputs.array
      while(index < value.length) {
        val arrayIndex = index + inputs.offset
        if (array.length <= arrayIndex) {
          return ParseFailure(Some(value), inputs, s"expected '$value' but end of source found")
        } else if (array.charAt(arrayIndex) != value.charAt(index)) {
          return ParseFailure(Some(value), inputs.drop(index), s"expected '$value' but found ${array.subSequence(inputs.offset, arrayIndex)}")
        }
        index += 1
      }
      ParseSuccess(value, inputs.drop(value.length), NoFailure)
    }

    override def getDefault(cache: DefaultCache): Option[String] = Some(value)
  }

  implicit class RegexFrom(regex: Regex) extends Parser[String] {
    override def parse(inputs: StringReader, cache: ParseState): ParseResult[String] = {
      regex.findPrefixMatchOf(new SubSequence(inputs.array, inputs.offset)) match {
        case Some(matched) =>
          ParseSuccess(
            inputs.array.subSequence(inputs.offset, inputs.offset + matched.end).toString,
            inputs.drop(matched.end), NoFailure)        case None =>
          val nextCharacter =
            if (inputs.array.length == inputs.offset) "end of source"
            else inputs.array.charAt(inputs.offset)
          ParseFailure(None, inputs, s"expected '$regex' but found '$nextCharacter'") // Partial regex matching toevoegen
      }
    }

    override def getDefault(cache: DefaultCache): Option[String] = None
  }
}

case class StringReader(array: Array[Char], offset: Int = 0) extends InputLike {
  def this(value: String) {
    this(value.toCharArray)
  }

  def drop(amount: Int): StringReader = StringReader(array, offset + amount)
  def position = OffsetPosition(array, offset)

  override def finished: Boolean = offset == array.length
}

class SubSequence(original: CharSequence, start: Int, val length: Int) extends CharSequence {
  def this(s: CharSequence, start: Int) = this(s, start, s.length - start)

  def charAt(index: Int): Char =
    if (index >= 0 && index < length) original.charAt(start + index) else throw new IndexOutOfBoundsException(s"index: $index, length: $length")

  def subSequence(_start: Int, _end: Int): SubSequence = {
    if (_start < 0 || _end < 0 || _end > length || _start > _end)
      throw new IndexOutOfBoundsException(s"start: ${_start}, end: ${_end}, length: $length")

    new SubSequence(original, start + _start, _end - _start)
  }

  override def toString: String = original.subSequence(start, start + length).toString
}
