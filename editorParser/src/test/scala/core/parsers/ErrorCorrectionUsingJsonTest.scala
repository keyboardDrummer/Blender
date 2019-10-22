package core.parsers

import org.scalatest.FunSuite
import _root_.core.parsers.editorParsers.UntilBestAndXStepsStopFunction

class ErrorCorrectionUsingJsonTest extends FunSuite {
  import ParseJson._

  test("removes incorrect b at start") {
    val input = """b{"hello":3}"""
    parseJson(input, List(("hello","3")), 1)
  }

  test("object with single member with number value") {
    val input = """{"person":3}"""
    parseJson(input, List(("person","3")), 0)
  }

  test("object with single member with string value") {
    val input = """{"person":"remy"}"""
    parseJson(input, List(("person","remy")), 0)
  }

  // Currently the correct result has quite a low score of -10, leading to a huge amount of crap results being tried,
  // Even [ [ [ [ has a better score than -10.
  test("garbage after number") {
    val input = """3blaa"""
    parseJson(input, "3", 1)
  }

  test("nothing as input") {
    val input = ""
    parseJson(input, UnknownExpression, 1)
  }

  test("object start with nothing else") {
    val input = """{"""
    parseJson(input, List.empty, 1)
  }

  test("object member with only the key") {
    val input = """{"person""""
    val expectation = List(("""person""", UnknownExpression))
    parseJson(input, expectation, 1)
  }

  test("object member with no expression") {
    val input = """{"person":"""
    val expectation = List(("person", UnknownExpression))
    parseJson(input, expectation, 1)
  }

  test("object member with only an unfinished key") {
    val input = """{"person"""
    val expectation = List(("person", UnknownExpression))
    parseJson(input, expectation, 1)
  }

  test("object member with an unfinished value") {
    val input = """{"person":"remy"""
    parseJson(input, List(("person", "remy")), 1)
  }

  test("object with a single member and comma") {
    val input = """{"person":3,"""
    val expectation = List("person" -> "3", "" -> UnknownExpression)
    parseJson(input, expectation, 1)
  }

  test("object with a single member and half second member") {
    val input = """{"person":3,"second""""
    val expectation = List(("person", "3"), ("second", UnknownExpression))
    parseJson(input, expectation, 1)
  }

  test("real life example missing :value") {
    val input = """{"Resources":{"NotificationTopic":{"Type":"AWS::SNS::Topic","Properties":{"Subscription"}}}}"""
    val expectation = List(("Resources",List(("NotificationTopic",List(("Type","AWS::SNS::Topic"), ("Properties",List(("Subscription", UnknownExpression))))))))
    parseJson(input, expectation, 1)
  }

  test("real life example 2") {
    val input = """{"Resources":{"NotificationTopic":{"Properties":{"Subsc"""
    val expectation = List(("Resources",List(("NotificationTopic",List(("Properties",List(("Subsc", UnknownExpression))))))))
    parseJson(input, expectation, 1)
  }

  test("garbage before key") {
    val input = """{g"person":"remy"}"""
    val expectation = List("person" -> "remy")
    parseJson(input, expectation, 1)
  }

  test("intertwined small garbage and success") {
    val input = """{g"person"hj:nh"remy"}"""
    val expectation = List("person" -> "remy")
    parseJson(input, expectation, 3)
  }

  // Partially Parse tests start
  test("object with single member with string value, where the colon is missing") {
    val input = """{"person""remy"}"""
    val expectation = List(("person", "remy"))
    parseJson(input, expectation, 1)
  }

  test("two members but the first misses a colon") {
    val input = """{"person""remy","friend":"jeroen"}"""
    val expectation = List(("person", "remy"), ("friend", "jeroen"))
    parseJson(input, expectation, 1)
  }

  test("two members but the comma is missing") {
    val input = """{"person":"remy""friend":"jeroen"}"""
    val expectation = List(("person", "remy"), ("friend", "jeroen"))
    parseJson(input, expectation, 1)
  }

