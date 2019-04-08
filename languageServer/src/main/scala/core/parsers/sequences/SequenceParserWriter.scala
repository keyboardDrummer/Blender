package core.parsers.sequences

import core.parsers.editorParsers.{DefaultCache, EditorParserWriter}

trait SequenceParserWriter extends EditorParserWriter {
  type Elem
  type Input <: SequenceInput[Input, Elem]

  def elem(predicate: Elem => Boolean, kind: String) = ElemPredicate(predicate, kind)
  case class ElemPredicate(predicate: Elem => Boolean, kind: String)
    extends EditorParserBase[Elem] with LeafParser[Elem] {

    override def apply(input: Input): ParseResult[Elem] = {
      if (input.atEnd) {
        return newFailure(input, s"$kind expected but end of source found")
      }

      val char = input.head
      if (predicate(char)) {
        newSuccess(char, input.tail)
      }
      else
        newFailure(input, s"'$char' was not a $kind")
    }

    override def getDefault(cache: DefaultCache): Option[Elem] = None

    override def getMustConsume(cache: ConsumeCache) = true
  }

}
