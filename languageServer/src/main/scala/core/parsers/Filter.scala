package core.parsers

case class Filter[Input <: ParseInput, Other, +Result <: Other](original: Parser[Input, Result], predicate: Other => Boolean, getMessage: Other => String) extends Parser[Input, Result] {
  override def parseNaively(input: Input, state: ParseState): ParseResult[Result] = original.parseNaively(input, state) match {
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