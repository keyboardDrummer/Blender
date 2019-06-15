package core.parsers.core

import scala.language.{existentials, higherKinds}

trait ParserWriter {

  type Input <: ParseInput
  type ParseResult[+Result] <: ParseResultLike[Result]
  type Self[+R]

  def succeed[Result](result: Result): Self[Result]

  def choice[Result](first: Self[Result], other: => Self[Result], firstIsLonger: Boolean = false): Self[Result]

  def map[Result, NewResult](original: Self[Result], f: Result => NewResult): Self[NewResult]

  def many[Result, Sum](original: Self[Result], zero: Sum, reduce: (Result, Sum) => Sum): Self[Sum]

  implicit class ParserExtensions[+Result](parser: Self[Result]) {

    def |[Other >: Result](other: => Self[Other]) = choice(parser, other)

    def map[NewResult](f: Result => NewResult): Self[NewResult] = ParserWriter.this.map(parser, f)

    def option: Self[Option[Result]] = choice(this.map(x => Some(x)), succeed[Option[Result]](None), firstIsLonger = true)

    def many[Sum](zero: Sum, reduce: (Result, Sum) => Sum): Self[Sum] = ParserWriter.this.many(parser, zero, reduce)

    def * : Self[List[Result]] = {
      many(List.empty, (h: Result, t: List[Result]) => h :: t)
    }

    def ^^[NewResult](f: Result => NewResult) = map(f)

  }

  case class Success[+Result](result: Result, remainder: Input) {
    def map[NewResult](f: Result => NewResult): Success[NewResult] = Success(f(result), remainder)
  }

  trait ParseResultLike[+Result] {
    def map[NewResult](f: Result => NewResult): ParseResult[NewResult]
  }

}




