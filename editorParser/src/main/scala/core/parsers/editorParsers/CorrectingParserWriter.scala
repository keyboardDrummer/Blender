package core.parsers.editorParsers

import core.parsers.core.OptimizingParserWriter

trait CorrectingParserWriter extends OptimizingParserWriter {

  def findBestParseResult[Result](parser: Parser[Result], input: Input, mayStop: StopFunction): SingleParseResult[Result, Input] = {

    val noResultFound = ReadyParseResult(None, input, History.error(FatalError(input, "Grammar is always recursive")))
    var bestResult: ReadyParseResult[Result] = noResultFound

    mayStop.reset()
    var queue = parser(input, newParseState(input))
    while(queue.nonEmpty) {
      val (parseResult, tail) = queue.pop()

      queue = parseResult match {
        case parseResult: ReadyParseResult[Result] =>

          bestResult = if (bestResult.score >= parseResult.score) bestResult else parseResult
          tail match {
            case tailCons: SRCons[Result] =>
              if (mayStop(bestResult.remainder.offset, bestResult.originalScore, tailCons.head.score))
                SREmpty
              else
                tail
            case _ =>
              SREmpty
          }
        case delayedResult: DelayedParseResult[Result] =>
          val results = delayedResult.results
          tail.merge(results)
      }
    }
    SingleParseResult(bestResult.resultOption, bestResult.history.errors.toList)
  }

  def singleResult[Result](parseResult: LazyParseResult[Result]) =
    new SRCons(parseResult,0, SREmpty)

  type ParseResult[+Result] = SortedParseResults[Result]

  def newFailure[Result](error: MyParseError): SRCons[Result] =
    singleResult(ReadyParseResult(None, error.from, History.error(error)))

  def leftRightSimple[Left, Right, Result](left: Self[Left],
                                     right: => Self[Right],
                                     combine: (Left, Right) => Result): Self[Result] = {
    leftRight(left, right, combineSimple(combine))
  }

  def combineSimple[Left, Right, Result](f: (Left, Right) => Result): (Option[Left], Option[Right]) => Option[Result] =
    (ao, bo) => ao.flatMap(a => bo.map(b => f(a, b)))

  def leftRight[Left, Right, Result](left: Self[Left],
                                     right: => Self[Right],
                                     combine: (Option[Left], Option[Right]) => Option[Result]): Self[Result] =
    new Sequence(left, right, combine)

  override def choice[Result](first: Self[Result], other: => Self[Result], firstIsLonger: Boolean = false): Self[Result] =
    if (firstIsLonger) new FirstIsLonger(first, other) else new Choice(first, other)

  override def map[Result, NewResult](original: Self[Result], f: Result => NewResult): Self[NewResult] = new MapParser(original, f)

  case class RecursionsList[SeedResult, +Result](recursions: List[RecursiveParseResult[SeedResult, Result]], rest: SortedParseResults[Result])

  sealed trait SortedParseResults[+Result] extends ParseResultLike[Result]  {
    def nonEmpty: Boolean
    def pop(): (LazyParseResult[Result], SortedParseResults[Result])
    def toList: List[LazyParseResult[Result]]
    def tailDepth: Int

    def merge[Other >: Result](other: SortedParseResults[Other], depth: Int = 0,
                               bests: Map[Input, Double] = Map.empty): SortedParseResults[Other]

    override def map[NewResult](f: Result => NewResult): SortedParseResults[NewResult] = {
      flatMap(lazyParseResult => singleResult(lazyParseResult.map(f)), uniform = true)
    }

    def mapResult[NewResult](f: LazyParseResult[Result] => LazyParseResult[NewResult], uniform: Boolean): SortedParseResults[NewResult] = {
      flatMap(r => singleResult(f(r)), uniform)
    }

    def flatMap[NewResult](f: LazyParseResult[Result] => SortedParseResults[NewResult], uniform: Boolean): SortedParseResults[NewResult]

    def recursionsFor[SeedResult](parse: Parser[SeedResult]): RecursionsList[SeedResult, Result] =
      RecursionsList[SeedResult, Result](List.empty, this)

    def addHistory(errors: MyHistory): SortedParseResults[Result] = {
      mapWithHistory(x => x, errors)
    }

