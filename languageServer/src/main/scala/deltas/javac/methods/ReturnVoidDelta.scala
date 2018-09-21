package deltas.javac.methods

import core.deltas._
import core.deltas.grammars.LanguageGrammars
import core.deltas.path.NodePath
import core.language.node.{Node, NodeShape}
import core.language.{Compilation, Language}
import core.smarts.ConstraintBuilder
import core.smarts.scopes.objects.Scope
import deltas.bytecode.coreInstructions.VoidReturnInstructionDelta
import deltas.javac.statements.StatementToByteCodeDelta
import deltas.statement.{StatementDelta, StatementInstance}

object ReturnVoidDelta extends StatementToByteCodeDelta with StatementInstance with DeltaWithGrammar  {

  override def dependencies: Set[Contract] = Set(MethodDelta, VoidReturnInstructionDelta)

  def returnToLines(_return: Node, compiler: MethodCompiler): Seq[Node] = {
    Seq(VoidReturnInstructionDelta.voidReturn)
  }

  override def transformGrammars(grammars: LanguageGrammars, state: Language): Unit = {
    import grammars._
    val statement = find(StatementDelta.Grammar)

    val returnExpression = ("return" ~ ";") ~> value(_return)
    statement.inner = statement.inner | returnExpression
  }

  def _return: Node = new Node(ReturnVoidKey)

  object ReturnVoidKey extends NodeShape

  override val shape = ReturnVoidKey

  override def toByteCode(_return: NodePath, compilation: Compilation): Seq[Node] = {
    Seq(VoidReturnInstructionDelta.voidReturn)
  }

  override def description: String = "Allows returning void."

  override def collectConstraints(compilation: Compilation, builder: ConstraintBuilder, statement: NodePath, parentScope: Scope): Unit = {}
}
