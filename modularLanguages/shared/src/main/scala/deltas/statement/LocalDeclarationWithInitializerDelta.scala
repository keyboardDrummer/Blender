package deltas.statement

import core.deltas._
import core.deltas.grammars.LanguageGrammars
import core.deltas.path.{NodeChildPath, NodePath, NodeSequenceElement, PathRoot}
import core.language.node._
import core.language.{Compilation, Language}
import deltas.expression.{ExpressionDelta, VariableDelta}
import deltas.javac.statements.ExpressionAsStatementDelta
import deltas.statement.LocalDeclarationDelta.{LocalDeclaration, Type}
import deltas.statement.assignment.SimpleAssignmentDelta
import deltas.HasNameDelta.Name

object LocalDeclarationWithInitializerDelta extends DeltaWithGrammar with DeltaWithPhase {


  override def dependencies: Set[Contract] = Set(SimpleAssignmentDelta, ExpressionAsStatementDelta, LocalDeclarationDelta)

  implicit class LocalDeclarationWithInitializer[T <: NodeLike](node: T) extends LocalDeclaration[T](node) {
    def initializer: T = node(Initializer).asInstanceOf[T]
  }

  override def transformGrammars(grammars: LanguageGrammars, state: Language): Unit = {
    import grammars._
    val statement = find(StatementDelta.Grammar)
    val expression = find(ExpressionDelta.FirstPrecedenceGrammar)
    val typeName = find(LocalDeclarationDelta.WithoutSemiColon)
    val parseDeclarationWithInitializer = typeName ~~ ("=" ~~> expression.as(Initializer)) ~< ";" asLabelledNode Shape
    statement.addAlternative(parseDeclarationWithInitializer)
  }

  def declarationWithInitializer(name: String, _type: Node, initializer: Node) = new Node(Shape,
    Name -> name, Type -> _type, Initializer -> initializer)

  object Shape extends NodeShape

  object Initializer extends NodeField

  override def description: String = "Enables declaring a local and initializing it in one statement."

  def transformDeclarationWithInitializer(path: NodePath, state: Language): Unit = {
    val withInitializer: LocalDeclarationWithInitializer[NodePath] = path
    val name: String = withInitializer.name
    path.shape = LocalDeclarationDelta.Shape
    val target = VariableDelta.neww(name)
    val assignment = SimpleAssignmentDelta.Shape.createWithData(
      SimpleAssignmentDelta.Target -> target,
      SimpleAssignmentDelta.Value -> path.getFieldData(Initializer))
    path.removeField(Initializer)

    val assignmentStatement = ExpressionAsStatementDelta.create(assignment)
    val statements = Seq(path.current, assignmentStatement)
    withInitializer.node match {
      case sequenceElement: NodeSequenceElement =>
        sequenceElement.replaceWith(statements)
      case element: NodeChildPath =>
        throw new Exception("Local declaration with initialization must occur in a sequence context.")
    }
  }

  override def transformProgram(program: Node, compilation: Compilation): Unit = {
    PathRoot(program).visitShape(Shape, obj => transformDeclarationWithInitializer(obj, compilation))
  }
}