    def mapWithHistory[NewResult](f: ReadyParseResult[Result] => ReadyParseResult[NewResult],
                                  oldHistory: MyHistory): SortedParseResults[NewResult] = {
      mapResult(l => l.mapWithHistory(f, oldHistory), uniform = !oldHistory.canMerge)
    }

    def mapReady[NewResult](f: ReadyParseResult[Result] => ReadyParseResult[NewResult], uniform: Boolean): SortedParseResults[NewResult] = {
      mapResult(l => l.mapReady(f, uniform), uniform)
    }

    def flatMapReady[NewResult](f: ReadyParseResult[Result] => SortedParseResults[NewResult], uniform: Boolean): SortedParseResults[NewResult] = {
      flatMap[NewResult](l => l.flatMapReady(f, uniform), uniform)
    }

    def updateRemainder(f: Input => Input) = {
      mapReady(r => ReadyParseResult(r.resultOption, f(r.remainder), r.history), uniform = true)
    }
  }

  object SREmpty extends SortedParseResults[Nothing] {
    override def merge[Other >: Nothing](other: SortedParseResults[Other], depth: Int,
                                         bests: Map[Input, Double] = Map.empty) = other

    override def mapResult[NewResult](f: LazyParseResult[Nothing] => LazyParseResult[NewResult], uniform: Boolean): SREmpty.type = this

    override def flatMap[NewResult](f: LazyParseResult[Nothing] => SortedParseResults[NewResult], uniform: Boolean) = this

    override def map[NewResult](f: Nothing => NewResult) = this

    override def tailDepth = 0

    override def toList = List.empty

    override def nonEmpty = false

    override def pop() = throw new Exception("Can't pop empty results")
  }

  // TODO: replace List with something that has constant concat operation.
  case class RecursiveResults[+Result](recursions: Map[Parser[Any], List[RecursiveParseResult[_, Result]]], tail: SortedParseResults[Result])
    extends SortedParseResults[Result] {

    override def nonEmpty = false

    override def pop() = throw new Exception("Can't pop recursions")

    override def toList = tail.toList

    override def tailDepth = 0

    override def merge[Other >: Result](other: SortedParseResults[Other], depth: Int,
                                        bests: Map[Input, Double] = Map.empty): RecursiveResults[Other] = other match {
      case otherRecursions: RecursiveResults[Result] =>
        val merged = this.recursions.foldLeft(otherRecursions.recursions)((acc, entry) => {
          val value = acc.get(entry._1) match {
            case Some(existingValue) => existingValue ++ entry._2
            case None => entry._2
          }
          acc + (entry._1 -> value)
        })
        RecursiveResults(merged, tail.merge(otherRecursions.tail))
      case _ =>
        RecursiveResults(this.recursions, tail.merge(other))
    }

    override def flatMap[NewResult](f: LazyParseResult[Result] => SortedParseResults[NewResult], uniform: Boolean) = {
      RecursiveResults(
        recursions.mapValues(s => s.map(r => r.compose(pr => pr.flatMap(f, uniform)))),
        tail.flatMap(f, uniform))
    }

    override def recursionsFor[SeedResult](parser: Parser[SeedResult]) = {
      val remainder = recursions - parser
      RecursionsList(
        recursions.getOrElse(parser, List.empty).asInstanceOf[List[RecursiveParseResult[SeedResult, Result]]],
        if (remainder.isEmpty) tail else RecursiveResults(remainder, tail))
    }

    override def mapWithHistory[NewResult](f: ReadyParseResult[Result] => ReadyParseResult[NewResult], oldHistory: MyHistory) = {
      if (oldHistory.flawed)
        tail.mapWithHistory(f, oldHistory)
      else
        super.mapWithHistory(f, oldHistory)
    }
  }

  case class RecursiveParseResult[SeedResult, +Result](get: ParseResult[SeedResult] => ParseResult[Result]) {

    def compose[NewResult](f: ParseResult[Result] => ParseResult[NewResult]): RecursiveParseResult[SeedResult, NewResult] = {
      RecursiveParseResult[SeedResult, NewResult](r => f(get(r)))
    }
  }

