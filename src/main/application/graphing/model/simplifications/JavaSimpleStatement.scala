package application.graphing.model.simplifications

import core.transformation.Contract
import transformations.javac.base.JavaMethodAndClassC
import transformations.javac.statements.{DeclarationC, WhileC}

object JavaSimpleStatement extends TransformationGroup {

  override def dependencies: Set[Contract] = Set(WhileC, DeclarationC)

  override def dependants: Set[Contract] = Set(JavaMethodAndClassC)
}
