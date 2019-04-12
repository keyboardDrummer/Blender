package deltas.yaml

import core.bigrammar.BiGrammar
import core.deltas.DeltaWithGrammar
import core.deltas.grammars.LanguageGrammars
import core.language.Language
import deltas.expression.ArrayLiteralDelta

object YamlArrayDelta extends DeltaWithGrammar {
  override def transformGrammars(grammars: LanguageGrammars, language: Language): Unit = {
    val _grammars = grammars
    import grammars._

    val blockValue = find(YamlCoreDelta.IndentationSensitiveExpression)
    val blockArray: BiGrammar = {
      val element = keyword("- ") ~> CheckIndentationGrammar.greaterThan(blockValue)
      CheckIndentationGrammar.aligned(_grammars, element).as(ArrayLiteralDelta.Members).asNode(ArrayLiteralDelta.Shape)
    }
    blockValue.addAlternative(blockArray)
  }

  override def description = "Adds the indentation sensitive literal array"

  override def dependencies = Set(YamlCoreDelta)
}