  final class SRCons[+Result](val head: LazyParseResult[Result],
                              var tailDepth: Int,
                              _tail: => SortedParseResults[Result]) extends SortedParseResults[Result] {

    // Used for debugging
    def toList: List[LazyParseResult[Result]] = head :: tail.toList

    def getTail = tail
    lazy val tail = _tail

    if (tailDepth == 50) {
      tail
      tailDepth = 0
    }

    def flatMap[NewResult](f: LazyParseResult[Result] => SortedParseResults[NewResult], uniform: Boolean): SortedParseResults[NewResult] = {
      f(head) match {
        case SREmpty => tail.flatMap(f, uniform)
        case cons: SRCons[NewResult] =>

          if (!uniform && head.score != cons.head.score)
            cons.merge(tail.flatMap(f, uniform))
          else
          {
            new SRCons(
              cons.head,
              1 + Math.max(this.tailDepth, cons.tailDepth),
              cons.tail.merge(tail.flatMap(f, uniform)))
          }
        case other => other.merge(tail.flatMap(f, uniform))
      }
    }

    override def map[NewResult](f: Result => NewResult): SRCons[NewResult] = {
      new SRCons(head.map(f), tailDepth + 1, tail.map(f))
    }

    /*
    We don't want multiple results in the list with the same remainder. Instead we can just have the one with the best score.
    To accomplish this, we use the bests parameter.
     */
    override def merge[Other >: Result](other: SortedParseResults[Other],
                                        mergeDepth: Int,
                                        bests: Map[Input, Double] = Map.empty): SortedParseResults[Other] = {
      if (mergeDepth > 200) // Should be 200, since 100 is not enough to let CorrectionJsonTest.realLifeExample2 pass
        return SREmpty

      def getResult(head: LazyParseResult[Other], tailDepth: Int, getTail: Map[Input, Double] => SortedParseResults[Other]) = {
        head match {
          case ready: ReadyParseResult[Other] =>
            bests.get(ready.remainder) match {
              case Some(previousBest) if previousBest >= ready.score =>
                getTail(bests)
              case _ =>
                new SRCons(head, tailDepth, getTail(bests + (ready.remainder -> ready.score)))
            }
          case _ =>
            new SRCons(head, tailDepth, getTail(bests))
        }
      }

      other match {
        case SREmpty => this
        case cons: SRCons[Other] =>
          if (head.score >= cons.head.score) {
            getResult(head,1 + tailDepth, newBests => tail.merge(cons, mergeDepth + 1, newBests))
          } else
            getResult(cons.head,1 + cons.tailDepth, newBests => this.merge(cons.tail, mergeDepth + 1, newBests))
        case earlier => earlier.merge(this)
      }
    }

    override def nonEmpty = true

    override def pop(): (LazyParseResult[Result], SortedParseResults[Result]) = (head, tail)
  }

  trait LazyParseResult[+Result] {
    def flatMapReady[NewResult](f: ReadyParseResult[Result] => SortedParseResults[NewResult], uniform: Boolean): SortedParseResults[NewResult]

    def mapReady[NewResult](f: ReadyParseResult[Result] => ReadyParseResult[NewResult], uniform: Boolean): LazyParseResult[NewResult]

    val score: Double = (if (history.flawed) 0 else 10000) + history.score

    def history: MyHistory
    def map[NewResult](f: Result => NewResult): LazyParseResult[NewResult]

    def mapWithHistory[NewResult](f: ReadyParseResult[Result] => ReadyParseResult[NewResult],
                       oldHistory: MyHistory): LazyParseResult[NewResult]
  }

  class DelayedParseResult[Result](val history: MyHistory, _getResults: () => SortedParseResults[Result])
    extends LazyParseResult[Result] {

    override def toString = score + " delayed: " + history

    override def map[NewResult](f: Result => NewResult): DelayedParseResult[NewResult] = {
      new DelayedParseResult(history, () => results.map(f))
    }

    lazy val results: SortedParseResults[Result] = _getResults()

    override def mapWithHistory[NewResult](f: ReadyParseResult[Result] => ReadyParseResult[NewResult], oldHistory: MyHistory) =
      new DelayedParseResult(this.history ++ oldHistory, () => {
        val intermediate = this.results
        intermediate.mapWithHistory(f, oldHistory)
    })

    override def mapReady[NewResult](f: ReadyParseResult[Result] => ReadyParseResult[NewResult], uniform: Boolean): DelayedParseResult[NewResult] =
      new DelayedParseResult(this.history, () => {
        val intermediate = this.results
        intermediate.mapReady(f, uniform)
      })

    override def flatMapReady[NewResult](f: ReadyParseResult[Result] => SortedParseResults[NewResult], uniform: Boolean) =
      singleResult(new DelayedParseResult(this.history, () => {
        val intermediate = this.results
        intermediate.flatMapReady(f, uniform)
      }))
  }

