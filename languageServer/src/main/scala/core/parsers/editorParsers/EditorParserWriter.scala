package core.parsers.editorParsers

import core.parsers.core.ParserWriter
import util.cache.{Cache, InfiniteCache}

trait EditorParserWriter extends ParserWriter {

  type Self[+R] = EditorParser[R]
  type ParseResult[+R] = EditorParseResult[R]
  type ExtraState = DefaultCache

  override def failure[R](input: Input, message: String): ParseFailure[Nothing] = ParseFailure(None, input, message)

  override def leftRight[Left, Right, NewResult](left: EditorParser[Left],
                                                 right: => EditorParser[Right],
                                                 combine: (Left, Right) => NewResult): EditorParser[NewResult] =
    new Sequence(left, right, combine)

  override def succeed[NR](result: NR): EditorParser[NR] = Succeed(result)

  override def choice[Result](first: EditorParser[Result], other: => EditorParser[Result], leftIsAlwaysBigger: Boolean): EditorParser[Result] =
    if (leftIsAlwaysBigger) new OrElse(first, other) else new BiggestOfTwo(first, other)

  override def flatMap[Result, NewResult](left: EditorParser[Result], f: Result => EditorParser[NewResult]): EditorParser[NewResult] = new FlatMap(left, f)

  override def map[Result, NewResult](original: Self[Result], f: Result => NewResult): Self[NewResult] = new MapParser(original, f)

  trait EditorParser[+Result] extends Parser[Result] with HasGetDefault[Result] {
    final def getDefault(state: PState): Option[Result] = getDefault(state.extraState)
  }

  override def fail[Result](message: String) = Fail(message)

  case class Fail(message: String) extends EditorParser[Nothing] {
    override def getDefault(cache: DefaultCache) = None

    override def parseInternal(input: Input, state: PState) = ParseFailure(None, input, message)
  }

  override def lazyParser[Result](inner: => EditorParser[Result]) = new EditorLazy(inner)

  implicit class EditorParserExtensions[+Result](parser: EditorParser[Result]) extends ParserExtensions(parser) {

    def filter[Other >: Result](predicate: Other => Boolean, getMessage: Other => String) = Filter(parser, predicate, getMessage)

    def withDefault[Other >: Result](_default: Other): EditorParser[Other] =
      WithDefault[Other](parser, cache => Some(_default))

    def parseWholeInput(input: Input,
                        cache: Cache[PN, ParseResult[Any]] = new InfiniteCache()): ParseResult[Result] = {

      parse(input, cache) match {
        case success: ParseSuccess[Result] =>
          if (success.remainder.atEnd) success
          else {
            val failedSuccess = ParseFailure(Some(success.result), success.remainder, "Did not parse entire input")
            failedSuccess.getBiggest(success.biggestFailure)
          }
        case f => f
      }
    }

    def parse(input: Input,
              cache: Cache[PN, ParseResult[Any]] = new InfiniteCache()): ParseResult[Result] = {

      val state = new PackratParseState(cache, new DefaultCache)
      state.parseIteratively(parser, input)
    }

    def withRange[Other >: Result](addRange: (Input, Input, Result) => Other): EditorParser[Other] = {
      val withPosition = new Sequence(
        new PositionParser(),
        new WithRemainderParser(parser),
        (left: Input, resultRight: (Result, Input)) => addRange(left, resultRight._2, resultRight._1))
      WithDefault(withPosition, cache => parser.getDefault(cache))
    }
  }

  case class WithDefault[+Result](original: Parser[Result], _getDefault: DefaultCache => Option[Result])
    extends EditorParser[Result] {
    override def parseInternal(input: Input, state: PState): ParseResult[Result] = {
      state.parse(original, input) match {
        case failure: ParseFailure[Result] if failure.partialResult.isEmpty || failure.remainder == input =>
          new ParseFailure[Result](_getDefault(state.extraState), failure.remainder, failure.message)
        case x => x
      }
    }

    override def getDefault(cache: DefaultCache): Option[Result] =
      _getDefault(cache)
  }

  class Sequence[+Left, +Right, +Result](left: EditorParser[Left],
                                         _right: => EditorParser[Right],
                                         combine: (Left, Right) => Result) extends EditorParser[Result] {
    lazy val right: EditorParser[Right] = _right

    override def parseInternal(input: Input, state: PState): ParseResult[Result] = {
      val leftResult = state.parse(left, input)
      leftResult match {
        case leftSuccess: ParseSuccess[Left] =>
          val rightResult = state.parse(right, leftSuccess.remainder)
          rightResult match {
            case rightSuccess: ParseSuccess[Right] =>
              rightSuccess.map(r => combine(leftSuccess.result, r)).
                addFailure(leftSuccess.biggestFailure.map(l => combine(l, rightSuccess.result)))

            case rightFailure: ParseFailure[Right] =>
              if (leftSuccess.biggestFailure.offset > rightFailure.offset && rightFailure.partialResult.nonEmpty) {
                val rightDefault = rightFailure.partialResult.get
                leftSuccess.biggestFailure.map(l => combine(l, rightDefault)).asInstanceOf[ParseFailure[Result]]
              }
              else {
                rightFailure.map(right => combine(leftSuccess.result, right))
              }
          }

        case leftFailure: ParseFailure[Left] =>
          val result = for {
            leftPartial <- leftFailure.partialResult
            rightDefault <- right.getDefault(state)
          } yield combine(leftPartial, rightDefault)
          ParseFailure(result, leftFailure.remainder, leftFailure.message)
      }
    }

    override def getDefault(cache: DefaultCache): Option[Result] = for {
      leftDefault <- cache(left)
      rightDefault <- cache(right)
    } yield combine(leftDefault, rightDefault)
  }

