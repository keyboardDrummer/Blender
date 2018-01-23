package core.nabl.language.types

import core.nabl.constraints.ConstraintBuilder
import core.nabl.constraints.scopes.objects.Scope
import core.nabl.constraints.types.objects.{BoolConstraintType, IntConstraintType, LongConstraintType, Type}

object LongType extends LanguageType {
  override def constraints(builder: ConstraintBuilder, _type: Type, scope: Scope): Unit = builder.typesAreEqual(_type, LongConstraintType)

  override def variables: Set[LanguageTypeVariable] = Set.empty
}

object IntType extends LanguageType {
  override def constraints(builder: ConstraintBuilder, _type: Type, scope: Scope): Unit = builder.typesAreEqual(_type, IntConstraintType)

  override def variables: Set[LanguageTypeVariable] = Set.empty
}

object BoolType extends LanguageType {
  override def constraints(builder: ConstraintBuilder, _type: Type, scope: Scope): Unit = builder.typesAreEqual(_type, BoolConstraintType)

  override def variables: Set[LanguageTypeVariable] = Set.empty
}