  case class ReadyParseResult[+Result](resultOption: Option[Result], remainder: Input, history: MyHistory)
    extends LazyParseResult[Result] {

    val originalScore = (if (history.flawed) 0 else 10000) + history.score
    override val score = 10000 + originalScore

    override def map[NewResult](f: Result => NewResult): ReadyParseResult[NewResult] = {
      ReadyParseResult(resultOption.map(f), remainder, history)
    }

    override def mapWithHistory[NewResult](f: ReadyParseResult[Result] => ReadyParseResult[NewResult], oldHistory: MyHistory) = {
      val newReady = f(this)
      ReadyParseResult(newReady.resultOption, newReady.remainder, newReady.history ++ oldHistory)
    }

    override def mapReady[NewResult](f: ReadyParseResult[Result] => ReadyParseResult[NewResult], uniform: Boolean): ReadyParseResult[NewResult] = f(this)

    override def flatMapReady[NewResult](f: ReadyParseResult[Result] => SortedParseResults[NewResult], uniform: Boolean) = f(this)
  }

  class LeftIfRightMoved[+Left, Result](val left: Self[Left],
                                         _right: => Self[Result],
                                         combine: (Option[Left], Option[Result]) => Option[Result])
    extends ParserBuilderBase[Result] with SequenceLike[Result] {

    lazy val right: Self[Result] = _right

    override def getParser(recursive: GetParser): Parser[Result] = {
      val parseLeft = recursive(left)
      lazy val parseRight = recursive(right)

      class LeftIfRightParser extends Parser[Result] {

        def parse(input: Input, state: ParseState, mayFail: Boolean): ParseResult[Result] = {

          def rightFromLeftReady(leftReady: ReadyParseResult[Left]): SortedParseResults[Result] = {

            def mapRightResult(rightResult: ReadyParseResult[Result]): ReadyParseResult[Result] = ReadyParseResult(
              combine(leftReady.resultOption, rightResult.resultOption),
              rightResult.remainder,
              rightResult.history)

            val rightResult = parseRight(leftReady.remainder, state)
            rightResult.flatMap({
              case rightReady: ReadyParseResult[Result] =>
                if (rightReady.remainder == leftReady.remainder)
                  SREmpty
                else {
                  singleResult(rightReady.mapWithHistory(mapRightResult, leftReady.history))
                }
              case other => singleResult(other.mapWithHistory(mapRightResult, leftReady.history))
            }, uniform = !leftReady.history.canMerge)
          }

          val withoutLeft = parseRight(input, state)
          if (input.atEnd)
            return withoutLeft

          val withLeft = parseLeft(input, state).flatMapReady(rightFromLeftReady, uniform = false)
          withoutLeft.merge(withLeft)
        }

        override def apply(input: Input, state: ParseState): ParseResult[Result] = {
          parse(input, state, mayFail = true)
        }
      }
      new LeftIfRightParser
    }
  }

  class Sequence[+Left, +Right, Result](val left: Self[Left],
                                         _right: => Self[Right],
                                         combine: (Option[Left], Option[Right]) => Option[Result])
    extends ParserBuilderBase[Result] with SequenceLike[Result] {

    lazy val right: Self[Right] = _right

    override def getParser(recursive: GetParser): Parser[Result] = {
      val parseLeft = recursive(left)
      lazy val parseRight = recursive(right)

      new Parser[Result] {
        override def apply(input: Input, state: ParseState) = {
          val leftResults = parseLeft(input, state)

          val delayedLeftResults: SortedParseResults[Left] = leftResults.mapResult({
            case ready: ReadyParseResult[Left] =>
              if (ready.history.flawed) {
                new DelayedParseResult[Left](ready.history, () => singleResult(ready))
              }
              else
                ready
            case lazyResult => lazyResult
          }, false) // TODO set to true?

          def rightFromLeftReady(leftReady: ReadyParseResult[Left]): SortedParseResults[Result] = {
            def mapRightResult(rightResult: ReadyParseResult[Right]): ReadyParseResult[Result] = ReadyParseResult(
              combine(leftReady.resultOption, rightResult.resultOption),
              rightResult.remainder,
              rightResult.history)

            val rightResult = parseRight(leftReady.remainder, state)
            rightResult.mapWithHistory[Result](mapRightResult, leftReady.history)
          }
          delayedLeftResults.flatMapReady(rightFromLeftReady, uniform = false)
        }
      }
    }
  }

