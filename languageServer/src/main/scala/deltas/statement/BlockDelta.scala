package deltas.statement

import core.deltas._
import core.deltas.grammars.LanguageGrammars
import core.deltas.path.{NodePath, SequenceElement}
import core.language.node._
import core.language.{Compilation, Language}
import core.smarts.ConstraintBuilder
import core.smarts.scopes.objects.Scope
import deltas.ConstraintSkeleton
import deltas.javac.statements.{ByteCodeStatementInstance, ByteCodeStatementSkeleton}

object BlockDelta extends ByteCodeStatementInstance with DeltaWithGrammar with StatementInstance {

  override def description: String = "Defines a grammar for blocks."
  override def dependencies: Set[Contract] = Set(StatementDelta)

  object Shape extends NodeShape
  object Statements extends NodeField

  def neww(statements: Seq[Node]): Node = Shape.create(Statements -> statements)

  implicit class BlockStatement[T <: NodeLike](val node: T) extends NodeWrapper[T] {
    def statements: Seq[T] = node(Statements).asInstanceOf[Seq[T]]
  }

  val indentAmount = 4
  override def transformGrammars(grammars: LanguageGrammars, state: Language): Unit = {
    import grammars._
    val statementGrammar = find(StatementDelta.Grammar)

    val blockGrammar = create(Grammar, "{" %> statementGrammar.manyVertical.as(Statements).indent(indentAmount) %< "}" asNode Shape)
    val statementAsSequence = statementGrammar.map[Any, Seq[Any]](statement => Seq(statement), x => x.head)
    val statementAsBlockGrammar = create(StatementAsBlockGrammar, statementAsSequence.as(Statements).asNode(Shape))
    create(BlockOrStatementGrammar, blockGrammar | statementAsBlockGrammar)
  }

  def collectConstraints(compilation: Compilation, builder: ConstraintBuilder, statements: Seq[NodePath], parentScope: Scope): Unit = {
    for(statement <- statements) {
      ConstraintSkeleton.constraints(compilation, builder, statement, parentScope)
    }
  }

  object BlockOrStatementGrammar extends GrammarKey
  object StatementAsBlockGrammar extends GrammarKey
  object Grammar extends GrammarKey

  override def shape: NodeShape = Shape

  override def toByteCode(statement: NodePath, compilation: Compilation): Seq[Node] = { //TODO consider changing this to a transformation to statements, but then that would have to come after SolveConstraints
    val toInstructions = ByteCodeStatementSkeleton.getToInstructions(compilation)
    val block: BlockStatement[NodePath] = statement
    block.statements.flatMap(childStatement => toInstructions(childStatement))
  }

  override def collectConstraints(compilation: Compilation, builder: ConstraintBuilder, statement: NodePath, parentScope: Scope): Unit = {
    val block: BlockStatement[NodePath] = statement
    val blockScope = builder.newScope(Some(parentScope), "blockScope")
    block.statements.foreach(childStatement => ConstraintSkeleton.constraints(compilation, builder, childStatement, blockScope))
  }

  override def getNextLabel(statement: NodePath): (NodePath, String) = {
    val block: BlockStatement[NodePath] = statement
    (block.statements.last, "next") //TODO this is prone to crash. Can we just delete this whole label system?
  }

  override def getLabels(language: Language, statement: NodePath): Map[Any, NodePath] = {
    val block: BlockStatement[NodePath] = statement
    val statementAfterBlockOption = statement.asInstanceOf[SequenceElement].getNext
    val nextMap: Map[Any, NodePath] = statementAfterBlockOption.fold[Map[Any, NodePath]](Map.empty)(
      (next: NodePath) => Map(getNextLabel(statement) -> next))
    nextMap ++ super.getLabels(language, statement)
  }

  override def getNextStatements(language: Language, statement: NodePath, labels: Map[Any, NodePath]): Set[NodePath] = {
    val block: BlockStatement[NodePath] = statement
    val childStatements = block.statements
    Set(childStatements.head)
  }
}
