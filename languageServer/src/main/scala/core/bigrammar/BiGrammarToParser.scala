package core.bigrammar

import core.bigrammar.grammars._
import langserver.types.Position
import languageServer.HumanPosition

import scala.collection.mutable
import scala.util.matching.Regex
import scala.util.parsing.combinator.{JavaTokenParsers, PackratParsers}
import scala.util.parsing.input.{CharArrayReader, OffsetPosition, Positional}

case class WithMapG[T](value: T, map: Map[Any,Any]) {}

//noinspection ZeroIndexToHead
object BiGrammarToParser extends JavaTokenParsers with PackratParsers {
  type WithMap = WithMapG[Any]
  type State = Map[Any, Any]
  type Result = StateFull[WithMap]

  def valueToResult(value: Any): Result = (state: State) => (state, WithMapG(value, Map.empty))

  def toStringParser(grammar: BiGrammar): String => ParseResult[Any] =
    input => toParser(grammar)(new CharArrayReader(input.toCharArray))

  def toParser(grammar: BiGrammar): PackratParser[Any] = {

    var keywords: Set[String] = Set.empty
    val allGrammars: Set[BiGrammar] = grammar.selfAndDescendants.toSet
    keywords ++= allGrammars.flatMap({
      case keyword: Keyword => if (keyword.reserved) Set(keyword.value) else Set.empty[String]
      case _ => Set.empty[String]
    })

    val valueParser: BiGrammarToParser.Parser[Any] = toParser(grammar, keywords)
    phrase(valueParser)
  }

  def toParser(grammar: BiGrammar, keywords: scala.collection.Set[String]): BiGrammarToParser.Parser[Any] = {
    val cache: mutable.Map[BiGrammar, PackratParser[Result]] = mutable.Map.empty
    lazy val recursive: BiGrammar => PackratParser[Result] = grammar => {
      cache.getOrElseUpdate(grammar, memo(toParser(keywords, recursive, grammar)))
    }
    val resultParser = toParser(keywords, recursive, grammar)
    val valueParser = resultParser.map(result => result(Map.empty[Any, Any])._2.value)
    valueParser
  }

  private def toParser(keywords: scala.collection.Set[String], recursive: BiGrammar => Parser[Result], grammar: BiGrammar): Parser[Result] = {
    grammar match {
      case sequence: Sequence =>
        val firstParser = recursive(sequence.first)
        val secondParser = recursive(sequence.second)
        val parser = for {
          firstResult <- firstParser
          secondResult <- secondParser
        } yield {
          val result: Result = (state: State) => {
            val firstMap = firstResult(state)
            val secondMap = secondResult(firstMap._1)
            val resultValue = (firstMap._2.value, secondMap._2.value)
            val resultMap = firstMap._2.map ++ secondMap._2.map
            (secondMap._1, WithMapG[Any](resultValue, resultMap)): (State, WithMapG[Any])
          }
          result
        }
        parser
      case choice: Choice =>
        val firstParser = recursive(choice.left)
        val secondParser = recursive(choice.right)
        if (choice.firstBeforeSecond) firstParser | secondParser else firstParser ||| secondParser

      case custom: CustomGrammarWithoutChildren => custom.getParser(keywords).map(valueToResult)
      case custom: CustomGrammar => custom.toParser(recursive)

      case many: Many =>
        val innerParser = recursive(many.inner)
        val manyInners = innerParser.* //TODO by implementing * ourselves we can get rid of the intermediate List.
        val parser: Parser[Result] = manyInners.map(elements => {
          val result: Result = (initialState: State) => {
            var state = initialState
            var totalMap = Map.empty[Any, Any]
            var totalValue = List.empty[Any]
            elements.foreach(elementStateM => {
              val elementResult = elementStateM(state)
              state = elementResult._1
              totalMap = totalMap ++ elementResult._2.map
              totalValue ::= elementResult._2.value
            })
            (state, WithMapG[Any](totalValue.reverse, totalMap))
          }
          result
        })
        parser
      case mapGrammar: MapGrammarWithMap =>
        val innerParser = recursive(mapGrammar.inner)
        innerParser.map(result => result.map(mapGrammar.construct))

      case BiFailure(message) => failure(message)
      case Print(_) => success(Unit).map(valueToResult)
      case ValueGrammar(value) => success(value).map(valueToResult)

      case labelled: Labelled =>
        lazy val inner = recursive(labelled.inner)
        Parser { in => inner.apply(in) } //Laziness to prevent infinite recursion
    }
  }

  /**
    * Improves the error message slightly over the original
    */
  override implicit def literal(value: String): Parser[String] = new Parser[String] {
    override def apply(in: BiGrammarToParser.Input): BiGrammarToParser.ParseResult[String] = {
      val source = in.source
      val offset = in.offset
      val start = offset
      var matchLength = 0
      var currentPosition = start
      while (matchLength < value.length && currentPosition < source.length && value.charAt(matchLength) == source.charAt(currentPosition)) {
        matchLength += 1
        currentPosition += 1
      }
      if (matchLength == value.length)
        Success(source.subSequence(start, currentPosition).toString, in.drop(currentPosition - offset))
      else {
        val nextPosition = currentPosition + 1
        val found = if (nextPosition >= source.length()) "end of source" else "`" + source.subSequence(start, nextPosition) + "'"
        Failure("`" + value + "' expected but " + found + " found", in.drop(matchLength))
      }
    }
  }

  override val whiteSpace: Regex = "".r

  def position[T <: Positional]: Parser[Position] = Parser { in =>
    val offsetPosition = in.pos.asInstanceOf[OffsetPosition]
    Success(new HumanPosition(offsetPosition.line, offsetPosition.column), in)
  }
}