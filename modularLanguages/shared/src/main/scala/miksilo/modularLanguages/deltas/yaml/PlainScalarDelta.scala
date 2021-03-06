package miksilo.modularLanguages.deltas.yaml

import miksilo.modularLanguages.core.bigrammar.BiGrammar
import miksilo.modularLanguages.core.bigrammar.grammars.{BiSequence, Delimiter, RegexGrammar, SequenceBijective}
import miksilo.modularLanguages.core.deltas.DeltaWithGrammar
import miksilo.modularLanguages.core.deltas.grammars.LanguageGrammars
import miksilo.languageServer.core.language.Language
import miksilo.editorParser.parsers.editorParsers.History
import miksilo.modularLanguages.deltas.expression.{ExpressionDelta, StringLiteralDelta}
import miksilo.modularLanguages.deltas.json.JsonObjectLiteralDelta.MemberKey
import miksilo.modularLanguages.deltas.json.{JsonObjectLiteralDelta, JsonStringLiteralDelta}

object PlainScalarDelta extends DeltaWithGrammar {
  def flowIndicatorChars = """,\[\]{}"""

  override def transformGrammars(_grammars: LanguageGrammars, language: Language): Unit = {
    val grammars = _grammars
    import grammars._

    val nonBreakChars = """\r\n"""
    val nonSpaceChars = """\r\n """
    val indicatorChars = """-\?:,\[\]\{\}#&*!\|>'"%@`"""
    val allowedInFirst = Set('?',':','-')
    val nonPlainFirstChars = (nonSpaceChars + indicatorChars).filter(c => !allowedInFirst.contains(c))
    val plainSafeOutChars = s"""$nonBreakChars#'"""
    val plainSafeInChars = s"""$plainSafeOutChars$flowIndicatorChars"""
    val doubleColonPlainSafeIn = RegexGrammar(s"""[^$nonPlainFirstChars]([^$plainSafeInChars:]|:[^$plainSafeInChars ])*""".r,
      "plain scalar", defaultValue = Some(""), allowDrop = false)
    val doubleColonPlainSafeOut = RegexGrammar(s"""[^$nonPlainFirstChars]([^$plainSafeOutChars:]|:[^$plainSafeOutChars ])*""".r,
      "plain scalar", defaultValue = Some(""), allowDrop = false)

    val nsPlainSafe: BiGrammar = new IfContext(Map(
      FlowIn -> doubleColonPlainSafeIn,
      FlowOut -> doubleColonPlainSafeOut,
      BlockKey -> doubleColonPlainSafeOut,
      FlowKey -> doubleColonPlainSafeIn), doubleColonPlainSafeOut)

    val plainStyleSingleLineString: BiGrammar = nsPlainSafe
    val newLine = RegexGrammar("""\r?\n""".r, "newLine", penaltyOption = Some(History.failPenalty), allowDrop = false)
    val plainStyleMultiLineString: BiGrammar = {
      val lineSeparator = new BiSequence(newLine, _grammars.trivia, BiSequence.ignoreLeft, true)
      val firstLine = new BiSequence(nsPlainSafe, lineSeparator, BiSequence.ignoreRight, false)
      val followingLine = CheckIndentationGrammar.equal(nsPlainSafe)
      val otherLines = CheckIndentationGrammar.greaterThan(new WithIndentationGrammar(followingLine.someSeparated(lineSeparator)))

      new WithIndentationGrammar(new BiSequence(firstLine, otherLines,
        SequenceBijective((firstLine: Any, rest: Any) => {
           rest.asInstanceOf[List[String]].fold(firstLine.asInstanceOf[String])((a, b) => a + " " + b)
        }, (value: Any) => Some(value, List.empty)), false))
    }

    val plainScalarNaked = new WithContext({
      case FlowIn => FlowIn
      case BlockKey => BlockKey
      case FlowKey => FlowKey
      case _ => FlowOut
    }, plainStyleMultiLineString | plainStyleSingleLineString)
    val plainScalar: BiGrammar = plainScalarNaked.
      as(StringLiteralDelta.Value).asLabelledNode(StringLiteralDelta.Shape)

    find(ExpressionDelta.FirstPrecedenceGrammar).addAlternative(plainScalar)

    find(MemberKey).addAlternative(plainScalarNaked.as(MemberKey))
  }

  override def description = "Adds the YAML plain scalar"

  override def dependencies = Set(JsonObjectLiteralDelta)
}