  class OrElse[+First <: Result, +Second <: Result, +Result](first: EditorParser[First], _second: => EditorParser[Second])
    extends EditorParser[Result] {
    lazy val second = _second

    override def parseInternal(input: Input, state: PState): ParseResult[Result] = {
      val firstResult = state.parse(first, input)
      val result = firstResult match {
        case _: ParseSuccess[Result] => firstResult
        case firstFailure: ParseFailure[Result] =>
          val secondResult = state.parse(second, input)
          secondResult match {
            case secondSuccess: ParseSuccess[Result] =>
              val biggestFailure = firstFailure.getBiggest(secondSuccess.biggestFailure)
              ParseSuccess(secondSuccess.result, secondSuccess.remainder, biggestFailure)
            case secondFailure: ParseFailure[Result] =>
              firstFailure.getBiggest(secondFailure)
          }
      }
      getDefault(state).fold[ParseResult[Result]](result)(d => result.addDefault[Result](d))
    }

    override def getDefault(cache: DefaultCache): Option[Result] = {
      val value: Option[First] = cache(first)
      value.orElse(cache(second))
    }
  }

  class BiggestOfTwo[+First <: Result, +Second <: Result, +Result](first: EditorParser[First], _second: => EditorParser[Second])
    extends EditorParser[Result] {
    lazy val second = _second

    override def parseInternal(input: Input, state: PState): ParseResult[Result] = {
      val firstResult = state.parse(first, input)
      val secondResult = state.parse(second, input)
      val result = (firstResult, secondResult) match {
        case (firstSuccess: ParseSuccess[Result], secondSuccess: ParseSuccess[Result]) =>
          if (firstSuccess.remainder.offset >= secondSuccess.remainder.offset)
            firstSuccess.addFailure(secondSuccess.biggestFailure)
          else
            secondSuccess.addFailure(firstSuccess.biggestFailure)
        case (firstFailure: ParseFailure[Result], secondSuccess: ParseSuccess[Result]) =>
          secondSuccess.addFailure(firstFailure)
        case (firstSuccess: ParseSuccess[Result], secondFailure: ParseFailure[Result]) =>
          firstSuccess.addFailure(secondFailure)
        case (firstFailure: ParseFailure[Result], secondFailure: ParseFailure[Result]) =>
          firstFailure.getBiggest(secondFailure)
        case _ => throw new Exception("can not occur")
      }
      getDefault(state).fold[ParseResult[Result]](result)(d => result.addDefault[Result](d))
    }

    override def getDefault(cache: DefaultCache): Option[Result] = {
      val value: Option[First] = cache(first)
      value.orElse(cache(second))
    }
  }

  case class Succeed[+Result](value: Result) extends EditorParser[Result] {
    override def parseInternal(inputs: Input, cache: PState): ParseResult[Result] = ParseSuccess(value, inputs, NoFailure)

    override def getDefault(cache: DefaultCache): Option[Result] = Some(value)
  }

  class FlatMap[+Result, +NewResult](left: EditorParser[Result], getRight: Result => EditorParser[NewResult])
    extends EditorParser[NewResult] {

    override def parseInternal(input: Input, state: PState): ParseResult[NewResult] = {
      val leftResult = state.parse(left, input)
      leftResult match {
        case leftSuccess: ParseSuccess[Result] =>
          val right = getRight(leftSuccess.result)
          val rightResult = state.parse(right, leftSuccess.remainder)
          rightResult match {
            case rightSuccess: ParseSuccess[NewResult] =>
              rightSuccess.
                addFailure(leftSuccess.biggestFailure match {
                  case NoFailure => NoFailure
                  case ParseFailure(partialResult, remainder, message) =>
                    ParseFailure(partialResult.flatMap(leftPartial => getRight(leftPartial).getDefault(state)), remainder, message)
                })

            case rightFailure: ParseFailure[NewResult] =>
              if (leftSuccess.biggestFailure.offset > rightFailure.offset) {
                val biggestFailure = leftSuccess.biggestFailure.asInstanceOf[ParseFailure[Result]]
                ParseFailure(rightFailure.partialResult, biggestFailure.remainder, biggestFailure.message)
              }
              else {
                rightFailure
              }
          }

        case leftFailure: ParseFailure[Result] =>
          val result = for {
            leftPartial <- leftFailure.partialResult
            rightDefault <- getRight(leftPartial).getDefault(state)
          } yield rightDefault
          ParseFailure(result, leftFailure.remainder, leftFailure.message)
      }
    }

    override def getDefault(cache: DefaultCache): Option[NewResult] = for {
      leftDefault <- cache(left)
      rightDefault <- cache(getRight(leftDefault))
    } yield rightDefault
  }

