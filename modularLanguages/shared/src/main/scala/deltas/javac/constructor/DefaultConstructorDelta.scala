package deltas.javac.constructor

import core.language.node.Node
import core.deltas.{Contract, DeltaWithPhase}
import core.language.Compilation
import deltas.javac.classes.skeleton.JavaClassDelta
import deltas.javac.classes.skeleton.JavaClassDelta.JavaClass
import deltas.javac.methods.AccessibilityFieldsDelta
import deltas.statement.BlockDelta

object DefaultConstructorDelta extends DeltaWithPhase {
  override def dependencies: Set[Contract] = Set.empty

  def transformProgram(program: Node, state: Compilation): Unit = {
    program.visitShape(JavaClassDelta.Shape, node => {
      val javaClass: JavaClass[Node] = node
      val noConstructors = !javaClass.members.exists(member => member.shape == ConstructorDelta.Shape)
      if (noConstructors) {
        val defaultConstructor = ConstructorDelta.constructor(javaClass.name, Seq(), BlockDelta.neww(), AccessibilityFieldsDelta.PublicVisibility)
        javaClass.members = Seq(defaultConstructor) ++ javaClass.members
      }
    })
  }

  override def description: String = "Adds a default public constructor to a class if none is specified."
}