  class FirstIsLonger[+First <: Result, +Second <: Result, Result](val first: Self[First], _second: => Self[Second])
    extends ParserBuilderBase[Result] with ChoiceLike[Result] {

    lazy val second = _second

    override def getParser(recursive: GetParser): Parser[Result] = {
      val parseFirst = recursive(first)
      lazy val parseSecond = recursive(second)

      new Parser[Result] {
        override def apply(input: Input, state: ParseState) = {
          val firstResult = parseFirst(input, state)
          val secondResult = parseSecond(input, state)
          firstResult match {
            case cons: SRCons[Result]
              if !cons.head.history.flawed => firstResult
            case _ =>
              firstResult.merge(secondResult)
          }
        }
      }
    }
  }

  class Choice[+First <: Result, +Second <: Result, Result](val first: Self[First], _second: => Self[Second])
    extends ParserBuilderBase[Result] with ChoiceLike[Result] {

    lazy val second = _second

    override def getParser(recursive: GetParser): Parser[Result] = {
      val parseFirst = recursive(first)
      lazy val parseSecond = recursive(second)

      new Parser[Result] {
        override def apply(input: Input, state: ParseState) = {
          val firstResult = parseFirst(input, state)
          val secondResult = parseSecond(input, state)
          val merged = firstResult.merge(secondResult)
          merged
        }
      }
    }
  }

  object PositionParser extends ParserBuilderBase[Input] with LeafParser[Input] {

    override def getParser(recursive: GetParser): Parser[Input] = {
      (input, _) => {
        singleResult(ReadyParseResult(Some(input), input, History.empty))
      }
    }

    override def getMustConsume(cache: ConsumeCache) = false
  }

  case class WithRangeParser[Result, NewResult](original: Self[Result], addRange: (Input, Input, Result) => NewResult)
    extends ParserBuilderBase[NewResult] with ParserWrapper[NewResult] {

    override def getParser(recursive: GetParser): Parser[NewResult] = {
      val parseOriginal = recursive(original)
      (input, state) => {
        parseOriginal(input, state).mapReady(ready => {
          val newValue = ready.resultOption.map(v => addRange(input, ready.remainder, v))
          ReadyParseResult(newValue, ready.remainder, ready.history)
        }, uniform = true)
      }
    }
  }

  override def succeed[Result](result: Result): Self[Result] = Succeed(result)


  def newSuccess[Result](result: Result, remainder: Input, score: Double): SRCons[Result] =
    singleResult(ReadyParseResult(Some(result), remainder, History.success(remainder, remainder, result, score)))

  case class Succeed[Result](value: Result) extends ParserBuilderBase[Result] with LeafParser[Result] {

    override def getParser(recursive: GetParser): Parser[Result] = {
      (input: Input, _) => newSuccess(value, input, 0)
    }

    override def getMustConsume(cache: ConsumeCache) = false
  }

  class MapParser[Result, NewResult](val original: Self[Result], f: Result => NewResult)
    extends ParserBuilderBase[NewResult] with ParserWrapper[NewResult] {

    override def getParser(recursive: GetParser): Parser[NewResult] = {
      val parseOriginal = recursive(original)

      new Parser[NewResult] {
        override def apply(input: Input, state: ParseState): ParseResult[NewResult] = parseOriginal(input, state).map(f)
      }
    }
  }

  def newFailure[Result](partial: Option[Result], input: Input, errors: MyHistory) =
    singleResult(ReadyParseResult(partial, input, errors))

  type MyParseError = ParseError[Input]
  type MyHistory = History[Input]

  case class FatalError(location: Input, message: String, penalty: Double = History.failPenalty) extends MyParseError {
    override def append(other: MyParseError): Option[MyParseError] = None

    override def from = location

    override def to = from
  }
}

case class SingleParseResult[+Result, Input](resultOption: Option[Result], errors: List[ParseError[Input]]) {
  def successful = errors.isEmpty
  def get: Result = resultOption.get
}