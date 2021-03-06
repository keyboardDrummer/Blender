package miksilo.editorParser.parsers.strings

import miksilo.editorParser.parsers.core.{OptimizingParserWriter, Processor}

trait NoStateParserWriter extends OptimizingParserWriter {
  type State = Unit

  override def startState: Unit = ()
}

object CommonStringReaderParser extends CommonStringReaderParser

trait CommonStringReaderParser extends CommonParserWriter with NoStateParserWriter {

}

trait CommonParserWriter extends StringParserWriter {

  /** An integer, without sign or with a negative sign. */
  lazy val wholeNumber: Parser[String] =
    parseRegex("""-?\d+""".r, "whole number")
  /** Number following one of these rules:
    *
    *  - An integer. For example: `13`
    *  - An integer followed by a decimal point. For example: `3.`
    *  - An integer followed by a decimal point and fractional part. For example: `3.14`
    *  - A decimal point followed by a fractional part. For example: `.1`
    */
  lazy val decimalNumber: Parser[String] =
    parseRegex("""(\d+(\.\d*)?|\d*\.\d+)""".r, "decimal number")
  /** Double quotes (`"`) enclosing a sequence of:
    *
    *  - Any character except double quotes, control characters or backslash (`\`)
    *  - A backslash followed by another backslash, a single or double quote, or one
    *    of the letters `b`, `f`, `n`, `r` or `t`
    *  - `\` followed by `u` followed by four hexadecimal digits
    */
  lazy val stringLiteral: Parser[String] =
    new Sequence(parseRegex(""""([^"\x00-\x1F\x7F\\]|\\[\\'"bfnrt]|\\u[a-fA-F0-9]{4})*""".r, "string literal", Some("\"")).
      map(r => r.substring(1, r.length )), literal("\"", allowDrop = false), Processor.ignoreRight[Option[String], Option[String]])

  /** A number following the rules of `decimalNumber`, with the following
    *  optional additions:
    *
    *  - Preceded by a negative sign
    *  - Followed by `e` or `E` and an optionally signed integer
    *  - Followed by `f`, `f`, `d` or `D` (after the above rule, if both are used)
    */
  lazy val floatingPointNumber: Parser[String] =
    parseRegex("""-?(\d+(\.\d*)?|\d*\.\d+)([eE][+-]?\d+)?[fFdD]?""".r, "floating point number")
}
