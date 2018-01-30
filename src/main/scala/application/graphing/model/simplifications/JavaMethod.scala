package application.graphing.model.simplifications

import core.deltas.Contract
import deltas.javac.ImplicitObjectSuperClass
import deltas.javac.classes.{BasicImportDelta, FieldDeclaration}
import deltas.javac.expressions.postfix.PostFixIncrementDelta
import deltas.javac.methods.assignment.{AssignToVariable, IncrementAssignmentDelta}
import deltas.javac.methods.{ImplicitReturnAtEndOfMethod, MemberSelector}
import deltas.javac.statements.locals.LocalDeclarationWithInitializerDelta

object JavaMethod extends DeltaGroup {

  override def dependencies: Set[Contract] = Set(ImplicitReturnAtEndOfMethod, LocalDeclarationWithInitializerDelta, IncrementAssignmentDelta, PostFixIncrementDelta, AssignToVariable)

  override def dependants: Set[Contract] = Set(ImplicitObjectSuperClass, MemberSelector, BasicImportDelta, FieldDeclaration)
}
