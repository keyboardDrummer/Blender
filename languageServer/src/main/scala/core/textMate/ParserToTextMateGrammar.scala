package core.textMate

import core.parsers.core.OptimizingParserWriter
import core.parsers.strings.CommonParserWriter
import util.GraphBasics

import scala.util.matching.Regex

trait JsExpression
case class JsArray(elements: Seq[JsExpression]) extends JsExpression
case class JsObject(fields: Map[String, JsExpression]) extends JsExpression
case class JsLiteral(value: String) extends JsExpression

trait ColoringParserWriter extends CommonParserWriter with OptimizingParserWriter {
  case class Colorize[Result](original: Self[Result], textMateScope: String) extends ParserBuilderBase[Result] with ParserWrapper[Result] {
    override def getParser(recursive: GetParser) = recursive(original)
  }

  def toTextMate(grammar: Self[_]): String = {
    val textMate: JsExpression = createTextMateAstFromBiGrammar(grammar)
    Json.printJson(textMate)
  }

  // TODO let this operate on parsers instead of BiGrammar, that way less cases have to be handled.
  def grammarToRegex[Result](root: Self[Result]): Option[String] = {
    var callStack = List.empty[Self[_]]

    def recurse(grammar: Self[_]): Option[String] = {
      if (callStack.contains(grammar))
        return None

      callStack ::= grammar

      val result: Option[String] = grammar match {
        case drop: DropParser[_] => recurse(drop.original)
        case sequence: Sequence[_, _, _] =>
          for {
            left <- recurse(sequence.left)
            right <- recurse(sequence.right)
          } yield left + right
        case choice: Choice[_, _, _] =>
          for {
            left <- recurse(choice.first)
            right <- recurse(choice.second)
          } yield left + "|" + right
        case regex: RegexParser => Some(regex.regex.regex)
        case map: MapParser[_, _] => recurse(map.original)
        case delimiter: Literal => Some(escapeLiteral(delimiter.value))
        case keyword: KeywordParser => Some("\\b" + escapeLiteral(keyword.value) + "\\b")
        // TODO how to incorporate many? case many: Many => recurse(many.inner).map(r => r + "*") // TODO add parenthesis
      }

      callStack = callStack.tail
      result
    }

    recurse(root)
  }

  case class Match(scope: String, regex: Regex)

  def createTextMateAstFromBiGrammar(grammar: Self[_]): JsExpression = {
    val reachables: Seq[Self[_]] = GraphBasics.traverseBreadth[Self[_]](Seq(grammar), grammar => grammar.children,
      node => if (node.isInstanceOf[Colorize[_]] || node.isInstanceOf[KeywordParser] ||
        node == parseIdentifier || node == stringLiteral) GraphBasics.SkipChildren else GraphBasics.Continue )

    val typedPatterns: Seq[Match] = reachables.collect({
      case parser if parser == parseIdentifier =>
        Match("variable", """\b[A-Za-z][A-Za-z0-9_]*\b""".r)
      case regexParser: RegexParser if Set("decimal number","floating point number","whole number").contains(regexParser.regexName) =>
        Match("constant.numeric", """-?\d+""".r)
      case keyword: KeywordParser /*if keyword.reserved*/ =>
        Match("keyword.control", grammarToRegex(keyword).get.r)
      case delimiter: Literal =>
        Match("keyword.operator", escapeLiteral(delimiter.value).r)
      case parser if parser == stringLiteral =>
        Match("string.quoted.double", grammarToRegex(parser).get.r)
      case Colorize(inner, textMateScope) =>
        val maybeRegex = grammarToRegex(inner)
        maybeRegex match {
          case None => throw new Exception("Colorize did not contain a regex")
          case Some(regex) =>
// TODO turn on validation, once we figure out how to filter out NOT \n, and ending in \n. For example the line comment regex currently incorrectly triggers this validation
//            if (regex.contains("\\n") || regex.contains("\\s"))
//              throw new Exception(s"Colorize regex $regex contained a newline")

            Match(textMateScope, regex.r)
        }
    }).sortBy(n => n.regex.regex)

    val patterns = typedPatterns.map(pattern => singleMatch(pattern.scope, pattern.regex)).distinct
    JsObject(Map(
      "patterns" -> JsArray(patterns)
    ))
  }

  def singleMatch(name: String, regex: Regex): JsExpression = {
    JsObject(Map(
      "name" -> JsLiteral(name),
      "match" -> JsLiteral(regex.toString())))
  }

  def escapeLiteral(regex: String): String = {
    var result = regex
    val chars = "\\<([{^-=$!|]})?*+.>"
    for(char <- chars) {
      result = result.replace(char.toString, "\\" + char)
    }
    result
  }
}