  class MapParser[+Result, NewResult](original: EditorParser[Result], f: Result => NewResult) extends EditorParser[NewResult] {
    override def parseInternal(input: Input, state: PState): ParseResult[NewResult] = {
      state.parse(original, input).map(f)
    }

    override def getDefault(cache: DefaultCache): Option[NewResult] = cache(original).map(f)
  }

  class PositionParser extends EditorParser[Input] {

    override def parseInternal(input: Input, state: PState): ParseResult[Input] = {
      ParseSuccess[Input](input, input, NoFailure)
    }

    override def getDefault(cache: DefaultCache): Option[Input] = None
  }

  class WithRemainderParser[Result](original: Parser[Result])
    extends EditorParser[(Result, Input)] {

    override def parseInternal(input: Input, parseState: PState): ParseResult[(Result, Input)] = {
      val parseResult = parseState.parse(original, input)

      parseResult.map(result => (result, parseResult.remainder))
    }

    override def getDefault(cache: DefaultCache): Option[(Result, Input)] = None
  }

  case class Filter[Other, +Result <: Other](original: EditorParser[Result], predicate: Other => Boolean, getMessage: Other => String)
    extends EditorParser[Result] {
    override def parseInternal(input: Input, state: PState): ParseResult[Result] = original.parseInternal(input, state) match {
      case success: ParseSuccess[Result] =>
        if (predicate(success.result)) success
        else ParseFailure(this.getDefault(state), success.remainder, getMessage(success.result)).getBiggest(success.biggestFailure)
      case failure: ParseFailure[Result] =>
        val partialResult = failure.partialResult.filter(predicate).orElse(this.getDefault(state))
        ParseFailure(partialResult, failure.remainder, failure.message)
    }

    override def getDefault(cache: DefaultCache): Option[Result] =
      original.getDefault(cache).filter(predicate)
  }

  class EditorLazy[+Result](_inner: => EditorParser[Result]) extends Lazy[Result](_inner) with EditorParser[Result] {

    override def getDefault(cache: DefaultCache): Option[Result] = cache(inner.asInstanceOf[EditorParser[Result]])
  }

  case class ParseFailure[+Result](partialResult: Option[Result], remainder: Input, message: String)
    extends EditorParseResult[Result] with OptionFailure[Result] {

    override def map[NewResult](f: Result => NewResult): ParseFailure[NewResult] =
      ParseFailure(partialResult.map(r => f(r)), remainder, message)

    override def offset: Int = remainder.offset

    def getBiggest[Other >: Result](other: OptionFailure[Other]): ParseFailure[Other] = {
      if (offset > other.offset) this else other.asInstanceOf[ParseFailure[Result]]
    }

    override def get: Result = throw new Exception("get was called on a ParseFailure")

    override def getSuccessRemainder: Option[Input] = None

    override def toString: String = message

    override def getPartial = partialResult

    override def addDefault[Other >: Result](value: Other): ParseFailure[Other] = partialResult match {
      case _: Some[Result] => this
      case None => ParseFailure(Some(value), remainder, message)
    }

  }

  trait EditorParseResult[+Result] extends ParseResultLike[Result] {

    def getPartial: Option[Result]
    def get: Result
    def remainder: Input

    def addDefault[Other >: Result](value: Other): EditorParseResult[Other]
  }

  case class ParseSuccess[+Result](result: Result, remainder: Input, biggestFailure: OptionFailure[Result])
    extends EditorParseResult[Result] {
    override def map[NewResult](f: Result => NewResult): ParseSuccess[NewResult] = {
      ParseSuccess[NewResult](f(result), remainder, biggestFailure.map(f))
    }

    def biggestRealFailure: Option[ParseFailure[Result]] = biggestFailure match {
      case failure: ParseFailure[Result] => Some(failure)
      case _ => None
    }

    def addFailure[Other >: Result](other: OptionFailure[Other]): ParseSuccess[Other] =
      if (biggestFailure.offset > other.offset) this else
        ParseSuccess(result, remainder, other)

    override def get: Result = result

    override def getSuccessRemainder: Option[Input] = Some(remainder)

    override def getPartial = Some(result)

    override def addDefault[Other >: Result](value: Other) = biggestFailure match {
      case NoFailure => this
      case f: ParseFailure[Result] => ParseSuccess(result, remainder, f.addDefault(value))
    }
  }

  trait OptionFailure[+Result] {
    def offset: Int
    def partialResult: Option[Result]
    def map[NewResult](f: Result => NewResult): OptionFailure[NewResult]
  }

  object NoFailure extends OptionFailure[Nothing] {
    override def offset: Int = -1

    override def map[NewResult](f: Nothing => NewResult): OptionFailure[NewResult] = this

    override def partialResult: Option[Nothing] = None
  }

}
