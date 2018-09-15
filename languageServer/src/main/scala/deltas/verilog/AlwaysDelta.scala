package deltas.verilog

import core.bigrammar.BiGrammar
import core.deltas.DeltaWithGrammar
import core.deltas.grammars.LanguageGrammars
import core.language.Language
import core.language.node.{NodeField, NodeShape}
import deltas.statement.StatementDelta

object AlwaysDelta extends DeltaWithGrammar {
  object Shape extends NodeShape
  object SensitivityVariables extends NodeField
  object Body extends NodeField

  override def transformGrammars(grammars: LanguageGrammars, language: Language): Unit = {
    import grammars._

    val statement = find(StatementDelta.Grammar)
    val edge: BiGrammar = "posedge" | "negedge" | ""
    val variable: BiGrammar = edge.as(SensitivityVariable.Edge) ~~ identifier.as(SensitivityVariable.Name) asNode SensitivityVariable.Shape
    val sensitivityList: BiGrammar = "@" ~ variable.manySeparated(",").as(SensitivityVariables)
    val always: BiGrammar = "always" ~~ sensitivityList % statement.as(Body) asNode Shape
    statement.addAlternative(always)
  }

  override def description: String = "Adds the always statement"
}

object SensitivityVariable {
  object Shape extends NodeShape
  object Edge extends NodeField
  object Name extends NodeField
}
