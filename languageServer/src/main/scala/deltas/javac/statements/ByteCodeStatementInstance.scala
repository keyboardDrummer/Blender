package deltas.javac.statements

import core.deltas._
import core.deltas.path.{NodePath, SequenceElement}
import core.language.node.Node
import core.language.{Compilation, Language}

trait ByteCodeStatementInstance extends DeltaWithGrammar with HasShape {

  override def inject(language: Language): Unit = {
    ByteCodeStatementSkeleton.instances.add(language, this)
    super.inject(language)
  }

  def toByteCode(statement: NodePath, compilation: Compilation): Seq[Node]

  case class SequenceDoesNotEndInJump(sequence: Seq[Node]) extends Exception
  {
    override def toString = s"SequenceDoesNotEndInJump: $sequence"
  }

  final def getNextLabel(statement: NodePath) = (statement, "next")
  def getNextStatements(obj: NodePath, labels: Map[Any, NodePath]): Set[NodePath] = {
    val selection = obj.asInstanceOf[SequenceElement]
    if (selection.hasNext)
      return Set(selection.next)

    val nextOption = labels.get(getNextLabel(obj))
    if (nextOption.nonEmpty)
      return Set(nextOption.get)

    throw SequenceDoesNotEndInJump(selection.parent.current(selection.field).asInstanceOf[Seq[Node]])
  }

  def getLabels(obj: NodePath): Map[Any, NodePath] = Map.empty

  def definedVariables(compilation: Compilation, obj: Node): Map[String, Node] = Map.empty
}