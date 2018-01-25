package core.nabl.language.modules

import core.nabl.constraints.ConstraintBuilder
import core.nabl.constraints.scopes.objects.Scope
import core.nabl.constraints.types.objects.Type
import core.nabl.language.expressions.Expression
import core.nabl.language.types.LanguageType

case class Binding(name: String, body: Expression, bindingType: Option[LanguageType] = None)
{
  def constraints(builder: ConstraintBuilder, parentScope: Scope): Unit = {
    val typeVariable = bindingType.fold[Type](builder.typeVariable())(t => t.constraints(builder, parentScope))
    builder.declaration(name, this, parentScope, Some(typeVariable))
    body.constraints(builder, typeVariable, parentScope)
  }
}