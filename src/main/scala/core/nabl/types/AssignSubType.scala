package core.nabl.types

import core.nabl.ConstraintSolver
import core.nabl.objects.{Declaration, DeclarationVariable}
import core.nabl.types.objects.{Type, TypeVariable}

case class AssignSubType(var subType: Type, var superType: Type) extends TypeConstraint {
  override def apply(solver: ConstraintSolver): Boolean = {
    if (subType.fullyApplied && superType.fullyApplied)
    {
      solver.typeGraph.add(TypeNode(subType), SuperType(TypeNode(superType)))
      true
    }
    else false
  }

  override def instantiateType(variable: TypeVariable, instance: Type): Unit = {
    subType = subType.instantiateType(variable, instance)
    superType = superType.instantiateType(variable, instance)
  }

  override def instantiateDeclaration(variable: DeclarationVariable, instance: Declaration): Unit = {
    subType.instantiateDeclaration(variable, instance)
    superType.instantiateDeclaration(variable, instance)
  }

  override def boundTypes: Set[Type] = Set(subType, superType)
}