  test("two members both missing colon") {
    val input = """{"person""remy","friend""jeroen"}"""
    val expectation = List(("person", "remy"), ("friend", "jeroen"))
    parseJson(input, expectation, 2)
  }

  test("two members, no first colon and comma") {
    val input = """{"person""remy""friend":"jeroen"}"""
    val expectation = List(("person", "remy"), ("friend", "jeroen"))
    parseJson(input, expectation, 2)
  }

  test("two members, no colons or comma") {
    val input = """{"person""remy""friend""jeroen"}"""
    val expectation = List(("person", "remy"), ("friend", "jeroen"))
    parseJson(input, expectation, 3)
  }

  test("nested two members without colons/comma") {
    val input = """{"directory"{"person""remy""friend""jeroen"}}"""
    val expectation = List("directory" -> List("person" -> "remy", "friend" -> "jeroen"))
    parseJson(input, expectation, 4)
  }

  test("starting brace insertion") {
    val input = """{"person":"remy":"jeroen"}}"""
    val expectation = List(("person",List(("remy","jeroen"))))
    parseJson(input, expectation, 1, 100)
  }

  test("starting brace insertion 2") {
    val input = """{"person""remy":"jeroen"}}"""
    val expectation = List(("person",List(("remy","jeroen"))))
    parseJson(input, expectation, 1, 100)
  }

  test("starting brace insertion unambiguous") {
    val input = """{"person":"remy":"jeroen","remy":"jeroen"}}"""
    val expectation = List(("person",List("remy" -> "jeroen", "remy" -> "jeroen")))
    parseJson(input, expectation, 1, 100)
  }

  /*
  Suppose we have grammar 'expr = { expr } | x' and several inputs
  1. 'veryLongGarbageString'
  2. }
  To be able to parse (2), we must allow running growResults even if no extra input has been consumed,
  otherwise the seed value 'x' will never be inserted.
  Maybe we can allow no consumed input only for the seed value?

  If we disallow growing beyond the seed when there is input consumption, then we disable trying
  {{{{{ and {{x}} for input (1)

  While input (2) still parses to {x}
   */
  test("mass garbage, and virtual left recursion through error correction") {
    val input = """doesNotMatchdoesNotMatchdoesNotMatchdoesNotMatchdoesNotMatchdoesNotMatchdoesNotMatchdoesNotMatch"""
    lazy val parser: Self[Any] = "{" ~ parser | "!"
    val detectValueParser = new DetectValueParser(("{",("{", ("{", "!"))), parser)
    val result = detectValueParser.getWholeInputParser.parse(new StringReader(input))
    assert(!detectValueParser.detected)
    assertResult(2)(result.errors.size)
    assert(result.resultOption.nonEmpty)
    assertResult("!")(result.resultOption.get)
  }

  class DetectValueParser[Result](valueToDetect: Result, val original: ParserBuilder[Result]) extends ParserBuilderBase[Result] with ParserWrapper[Result] {
    var detected = false

    override def getParser(recursive: ParseJson.GetParser) = {
      val parseOriginal = recursive(original)
      (input, state) => {
        val result = parseOriginal(input, state)
        result.map(resultValue => {
          if (resultValue == valueToDetect) {
            detected = true
          }
          resultValue
        })
      }
    }
  }

  // Add test for left recursion and errors
  // Add test with multiple errors in one branch "b" => "a" "b" "c"
  // Add test with three way branch with 0,1,2 errors, and 0,2,1 errors.
  private def parseJson(input: String, expectation: Any, errorCount: Int, steps: Int = 0) = {
    val result = jsonParser.getWholeInputParser.parse(new StringReader(input), UntilBestAndXStepsStopFunction(steps))
    System.out.println(result.errors.toString())
    assertResult(expectation)(result.resultOption.get)
    assertResult(errorCount)(result.errors.size)
  }
}



