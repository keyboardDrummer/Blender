package deltas.smithy

import core.SolveConstraintsDelta
import core.deltas.{Delta, LanguageFromDeltas, ParseUsingTextualGrammar}
import core.language.Language
import deltas.json.ModularJsonLanguage
import deltas.trivia.SlashSlashLineCommentsDelta
import deltas.{FileWithMembersDelta, HasNameDelta}

object SmithyLanguage {
  val deltas: Seq[Delta] = Seq(SmithyStandardLibrary, SmithyListDelta, StructureStatementDelta,
    OperationDelta, SimpleShapeDelta, ShapeStatementDelta, TraitDelta, ResourceDelta, ServiceDelta,
    GenericSmithyDelta, AbsoluteShapeIdentifierDelta, RelativeShapeIdentifierDelta,
    NamespaceDelta, FileWithMembersDelta, HasNameDelta, SlashSlashLineCommentsDelta, SolveConstraintsDelta) ++
    ModularJsonLanguage.deltas

  val language: Language = LanguageFromDeltas(Seq(ParseUsingTextualGrammar()) ++ deltas)
}






