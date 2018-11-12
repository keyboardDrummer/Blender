package deltas.statement

import core.deltas.DeltaWithGrammar
import core.deltas.grammars.LanguageGrammars
import core.deltas.path.NodePath
import core.language.node.{GrammarKey, Node, NodeField, NodeShape}
import core.language.{Compilation, Language}
import core.smarts.ConstraintBuilder
import core.smarts.scopes.objects.Scope

object LabelStatementDelta extends StatementInstance with DeltaWithGrammar {
  override val shape = Shape

  object Shape extends NodeShape
  object Name extends NodeField

  def neww(name: String) = new Node(Shape, Name -> name)

  def getName(statement: Node) = statement(Name).asInstanceOf[String]

  object JavaLabelGrammar extends GrammarKey

  override def transformGrammars(grammars: LanguageGrammars, state: Language): Unit = {
    import grammars._
    val statementGrammar = find(StatementDelta.Grammar)
    statementGrammar.addAlternative(create(JavaLabelGrammar, "label" ~~> identifier.as(Name) ~< ";" asNode Shape))
  }

  override def description: String = "Adds a label statement"

  override def getLabels(language: Language, obj: NodePath): Map[Any, NodePath] = {
    super.getLabels(language, obj) + (getName(obj.current) -> obj)
  }

  override def collectConstraints(compilation: Compilation, builder: ConstraintBuilder, statement: NodePath, parentScope: Scope): Unit = {
    val label = getName(statement)
    builder.declare(label, parentScope, statement.getMember(Name))
  }
}
