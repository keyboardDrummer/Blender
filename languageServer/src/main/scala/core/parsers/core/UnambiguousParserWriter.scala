package core.parsers.core

import scala.annotation.tailrec
import scala.collection.mutable
import scala.language.higherKinds

trait UnambiguousParserWriter extends ParserWriter {
  type ParseResult[+Result] <: UnambiguousParseResult[Result]

  trait UnambiguousParseResult[+Result] extends ParseResultLike[Result] {
    def getSuccessRemainder: Option[Input]
    override def successful: Boolean = getSuccessRemainder.nonEmpty
    def get: Result
  }

  class CheckCache[Result](parseState: PackratParseState, parser: Parser[Result])
    extends ParserState[Result](parseState, parser)
    with Parse[Result] {

    def apply(input: Input, state: ParseState): ParseResult[Result] = {
      cache.get (input) match {
        case None =>
          state.callStack.push(parser)
          val value: ParseResult[Result] = parser.parseInternal (input, state)
          state.callStack.pop()
          if (!isPartOfACycle) {
            cache.put (input, value)
          }
          value
        case Some (result) => result
      }
    }
  }

  class ParserState[Result](val parseState: PackratParseState, val parser: Parser[Result]) {
    val recursionIntermediates = mutable.HashMap[Input, ParseResult[Result]]()
    val callStackSet = mutable.HashSet[Input]() // TODO might not be needed if we put an abort in the intermediates.
    var isPartOfACycle: Boolean = false
    var hasBackEdge: Boolean = false
    val cache = mutable.HashMap[Input, ParseResult[Result]]()

    def getPreviousResult(input: Input): Option[ParseResult[Result]] = {
      if (!callStackSet.contains(input))
        return None

      Some(recursionIntermediates.getOrElse(input, {
        hasBackEdge = true
        val index = parseState.callStack.indexOf(parser)
        parseState.callStack.take(index + 1).
          foreach(parser => parseState.parserStates(parser).isPartOfACycle = true) // TODO this would also be possible by returning a value that indicates we found a cycle, like the abort!
        abort
      }))
    }

    @tailrec
    private def growResult(input: Input, previous: ParseResult[Result]): ParseResult[Result] = {
      recursionIntermediates.put(input, previous)

      val nextResult: ParseResult[Result] = parser.parseInternal(input, parseState)
      nextResult.getSuccessRemainder match {
        case Some(remainder) if remainder.offset > previous.getSuccessRemainder.get.offset =>
          growResult(input, nextResult)
        case _ =>
          recursionIntermediates.remove(input)
          previous
      }
    }

    def checkFixpoint(input: Input, state: ParseState): ParseResult[Result] = {
      getPreviousResult(input) match {
        case None =>

          callStackSet.add(input)
          state.callStack.push(parser)
          var result = parser.parseInternal(input, state)
          if (result.successful && hasBackEdge) {
            result = growResult(input, result)
          }
          callStackSet.remove(input)
          state.callStack.pop()
          result

        case Some(result) => result
      }
    }

    def cacheAndFixpoint(input: Input, state: ParseState): ParseResult[Result] = {
      cache.get(input) match {
        case None =>

          val value = getPreviousResult(input) match {
            case None =>

              callStackSet.add(input)
              parseState.callStack.push(parser)
              var result = parser.parseInternal(input, parseState)
              if (result.successful && hasBackEdge) {
                result = growResult(input, result)
              }
              callStackSet.remove(input)
              parseState.callStack.pop()
              result

            case Some(result) => result
          }

          if (!isPartOfACycle) {
            cache.put(input, value)
          }
          value
        case Some(result) => result
      }
    }
  }

  override def getParse[Result](parseState: ParseState,
                                parser: ParserBase[Result],
                                shouldCache: Boolean,
                                shouldDetectLeftRecursion: Boolean): Parse[Result] = {
    if (!shouldCache && !shouldDetectLeftRecursion) {
      return parser.parseInternal
    }
    if (shouldCache && shouldDetectLeftRecursion) {
      val parserState = parseState.parserStates.getOrElseUpdate(parser, new ParserState(parseState, parser)).asInstanceOf[ParserState[Result]]
      return parserState.cacheAndFixpoint
    }

    if (shouldCache) {
      return parseState.parserStates.getOrElseUpdate(parser, new CheckCache[Any](parseState, parser)).asInstanceOf[Parse[Result]]
    }

    val parserState = parseState.parserStates.getOrElseUpdate(parser, new ParserState(parseState, parser)).asInstanceOf[ParserState[Result]]
    parserState.checkFixpoint
  }

  type ParseState = PackratParseState
  class PackratParseState {
    val parserStates = mutable.HashMap[Parser[Any], ParserState[Any]]()
    val callStack = mutable.Stack[Parser[Any]]()
  }
}
