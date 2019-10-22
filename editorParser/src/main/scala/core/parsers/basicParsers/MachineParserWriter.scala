package core.parsers.basicParsers

import core.parsers.core.{ParserWriter, Processor}

trait MachineParserWriter extends ParserWriter {

  type Parser[+Result] = MachineParser[Result]

  trait MachineParser[+Result] {
    def parse(input: Input): ParseResult[Result]
  }

  def many[Result, Sum](original: MachineParser[Result], zero: Sum, reduce: (Result, Sum) => Sum) = {
    lazy val result: Self[Sum] = choice(leftRight(original, result, reduce), succeed(zero))
    result
  }

  type ParseResult[+Result] = SimpleParseResult[Result]
  override type Self[+Result] = Parser[Result]

  def succeed[Result](result: Result): Self[Result] = new SuccessParser(result)

  class SuccessParser[Result](result: Result) extends Parser[Result] {

    override def parse(input: Input): ParseResult[Result] = {
      newSuccess(result, input)
    }
  }

  def newSuccess[Result](result: Result, remainder: Input) = SimpleParseResult(Some(Success(result, remainder)))

  def fail[Result](message: String) = FailureParser

  def leftRight[Left, Right, NewResult](left: Parser[Left], right: => Parser[Right], combine: (Left, Right) => NewResult) =
    Sequence(left, right, combine)

  override def choice[Result](first: Parser[Result], other: => Parser[Result], firstIsLonger: Boolean = false) = new BiggestOfTwo(first, other)

  override def map[Result, NewResult](original: Parser[Result], f: Result => NewResult) = MapParser(original, f)

  case class MapParser[Result, NewResult](original: Self[Result], f: Result => NewResult)
    extends Parser[NewResult] {

    override def parse(input: Input) = {
      val result = original.parse(input)
      result.map[NewResult](f)
    }
  }

  object FailureParser extends Parser[Nothing] {
    override def parse(input: Input) = failureSingleton
  }

  class BiggestOfTwo[Result](val first: Parser[Result], _second: => Parser[Result])
    extends Parser[Result] {

    lazy val second = _second

    override def parse(input: Input) = {
      val firstResult = first.parse(input)
      val secondResult = second.parse(input)
      (firstResult.successOption, secondResult.successOption) match {
        case (Some(firstSuccess), Some(secondSuccess)) =>
          if (firstSuccess.remainder.offset >= secondSuccess.remainder.offset) firstResult else secondResult
        case (None, _) => secondResult
        case (_, None) => firstResult
      }
    }
  }

  case class Sequence[Left, Right, Result](left: Parser[Left], right: Parser[Right], combine: (Left, Right) => Result)
    extends Parser[Result] {

    override def parse(input: Input) = {
      val leftResult = left.parse(input)
      leftResult.successOption match {
        case None => failureSingleton
        case Some(leftSuccess) => right.parse(leftSuccess.remainder).map(r => combine(leftSuccess.result, r))
      }
    }
  }

  val failureSingleton = new SimpleParseResult[Nothing](None)

  case class SimpleParseResult[+Result](successOption: Option[Success[Result]]) extends ParseResultLike[Result] { // TODO Don't use nested Option

    def get = successOption.get.result

    override def map[NewResult](f: Result => NewResult) = SimpleParseResult(successOption.map(s => s.map(f)))

    def successful = successOption.nonEmpty
  }

  implicit class BasicParserExtensions[+Result](parser: Parser[Result]) {

    def many[Sum](zero: Sum, reduce: (Result, Sum) => Sum): Self[Sum] = MachineParserWriter.this.many(parser, zero, reduce)

    def * : Self[List[Result]] = {
      many(List.empty, (h: Result, t: List[Result]) => h :: t)
    }

    def manySeparated(separator: Self[Any]): Self[List[Result]] = {
      leftRight(parser, (separator ~> parser).*, (h: Result, t: List[Result]) => h :: t) | succeed(List.empty[Result])
    }

    def repN(amount: Int): Self[List[Result]] = {
      if (amount == 0) {
        succeed(List.empty[Result])
      } else {
        leftRight[Result, List[Result], List[Result]](parser, repN(amount - 1), (a,b) => a :: b)
      }
    }

    def ~[Right](right: => Self[Right]): Self[(Result, Right)] = leftRight(parser, right, (a: Result, b: Right) => (a, b))

    def ~<[Right](right: Self[Right]): Self[Result] = leftRight(parser, right, Processor.ignoreRight[Result, Right])

    def ~>[Right](right: Self[Right]): Self[Right] = leftRight(parser, right, Processor.ignoreLeft[Result, Right])

    def parseWholeInput(input: Input): ParseResult[Result] = {

      val parseResult = parser.parse(input)
      parseResult.successOption match {
        case Some(success) if !success.remainder.atEnd => failureSingleton
        case _ => parseResult
      }
    }
  }
}