package deltas.javac.expressions

import core.deltas.path.NodePath
import core.deltas.{Delta, HasShape, ShapeProperty}
import core.language.node.Node
import core.language.{Compilation, Language}

trait ConvertsToByteCodeDelta extends Delta with HasShape {
  def toByteCode(node: NodePath, compilation: Compilation): Seq[Node]

  override def inject(language: Language): Unit = {
    super.inject(language)
    ToByteCodeSkeleton.toByteCode.add(language, shape, this)
  }
}

object ToByteCodeSkeleton {
  val toByteCode = new ShapeProperty[ConvertsToByteCodeDelta]

  def getToInstructions(compilation: Compilation): NodePath => Seq[Node] = {
    expression => toByteCode(compilation, expression.shape).toByteCode(expression, compilation)
  }
}
