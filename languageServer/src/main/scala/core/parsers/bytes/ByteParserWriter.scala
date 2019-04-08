package core.parsers.bytes

import java.nio.ByteBuffer

import core.language.node.Node
import core.parsers.basicParsers.NoErrorReportingParserWriter
import core.parsers.core.ParseInput
import deltas.bytecode.constants.Utf8ConstantDelta

case class ByteReader(array: Array[Byte], offset: Int) extends ParseInput {
  def this(array: Array[Byte]) {
    this(array, 0)
  }

  def drop(amount: Int): ByteReader = ByteReader(array, offset + amount)

  def head: Byte = array(offset)

  def tail: ByteReader = drop(1)

  override def atEnd: Boolean = offset == array.length
}

trait ByteParserWriter extends NoErrorReportingParserWriter {
  type Input = ByteReader
  type Elem = Byte

  val ParseInteger = XBytes(4).map(bytes => bytes.getInt)
  val ParseFloat = XBytes(4).map(bytes => bytes.getFloat)
  val ParseDouble = XBytes(8).map(bytes => bytes.getDouble())
  val ParseLong = XBytes(8).map(bytes => bytes.getLong())
  val ParseByte = XBytes(1).map(bytes => bytes.get())
  val ParseShort = XBytes(2).map(bytes => bytes.getShort())
  val ParseUtf8: Parser[Node] = ParseShort.flatMap(length => parseString(length)).map(s => Utf8ConstantDelta.create(s))


  case class XBytes(amount: Int) extends ParserBase[ByteBuffer] with LeafParser[ByteBuffer] {

    override def apply(input: ByteReader) = {
      newSuccess(ByteBuffer.wrap(input.array, input.offset, amount), input.drop(amount))
    }

    override def getMustConsume(cache: ConsumeCache) = amount > 0
  }

  def parseString(length: Int) =
    XBytes(length).map(bytes => {
      new String(bytes.array(), bytes.position(), length, "UTF-8")
    })

  def elems(bytes: Seq[Byte]): Self[Unit] = {
    XBytes(bytes.length).flatMap[Unit](parsedBytes => {
      val destination = new Array[Byte](bytes.length)
      parsedBytes.get(destination)
      val result = if (destination sameElements bytes) {
        succeed(())
      }
      else {
        fail(s"parsed bytes '$parsedBytes' were not equal to expected bytes $bytes")
      }
      result
    })
  }

  // TODO because of the flatMap usage, compile doesn't work, and we don't need it anyways because we don't have left recursion.
  override def compile[Result](root: Parser[Result]) = {
    newParseState(root)
  }
}
