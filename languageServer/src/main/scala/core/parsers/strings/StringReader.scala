package core.parsers.strings

import core.parsers.sequences.SequenceInput

import scala.util.parsing.input.OffsetPosition

case class StringReader(array: Array[Char], offset: Int = 0) extends SequenceInput[StringReader, Char] {
  def this(value: String) {
    this(value.toCharArray)
  }

  def drop(amount: Int): StringReader = StringReader(array, offset + amount)
  def position = OffsetPosition(array, offset)

  override def atEnd: Boolean = offset == array.length

  override def head: Char = array(offset)

  override def tail: StringReader = drop(1)

  override def hashCode(): Int = offset

  override def equals(obj: Any): Boolean = obj match {
    case other: StringReader => offset == other.offset
    case _ => false
  }
}
